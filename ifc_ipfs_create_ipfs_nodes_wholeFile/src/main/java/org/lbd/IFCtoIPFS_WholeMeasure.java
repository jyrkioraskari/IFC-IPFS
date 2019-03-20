package org.lbd;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.IPFS_Fetch;
import org.apache.commons.io.IOUtils;
import org.lbd.rdf.CanonizedPattern;
import org.rdfcontext.signing.RDFC14Ner;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;

public class IFCtoIPFS_WholeMeasure {
	private final Property merkle_node;
	private final Model guid_directory_model = ModelFactory.createDefaultModel();
	private String webBase="http://94.237.54.151:8080/serialized_simple/";

	private final IPFS ipfs;

	public IFCtoIPFS_WholeMeasure(String directory) {
		this.merkle_node = guid_directory_model.createProperty("http://ipfs/merkle_node");
		this.ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
		try {
			FileInputStream fileIn = new FileInputStream(directory + "/dir-map.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			Map<String, String> dir_hashes_map = (Map<String, String>) in.readObject();
			for (String s : dir_hashes_map.keySet()) {
				System.out.println("file: " + s);
				updateTimeLimiByreadingHTTP(webBase, s+".filtered.ttl");
				fetch(dir_hashes_map.get(s));
			}
			in.close();
			fileIn.close();
		} catch (IOException i) {
			i.printStackTrace();
			return;
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
			return;
		}
	}
	long start_concurrent;
	long timelimit=0;  // nanoseconds

	public void fetch(String dir_hash) {
		ExecutorService taskExecutor1 = Executors.newFixedThreadPool(100);
		ExecutorService taskExecutor2 = Executors.newFixedThreadPool(100);
		DateFormat df = new SimpleDateFormat("yyyyMMdd HHmmss");

		Date today = Calendar.getInstance().getTime();
		System.out.println("Read DIR");
		readInGuidTable(dir_hash);

		System.out.println("DIR read");
		this.downloaded_in_time_count=0; // RESET
		long total_count=this.guid_directory_model.listStatements().toList().size();
		
		this.guid_directory_model.listStatements().toList().stream()
				.filter(s -> s.getPredicate().equals(this.merkle_node)).map(s -> s.getObject()).forEach(x -> {
					Multihash filePointer = Multihash.fromBase58(x.asLiteral().getLexicalForm());
					try {
						// Removes the local copy
						ipfs.pin.rm(filePointer);
					} catch (Exception e) {

					}
				});
		try {
			ipfs.repo.gc();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Remove done");

		System.out.println("START");

		this.start_concurrent = System.nanoTime();
		this.guid_directory_model.listStatements().toList().stream()
				.filter(s -> s.getPredicate().equals(this.merkle_node)).map(s -> s.getObject()).forEach(x -> {
					Future<Integer> future = taskExecutor1
							.submit(createreadNodeCallable(x.asLiteral().getLexicalForm()));
				});

		taskExecutor1.shutdown();
		try {
			taskExecutor1.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		System.out.println("Percentage "+100*(this.downloaded_in_time_count/total_count)+" count: "+this.downloaded_in_time_count);

	}

	private Callable<Integer> createreadNodeCallable(String key) {
		return () -> {
			readInNode(key);
			return 123;

		};
	}

	private void readInNode(String key) {
		Multihash filePointer = Multihash.fromBase58(key);
		try {
			String tmp = new String(ipfs.cat(filePointer));
			addCount();
			ByteArrayInputStream bi = new ByteArrayInputStream(tmp.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	double downloaded_in_time_count=0;
	synchronized protected void addCount()
	{
		if((System.nanoTime()-this.start_concurrent)<this.timelimit)
			downloaded_in_time_count++;
	}

	private void readInGuidTable(String key) {
		guid_directory_model.removeAll();
		Multihash filePointer = Multihash.fromBase58(key);
		try {
			String content = new String(ipfs.cat(filePointer));
			guid_directory_model.read(new ByteArrayInputStream(content.getBytes()), null, "TTL");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateTimeLimiByreadingHTTP(String baseURL, String key) {
		System.out.println("http get: "+key);
		long start = System.nanoTime();

		try {
			String content =getHTML(baseURL + key);
			long end = System.nanoTime();
			this.timelimit=end-start;
			System.out.println("Web read in: "+(this.timelimit / 1000000f)+" ms");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	// https://stackoverflow.com/questions/1485708/how-do-i-do-a-http-get-in-java
	public static String getHTML(String urlToRead) throws Exception {
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		rd.close();
		return result.toString();
	}

	public static void main(String[] args) {
		IFCtoIPFS_WholeMeasure ifc_ipfs = new IFCtoIPFS_WholeMeasure("c:\\ifc2");
	}

}

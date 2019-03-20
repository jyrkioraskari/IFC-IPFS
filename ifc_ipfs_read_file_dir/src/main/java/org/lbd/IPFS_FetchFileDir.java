package org.lbd;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.IPFS_Fetch;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

public class IPFS_FetchFileDir extends IPFS_Fetch{
	
	private final Property node_property;

	private final Model guid_directory_model = ModelFactory.createDefaultModel();
	private final Model temp_model = ModelFactory.createDefaultModel();

	public IPFS_FetchFileDir(String dir_file) throws IOException {
		this.node="file_directory";
		this.node_property = guid_directory_model.createProperty("http://ipfs/entity");
		fetch(dir_file,true);
		timelog.stream().forEach(txt->writeToFile(txt,true));
		timelog.clear();
		fetch(dir_file,false);
		timelog.stream().forEach(txt->writeToFile(txt,false));
	}


	public void fetch(String directory,boolean jena) {
		Date today = Calendar.getInstance().getTime();
		this.reportDate = df.format(today);
		System.out.println("Read DIR");
		readInGuidTable(directory);
		long start;
		long end;
		System.out.println("START");
		start = System.nanoTime();
		this.guid_directory_model.listStatements().toList().stream()
				.filter(s -> s.getPredicate().equals(this.node_property)).map(s -> s.getObject()).forEach(x -> {					
					readInNode(directory, x.asLiteral().getLexicalForm(),jena);
				});

		end = System.nanoTime();
		System.out.println("Round read in: total " + (end - start) / 1000000f + " ms");
		addLog("Round read in: total " + (end - start) / 1000000f + " ms");

	}

	private void readInNode(String directory, String key,boolean jena) {
		System.out.println("key: "+key);
		if(jena)
		 temp_model.removeAll();
		long start = System.nanoTime();

		Scanner in;
		try {
			in = new Scanner(new FileReader(directory + "\\"+key));

			StringBuilder sb = new StringBuilder();
			while (in.hasNextLine()) {
				sb.append(in.nextLine());
			}
			in.close();
			String content = sb.toString();
			long end = System.nanoTime();
			addLog(key + " read  in: " + (end - start) / 1000000f + " ms");
			ByteArrayInputStream bi = new ByteArrayInputStream(content.getBytes());
			if(jena)
			  temp_model.read(bi, null, "TTL");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void readInGuidTable(String directory) {
		Scanner in;
		try {
			in = new Scanner(new FileReader(directory + "\\V1_directory.ttl"));
			guid_directory_model.removeAll();

			StringBuilder sb = new StringBuilder();
			while (in.hasNextLine()) {
				sb.append(in.nextLine());
			}

			in.close();
			String content = sb.toString();
			guid_directory_model.read(new ByteArrayInputStream(content.getBytes()), null, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	

	public static void main(String[] args) {
		try {
			new IPFS_FetchFileDir("c:\\jo\\output\\");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
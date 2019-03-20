package org.lbd;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

import org.IPFS_Fetch;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

public class IPFS_FetchWebDir extends IPFS_Fetch{
	
	private final Property node_property;

	private final Model guid_directory_model = ModelFactory.createDefaultModel();
	private final Model temp_model = ModelFactory.createDefaultModel();

	public IPFS_FetchWebDir(String baseURL) throws IOException {
		this.node="web_directory";
		this.node_property = guid_directory_model.createProperty("http://ipfs/entity");
		fetch(baseURL,true);
		timelog.stream().forEach(txt->writeToFile(txt,true));
		timelog.clear();
		fetch(baseURL,false);
		timelog.stream().forEach(txt->writeToFile(txt,false));
	}


	public void fetch(String baseURL,boolean jena) {
		Date today = Calendar.getInstance().getTime();
		this.reportDate = df.format(today);
		System.out.println("Read DIR");
		readInGuidTable(baseURL);
		long start;
		long end;
		System.out.println("START");
		start = System.nanoTime();
		this.guid_directory_model.listStatements().toList().stream()
				.filter(s -> s.getPredicate().equals(this.node_property)).map(s -> s.getObject()).forEach(x -> {					
					readInNode(baseURL, x.asLiteral().getLexicalForm(),jena);
				});

		end = System.nanoTime();
		System.out.println("Round read in: total " + (end - start) / 1000000f + " ms");
		addLog("Round read in: total " + (end - start) / 1000000f + " ms");

	}

	private void readInNode(String baseURL, String key,boolean jena) {
		System.out.println("key: "+key);
		if(jena)
		 temp_model.removeAll();
		long start = System.nanoTime();

		Scanner in;
		try {
			String content =getHTML(baseURL + key);
			long end = System.nanoTime();
			addLog(key + " read  in: " + (end - start) / 1000000f + " ms");
			ByteArrayInputStream bi = new ByteArrayInputStream(content.getBytes());
			if(jena)
			  temp_model.read(bi, null, "TTL");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readInGuidTable(String baseURL) {
		try {
			guid_directory_model.removeAll();

			String content =getHTML(baseURL + "V1_directory.ttl");
			guid_directory_model.read(new ByteArrayInputStream(content.getBytes()), null, "TTL");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//https://stackoverflow.com/questions/1485708/how-do-i-do-a-http-get-in-java
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
		try {
			new IPFS_FetchWebDir("http://94.237.54.151:8080/");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
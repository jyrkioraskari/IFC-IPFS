package org.lbd.process;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;

public class IPFSPINServerProcess implements Runnable {
	private final Property merkle_node;

	private final Supplier<Object> sub;
	final String channel_name;
	final String identity;

	private final IPFS ipfs;
	private final Model guid_directory_model = ModelFactory.createDefaultModel();
	private final Model temp_model = ModelFactory.createDefaultModel();


	public IPFSPINServerProcess(IPFS ipfs, String channel_name, String identity) throws IOException {
		this.ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
		this.merkle_node = guid_directory_model.createProperty("http://ipfs/MerkleNode_Hash");

		this.sub = ipfs.pubsub.sub(channel_name);
		this.channel_name = channel_name;
		this.identity = identity;
	}

	public void run() {
		System.out.println("Server listening process starts. Channel:" + this.channel_name);
		while (true) {
			LinkedHashMap lhm = (LinkedHashMap) sub.get();
			Object encoded_data = lhm.get("data");
			if (encoded_data != null) {
				byte[] decoded = Base64.getDecoder().decode(encoded_data.toString());
				String msg = new String(decoded);

				if (msg.startsWith("ipfs_")) {
					String[] splitted = msg.split("_");
					if (splitted.length > 1) {
						long start = System.currentTimeMillis();

						readInGuidTable(splitted[1]);
						System.out.println("Read directory in : " + (System.currentTimeMillis() - start));
						start = System.currentTimeMillis();
						Multihash dirPointer = Multihash.fromBase58(splitted[1]);
						try {
							ipfs.pin.add(dirPointer);
						} catch (IOException e) {
							e.printStackTrace();
						}


						this.guid_directory_model.listStatements().toList().stream()
								.filter(s -> s.getPredicate().equals(this.merkle_node)).map(s -> s.getObject())
								.forEach(x -> {
									Multihash filePointer = Multihash.fromBase58(x.asLiteral().getLexicalForm());
									try {
										System.out.println("pins: " + x);
										ipfs.pin.add(filePointer);
									} catch (IOException e) {
										e.printStackTrace();
									}
								});
						System.out.println("Pinned in : " + (System.currentTimeMillis() - start));
						dirPointer = Multihash.fromBase58(splitted[1]);
						/*
						System.out.println("Ping clean-up starts");
						try {
							ipfs.pin.rm(dirPointer);
						} catch (IOException e) {
							e.printStackTrace();
						}

						this.guid_directory_model.listStatements().toList().stream()
								.filter(s -> s.getPredicate().equals(this.merkle_node)).map(s -> s.getObject())
								.forEach(x -> {
									Multihash filePointer = Multihash.fromBase58(x.asLiteral().getLexicalForm());
									try {
										System.out.println("pins: " + x);
										ipfs.pin.rm(filePointer);
									} catch (IOException e) {
										e.printStackTrace();
									}
								});
						System.out.println("Ping clean-up ends");
						System.out.println("GC starts");
						try {
							ipfs.repo.gc();
						} catch (IOException e) {
							e.printStackTrace();
						}
						System.out.println("GC ends");*/
					}
				}

			}

		}

	}
	
	private void readInNode(String key) {
		Multihash filePointer = Multihash.fromBase58(key);
		try {
			temp_model.removeAll();
			String tmp=new String(ipfs.cat(filePointer));
			System.out.println(tmp);
			ByteArrayInputStream bi = new ByteArrayInputStream(tmp.getBytes());
			System.out.println("Model read");
			temp_model.read(bi, null, "TTL");
			System.out.println("Model read ends");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readInGuidTable(String key) {
		guid_directory_model.removeAll();
		Multihash filePointer = Multihash.fromBase58(key);
		try {
			guid_directory_model.read(new ByteArrayInputStream(new String(ipfs.cat(filePointer)).getBytes()), null,
					"TTL");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String publish(String topic, String content) {
		System.out.println("Server publish topic: " + topic + " content: " + content);

		String urlToRead = "http://127.0.0.1:5001/api/v0/pubsub/pub?arg=" + URLEncoder.encode(topic) + "&arg="
				+ URLEncoder.encode(content);
		StringBuilder result = new StringBuilder();
		URL url;
		try {
			url = new URL(urlToRead);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString();
	}

}
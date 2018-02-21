package org.lbd.process;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lbd.IfcIPFSPINClient;
import org.rdfcontext.signing.RDFC14Ner;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;

public class IPFSPINClientProcess implements Runnable {
	private final Supplier<Object> sub;
	final String channel_name;
	
	private final Property merkle_node;
	private final IPFS ipfs;
	private final Model guid_directory_model = ModelFactory.createDefaultModel();
	private final Model temp_model = ModelFactory.createDefaultModel();

	private final String guid_uri = "http://ipfs/bim/ad8339ce-4378-4e54-b917-ed305724ca9b";
	private String directory = "QmZ7eAfEZq8eKrWXfPwR8NxH2cDga6CMuLrQ7srv75KWeX";
	private long start_time;
	private long sent_time;


	public IPFSPINClientProcess(IPFS ipfs,String channel_name) throws IOException {
		this.merkle_node = guid_directory_model.createProperty("http://ipfs/MerkleNode_Hash");
		this.ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");

		this.sub = ipfs.pubsub.sub(channel_name);
		this.channel_name=channel_name;
	}

	public void run() {
		System.out.println("Server listening process starts. Channel:"+this.channel_name);
		initialMessage();
		while (true) {
			LinkedHashMap lhm=(LinkedHashMap) sub.get();
			Object encoded_data = lhm.get("data");
			if (encoded_data != null) {
				byte[] decoded = Base64.getDecoder().decode(encoded_data.toString());
				String msg=new String(decoded);
				if(msg.startsWith("answer_")) {
					System.out.println("Got answer in : "+(System.currentTimeMillis()-this.sent_time));
					long start=System.currentTimeMillis();			
					String[] splitted = msg.split("_");
					if (splitted.length > 1) {
						System.out.println("Reading..."+splitted[1]);
						readInNode(splitted[1]);
						System.out.println("Read andwer in : "+(System.currentTimeMillis()-start));
						temp_model.write(System.out, "TTL");
					}
				}
			}

		}

	}
	
	
	
	private MerkleNode createProjectMerkleNode(String project_version, Model m) {
		List<MerkleNode> node = null;

		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			m.write(buf, "TTL");
			NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(project_version,
					buf.toString("UTF-8").getBytes());
			node = ipfs.add(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (node == null | node.size() == 0)
			return null;
		return node.get(0);
	}
	
	
	private void initialMessage()
	{
		this.start_time=System.currentTimeMillis();
		readInGuidTable(directory);
		Resource guid_resource=this.guid_directory_model.getResource(this.guid_uri);
		System.out.println("Read in directory: "+(System.currentTimeMillis()-start_time));
		long start=System.currentTimeMillis();
		guid_resource.listProperties(this.merkle_node).forEachRemaining(x->readInNode(x.getObject().toString()));
		System.out.println("Node was read in: "+(System.currentTimeMillis()-start));

		Resource subject = temp_model.createResource("http://ipfs/task");
    	Property property = temp_model.createProperty("http://ipfs/timestamp");
    	Literal time_literal = guid_directory_model.createLiteral(""+System.currentTimeMillis());
    	temp_model
				.add(temp_model.createStatement(subject, property, time_literal));
    	
		start=System.currentTimeMillis();
		RDFC14Ner r1 = new RDFC14Ner(temp_model);

		// Converts C14 into N3
		String output_format = r1.getCanonicalString().replaceAll("\\[", "").replaceAll("\\]", " \n")
				.replaceAll(",", " \n");
		Model m = ModelFactory.createDefaultModel();
		String[] model_splitted = output_format.split("\n");
		StringBuilder sp = new StringBuilder();
		for (String s : model_splitted) {
			List<String> list = new ArrayList<String>();
			try {
			Matcher mx = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(s.trim());
			while (mx.find())
				list.add(mx.group(1));
			
			if(list.size()==2)
				list.add("\"\"");

			String ls = list.get(0);
			if (ls.matches("^(http|https|ftp)://.*$"))
				ls = "<" + ls + ">";
			String lp = list.get(1);
			if (lp.matches("^(http|https|ftp)://.*$"))
				lp = "<" + lp + ">";
			String lo = list.get(2);
			if (lo.matches("^(http|https|ftp)://.*$"))
				lo = "<" + lo + ">";
			sp.append(ls + " " + lp + " " + lo + " .\n");
			}
			catch (Exception e) {
				System.err.println("Bad: pattern: "+s.trim());
				System.err.println("list was"+list);
				e.printStackTrace();
				System.exit(1);
			}

		}
		System.out.println("Convert to C14:N3: "+(System.currentTimeMillis()-start));
		start=System.currentTimeMillis();
		NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper("checked",
				sp.toString().getBytes());
		try {
			List<MerkleNode> node = ipfs.add(file);
			if (node.size() > 0)
			{
				System.out.println("Node published: "+(System.currentTimeMillis()-start));
				start=System.currentTimeMillis();
				Literal hash_literal = guid_directory_model.createLiteral(node.get(0).hash.toBase58());
				guid_resource.addLiteral(this.merkle_node, hash_literal);
				
				MerkleNode dir = createProjectMerkleNode("v1.1", this.guid_directory_model);
				System.out.println("Directory node generated and published: "+(System.currentTimeMillis()-start));
				
				// event.respondWith("answer_"+node.get(0).hash.toBase58());
				publish("IFCBIM", 
						"ipfs_"+dir.hash.toBase58()+"_"+node.get(0).hash.toBase58());
				this.sent_time=System.currentTimeMillis();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}


		
	}

	public static String publish(String topic, String content) {
		System.out.println("Client publish topic: "+topic+" content: "+content);
		String urlToRead = "http://127.0.0.1:5001/api/v0/pubsub/pub?arg=" + URLEncoder.encode(topic) + "&arg=" + URLEncoder.encode(content);
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

	
	
	private void readInGuidTable(String key) {
		Multihash filePointer = Multihash.fromBase58(key);
		try {
			guid_directory_model.read(new ByteArrayInputStream(new String(ipfs.cat(filePointer)).getBytes()), null,
					"TTL");
		} catch (IOException e) {
			e.printStackTrace();
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


}
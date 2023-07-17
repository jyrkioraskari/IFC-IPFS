package org.lbd.process;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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

import org.rdfcontext.signing.RDFC14Ner;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;

public class IPFSPingServerProcess implements Runnable {
	private final Supplier<Object> sub;
	final String channel_name;
    final String identity;
	
	private final IPFS ipfs;
	private final Model guid_directory_model = ModelFactory.createDefaultModel();
	private final Model temp_model = ModelFactory.createDefaultModel();

    
	public IPFSPingServerProcess(IPFS ipfs,String channel_name,String identity) throws IOException {
		this.ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");

		this.sub = ipfs.pubsub.sub(channel_name);
		this.channel_name=channel_name;
		this.identity=identity;
	}

	public void run() {
		System.out.println("Server listening process starts. Channel:"+this.channel_name);
		while (true) {
			LinkedHashMap lhm=(LinkedHashMap) sub.get();
			Object encoded_data = lhm.get("data");
			if (encoded_data != null) {
				byte[] decoded = Base64.getDecoder().decode(encoded_data.toString());
				String msg=new String(decoded);
				if(msg.startsWith("ping_"))
				{
				    String[] splitted=msg.split("_");
				    if(splitted.length>1)
				    {
				    	publish(this.channel_name, "reply_"+splitted[1]+"_"+identity);
				    }
				}
				
	            if(msg.startsWith("ipfs_"))
	            {
	                    String[] splitted=msg.split("_");
					    if(splitted.length>2)
					    {
					    	long start=System.currentTimeMillis();
					    	readInNode(splitted[2]);
					    	System.out.println("Read node in : "+(System.currentTimeMillis()-start));
					    	temp_model.write(System.out,"TTL");

					    	Resource subject = temp_model.createResource("http://ipfs/"+splitted[2]);
					    	Property property = temp_model.createProperty("http://ipfs/CHECKED");
					    	Resource object = temp_model.createResource("http://ipfs/OK");
					    	temp_model
									.add(temp_model.createStatement(subject, property, object));
					    	
					    	
					    	RDFC14Ner r1 = new RDFC14Ner(temp_model);

							// Converts C14 into N3
							String output_format=r1.getCanonicalString().replaceAll("\\[", "").replaceAll("\\]", " \n").replaceAll(",", " \n");
							Model m=ModelFactory.createDefaultModel();
							String[] model_splitted=output_format.split("\n");
							StringBuilder sp=new StringBuilder();
							for(String s:model_splitted)
							{
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
							NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper("checked",
									sp.toString().getBytes());
							try {
						    	start=System.currentTimeMillis();
								List<MerkleNode> node = ipfs.add(file);
								System.out.println("Node publish time : "+(System.currentTimeMillis()-start));
								if(node.size()>0)
								  publish(this.channel_name,"answer_"+node.get(0).hash.toBase58());
							} catch (IOException e) {
								e.printStackTrace();
							}

					    	
					    }
	            }

			}

		}

	}
	
	private void readInNode(String key) {
		temp_model.removeAll();
		Multihash filePointer = Multihash.fromBase58(key);
		try {
			ByteArrayInputStream bi=new ByteArrayInputStream(new String(ipfs.cat(filePointer)).getBytes());
			temp_model.read(bi, null, "TTL");
			System.out.println("Model read ends");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String publish(String topic, String content) {
		System.out.println("Server publish topic: "+topic+" content: "+content);

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

	
}
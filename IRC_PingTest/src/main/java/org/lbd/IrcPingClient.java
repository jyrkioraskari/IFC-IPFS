package org.lbd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
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

public class IrcPingClient extends ListenerAdapter {
	private final Property merkle_node;
	private final IPFS ipfs;
	private final Model guid_directory_model = ModelFactory.createDefaultModel();
	private final Model temp_model = ModelFactory.createDefaultModel();

	private final String guid_uri = "http://ipfs/bim/ad8339ce-4378-4e54-b917-ed305724ca9b";
	private String directory = "QmZ7eAfEZq8eKrWXfPwR8NxH2cDga6CMuLrQ7srv75KWeX";
	private long start_time;
	private long sent_time;

	boolean is_sent = false;
	
	private IrcPingClient() throws InterruptedException, IOException {
		this.merkle_node = guid_directory_model.createProperty("http://ipfs/merkle_node");
		this.ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");

		Configuration configuration = new Configuration.Builder().setName("IPFS" + (System.currentTimeMillis() % 9999))
				.addServer("irc.portlane.se").addAutoJoinChannel("#bimnetwork").addListener(this).buildConfiguration();

		PircBotX bot = new PircBotX(configuration);
		try {
			bot.startBot();
		} catch (IrcException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onJoin(JoinEvent event) {
		System.out.println("onJoin:"+event.getUser().getLogin().toString());
		
		
		
		if (!is_sent) {
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
					event.getChannel().send().message(
							"ipfs_"+dir.hash.toBase58()+"_"+node.get(0).hash.toBase58());
					this.sent_time=System.currentTimeMillis();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

			is_sent = true;
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

	@Override
	public void onGenericMessage(GenericMessageEvent event) {
		if (event.getMessage().startsWith("answer_")) {
			System.out.println("Got answer in : "+(System.currentTimeMillis()-this.sent_time));
			long start=System.currentTimeMillis();			
			String[] splitted = event.getMessage().split("_");
			if (splitted.length > 1) {
				event.respondWith("OK_" + splitted[1]);
				System.out.println("Reading..."+splitted[1]);
				readInNode(splitted[1]);
				System.out.println("Read andwer in : "+(System.currentTimeMillis()-start));
				temp_model.write(System.out, "TTL");
			}
		}
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

	private String resolve() {
		Properties prop = new Properties();
		String ret = null;
		File f = new File(System.getProperty("java.io.tmpdir") + "ipfs.data");
		if (f.exists() && (System.currentTimeMillis() - f.lastModified() < 100000)) {
			System.out.println("Saved directory entry found.");
			try {
				InputStream input = new FileInputStream(f);
				prop.load(input);
				ret = prop.getProperty("directory");
				if (ret == null) {
					try {
						OutputStream output = new FileOutputStream(f);
						ret = ipfs.name.resolve(Multihash.fromBase58("QmPJV1gq9xPPMfuDsy31bvLurzsQM3hw71PwaGoLX5g6rb"));
						prop.setProperty("directory", ret);
						prop.store(output, "");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Get Merkle node for the directory");
			try {
				OutputStream output = new FileOutputStream(f);
				ret = ipfs.name.resolve(Multihash.fromBase58("QmPJV1gq9xPPMfuDsy31bvLurzsQM3hw71PwaGoLX5g6rb"));
				prop.setProperty("directory", ret);
				prop.store(output, "");
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return ret;
	}

	public static void main(String[] args) {
		try {
			new IrcPingClient();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

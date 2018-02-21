package org.lbd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.rdfcontext.signing.RDFC14Ner;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;

public class IrcPINServer extends ListenerAdapter {
	private final Property merkle_node;

	private final IPFS ipfs;
	private final Model guid_directory_model = ModelFactory.createDefaultModel();

	private IrcPINServer() throws InterruptedException, IOException {
		this.merkle_node = guid_directory_model.createProperty("http://ipfs/MerkleNode_Hash");

		ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");

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
    public void onGenericMessage(GenericMessageEvent event) {
            if (event.getMessage().startsWith("ipfs_"))
            {
                    String[] splitted=event.getMessage().split("_");
				    if(splitted.length>1)
				    {
				    	event.respondWith("OK_"+splitted[1]);
				    	long start=System.currentTimeMillis();

				    	readInGuidTable(splitted[1]);
				    	System.out.println("Read directory in : "+(System.currentTimeMillis()-start));
				    	start=System.currentTimeMillis();
				    	this.guid_directory_model.listStatements().toList().stream().filter(s -> s.getPredicate().equals(this.merkle_node)).map(s->s.getObject()).forEach(x->{
				    		Multihash filePointer = Multihash.fromBase58(x.asLiteral().getLexicalForm());
				    		try {
								ipfs.pin.add(filePointer);
							} catch (IOException e) {
								e.printStackTrace();
							}
				    		});
				    	System.out.println("Pinned in : "+(System.currentTimeMillis()-start));
						 event.respondWith("pinned_"+splitted[1]);

				    	
				    }
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

	public static void main(String[] args) {
		try {
			new IrcPINServer();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

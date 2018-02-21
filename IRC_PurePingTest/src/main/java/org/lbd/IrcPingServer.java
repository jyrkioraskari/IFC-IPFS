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

public class IrcPingServer extends ListenerAdapter{

	private IrcPingServer() throws InterruptedException, IOException {
        Configuration configuration = new Configuration.Builder()
                        .setName("IPFS"+(System.currentTimeMillis()%9999)) 
                        .addServer("irc.portlane.se") 
                        .addAutoJoinChannel("#bimnetwork") 
                        .addListener(this) 
                        .buildConfiguration();

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
					    event.respondWith("answer_4321");
				    }
            }
    }
	


	public static void main(String[] args) {
		try {
			new IrcPingServer();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

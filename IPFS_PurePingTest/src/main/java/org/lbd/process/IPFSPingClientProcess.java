package org.lbd.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

import org.lbd.IfcIPFSPingClient;

import io.ipfs.api.IPFS;

public class IPFSPingClientProcess implements Runnable {
	private final Supplier<Object> sub;
	final String channel_name;

	public IPFSPingClientProcess(IPFS ipfs,String channel_name) throws IOException {
		this.sub = ipfs.pubsub.sub(channel_name);
		this.channel_name=channel_name;
	}

	public void run() {
		System.out.println("Server listening process starts. Channel:"+this.channel_name);

		while (true) {
			LinkedHashMap lhm=(LinkedHashMap) sub.get();
			Object encoded_data = lhm.get("data");
			if (encoded_data != null) {
				byte[] decoded = Base64.getDecoder().decode(encoded_data.toString());
				String msg=new String(decoded);
				if(msg.startsWith("reply_"))
				{
				    System.out.println("reply was: "+msg+" time: "+(System.currentTimeMillis()-IfcIPFSPingClient.start_time));
				}
			}

		}

	}
	

	
}
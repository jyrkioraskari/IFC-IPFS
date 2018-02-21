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

import io.ipfs.api.IPFS;

public class IPFSPingServerProcess implements Runnable {
	private final Supplier<Object> sub;
	final String channel_name;
    final String identity;
	
	public IPFSPingServerProcess(IPFS ipfs,String channel_name,String identity) throws IOException {
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
			}

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
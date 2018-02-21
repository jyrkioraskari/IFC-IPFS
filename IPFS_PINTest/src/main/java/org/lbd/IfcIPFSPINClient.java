package org.lbd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.lbd.process.IPFSPINClientProcess;

import io.ipfs.api.IPFS;
/*
* The GNU Affero General Public License
* 
* Copyright (c) 2017 Jyrki Oraskari (Jyrki.Oraskari@gmail.f)
* 
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
* 
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

public class IfcIPFSPINClient {

	private final IPFS ipfs;
	static public long start_time;

	private IfcIPFSPINClient() throws InterruptedException, IOException {
        System.out.println("Client starts");
		ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
		new Thread(new IPFSPINClientProcess(ipfs,"IFCBIM")).start();
		start_time=System.currentTimeMillis();
		publish("IFCBIM", "ping_"+System.currentTimeMillis());
	
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

	

	public static void main(String[] args) {
		try {
			new IfcIPFSPINClient();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

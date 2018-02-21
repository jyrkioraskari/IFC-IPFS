package org.lbd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.lbd.process.IPFSPingServerProcess;

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

public class IfcIPFSPingServer {

	private final IPFS ipfs;

	private IfcIPFSPingServer(String identity) throws InterruptedException, IOException {

        System.out.println("Server starts");

		ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
		new Thread(new IPFSPingServerProcess(ipfs,"IFCBIM",identity)).start();
	
	}


	public static void main(String[] args) {
		try {
			new IfcIPFSPingServer(args[0]);
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

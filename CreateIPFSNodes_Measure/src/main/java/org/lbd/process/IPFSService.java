package org.lbd.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/*
* The GNU Affero General Public License
* 
* Copyright (c) 2018 Jyrki Oraskari (Jyrki.Oraskari@gmail.f)
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

public class IPFSService implements Runnable {
	private final static Logger logger = Logger.getLogger(IPFSService.class.getName());
	private Process process = null;

	public IPFSService() {
	}

	public void run() {
		
		String command="C:\\jo\\go-ipfs\\ipfs daemon";
		try {
			process = Runtime.getRuntime().exec(command);
			String line;
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			  while ((line = input.readLine()) != null) {
			    System.out.println("process: "+line);
			  }
			  input.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void stop() {
		if(process!=null)
			process.destroy();
	}
}

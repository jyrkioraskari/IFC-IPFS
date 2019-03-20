package org.lbd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ScreenScrape {

	public ScreenScrape(String filename) throws FileNotFoundException, IOException
	{
		try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
		    String line = br.readLine();
		    
	        if(line!=null)
		      handleLine(line);
		    while (line != null) {
		        line = br.readLine();
		        if(line!=null)
			      handleLine(line);
		    }
		}	
	}
	
	private void handleLine(String line) {
		if(line.contains(":"))
		{
			  System.out.println("+_"+line);
			  System.out.println("++ "+line.substring(line.indexOf("|")+1));
		}
		if(line.contains(".ifc"))
		  System.out.println(".. "+line);
	}
	
	
	public static void main(String[] args) {
		try {
			new ScreenScrape("c:/jo/IFC_screencopies/1.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

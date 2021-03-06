import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class ReadFileC {

	public ReadFileC(String directory) {

		File curDir = new File(directory);
		getAllFiles(curDir);
	}

	private void getAllFiles(File curDir) {

		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isFile()) {
				handleIPFSTotal(f);
			}
		}
	}

	private void handleIPFSTotal(File file) {
		if (file.getName().contains("jena_false")&& file.getName().endsWith(".txt")) {

			Scanner sc;
			try {
				sc = new Scanner(file);
				sc.useDelimiter("\n");

				while (sc.hasNext()) {
					String line = sc.next();
					String[] s = line.split(" ");
					if (line.contains("total")&&line.contains("Second"))
					{
						System.out.println("" + s[5]);
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			//writeFile(log, file.getAbsolutePath()+".csv");
			log.setLength(0);
		}
		
	}
	
	

	private void writeFile(StringBuffer sb,String filename)
	{
	     
		    Path path = Paths.get(filename);
		  
		    try {
				Files.write(path, sb.toString().getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	

	protected StringBuffer log=new StringBuffer();  
	protected  final DateFormat df = new SimpleDateFormat("yyyyMMdd HHmmss");


	private void addLog(String txt)
	{
		log.append(txt);
		log.append("\n");
	}


	public static void main(String[] args) {
		
		new ReadFileC("C:\\jo\\Test results\\Concurrent\\FirstConcurrentResultFromRemote");

	}

}

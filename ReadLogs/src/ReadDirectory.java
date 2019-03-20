import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ReadDirectory {

	public ReadDirectory(String directory) {

		File curDir = new File(directory);
		getAllFiles(curDir);
	}

	private void getAllFiles(File curDir) {

		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isFile()) {
				handleIPFS(f);
			}
		}
	}

	private void handleHTTPS(File file) {
		if (file.getName().contains("jena_false")) {

			Scanner sc;
			try {
				sc = new Scanner(file);
				sc.useDelimiter("\n");

				while (sc.hasNext()) {
					String line = sc.next();
					if (line.contains("Round"))
						continue;
					String[] s = line.split(" ");
					// System.out.print(s[0]+" ");
					System.out.println("" + s[4]);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	private void handleIPFS(File file) {
		if (file.getName().contains("jena_false")) {

			boolean read = false;
			Scanner sc;
			try {
				sc = new Scanner(file);
				sc.useDelimiter("\n");

				while (sc.hasNext()) {
					String line = sc.next();
					if (line.contains("First round read in:"))
						read=true;
					if (line.contains("Round") || line.contains("total"))
						continue;
					if (read) {
						String[] s = line.split(" ");
						System.out.println("" + s[4]);
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private void handle(File file) {
		if (file.getName().contains("jena_false")) {

			boolean read = true;
			Scanner sc;
			try {
				sc = new Scanner(file);
				sc.useDelimiter("\n");

				while (sc.hasNext()) {
					String line = sc.next();
					if (line.contains("First round read in:"))
						read=false;
					if (line.contains("Round") || line.contains("total"))
						continue;
					if (read) {
						String[] s = line.split(" ");
						System.out.println("" + s[4]);
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		
		new ReadDirectory("C:\\jo\\Test results\\IPFS\\new_FirstResultFromRemote");

	}

}

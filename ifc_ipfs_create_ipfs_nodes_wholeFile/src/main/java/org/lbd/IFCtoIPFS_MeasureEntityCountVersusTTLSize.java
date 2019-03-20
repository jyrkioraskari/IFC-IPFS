package org.lbd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.lbd.rdf.CanonizedPattern;

public class IFCtoIPFS_MeasureEntityCountVersusTTLSize {
	private final String name;
	private final String baseURI = "http://ipfs/bim/";

	private final CanonizedPattern canonized_pattern = new CanonizedPattern();
	private Map<String, String> dir_hashes_map = new HashMap<>();

	public IFCtoIPFS_MeasureEntityCountVersusTTLSize(String name) {
		this.name = name;
	}

	public void add(String directory) throws InterruptedException, IOException {
		File curDir = new File(directory);
		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isFile()) {
				handle(f.getAbsolutePath());
			}
		}
	}

	public void handle(String ifc_file) throws FileNotFoundException {
		if (!ifc_file.endsWith(".ifc"))
			return;
		try {
			Splitted_NoGeometryIfcOWL ifcrdf = new Splitted_NoGeometryIfcOWL(ifc_file);
			ByteArrayOutputStream stringStream = new ByteArrayOutputStream();
			ifcrdf.getFilteredModel().write(stringStream, "TTL");
			if (ifcrdf.getEntitys().size() > 0)
				System.out.println(
						"LINE," + ifc_file + "," + stringStream.toString().length() + "," + ifcrdf.getEntitys().size());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		try {
			IFCtoIPFS_MeasureEntityCountVersusTTLSize ifc_ipfs = new IFCtoIPFS_MeasureEntityCountVersusTTLSize(
					"IFC project");
			ifc_ipfs.add("c:\\ifc3\\ifc");
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

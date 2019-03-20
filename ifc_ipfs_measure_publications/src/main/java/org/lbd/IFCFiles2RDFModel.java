package org.lbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.lbd.ifc.IfcFileMeasurements;

public class IFCFiles2RDFModel {
	private File database_file = new File("c:/jo/measures.ttl");

	private final Model jena_model = ModelFactory.createDefaultModel();
	private final Property jena_property_ifcfile;
	private final Property jena_property_ifcowl_triplescount;

	public IFCFiles2RDFModel() {
		this.jena_property_ifcfile = IfcFileMeasurements.jena_property_ifcfile;
		this.jena_property_ifcowl_triplescount = IfcFileMeasurements.jena_property_ifcowl_triplescount;
		if (!database_file.exists())
			return;
		InputStream in;
		try {
			in = new FileInputStream(database_file);
			jena_model.read(in, null, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void addFiles(String directory) {
		try {
			File folder = new File(directory);
			File[] listOfFiles = folder.listFiles();

			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					if (listOfFiles[i].getName().toLowerCase().endsWith(".ifc")) {
						IfcOWLFile ifcowl = new IfcOWLFile(listOfFiles[i].getAbsolutePath());
						if (ifcowl.getTriples_count() > 0) {
							Resource ifcfile = this.jena_model.createResource();
							Literal abs_filename = this.jena_model.createLiteral(listOfFiles[i].getAbsolutePath());
							ifcfile.addLiteral(this.jena_property_ifcfile, abs_filename);
							ifcfile.addLiteral(this.jena_property_ifcowl_triplescount,ifcowl.getTriples_count());
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void save() {

		OutputStream out;
		try {
			out = new FileOutputStream(database_file);
			jena_model.write(out, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		IFCFiles2RDFModel m = new IFCFiles2RDFModel();
		m.addFiles("c:/ifc/");
		m.save();

	}

}

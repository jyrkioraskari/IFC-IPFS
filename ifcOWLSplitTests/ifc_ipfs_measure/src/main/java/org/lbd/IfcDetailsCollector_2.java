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

import nl.tue.ddss.convert.Header;
import nl.tue.ddss.convert.HeaderParser;
import nl.tue.ddss.convert.IfcVersion;

public class IfcDetailsCollector_2 {
	private File database_file = new File("c:/jo/measures.ttl");

	private final Model jena_model = ModelFactory.createDefaultModel();
	private final Property jena_property_ifcfile;
	
	public final Property jena_property_ifcowl_filesize;
	public final Property jena_property_ifc_filesize;
	public final Property jena_property_ifc_version;


	public IfcDetailsCollector_2() {
		this.jena_property_ifcfile = IfcFileMeasurements.jena_property_ifcfile;
		this.jena_property_ifcowl_filesize = IfcFileMeasurements.jena_property_ifcowl_filesize;
		this.jena_property_ifc_filesize = IfcFileMeasurements.jena_property_ifc_filesize;
		this.jena_property_ifc_version = IfcFileMeasurements.jena_property_ifc_version;

		if (!database_file.exists())
		{
			return;
		}
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
						Literal abs_filename = this.jena_model.createLiteral(listOfFiles[i].getAbsolutePath());
						FileInputStream input = new FileInputStream(listOfFiles[i]);
						Header header = HeaderParser.parseHeader(input);
						String version=header.getSchema_identifiers().get(0);
						//System.out.println(listOfFiles[i]+" "+version);
						
						Resource ifcfile;
						if(this.jena_model.containsLiteral(null, jena_property_ifcfile, abs_filename))
						{
							ifcfile=this.jena_model.listStatements(null, jena_property_ifcfile, abs_filename).next().getSubject();							
						}
						else
						{
							ifcfile = this.jena_model.createResource();
						}
						
						ifcfile.addLiteral(this.jena_property_ifc_version, version);
						double ifcfile_bytes = listOfFiles[i].length();
						
						ifcfile.addLiteral(this.jena_property_ifc_filesize,listOfFiles[i].length());
						
						File ifcowl_file=new File(listOfFiles[i].getAbsolutePath() + ".ttl");
						if(ifcowl_file.length()>0)
						{
							ifcfile.addLiteral(this.jena_property_ifcowl_filesize, ifcowl_file.length());
						}
						
						double triples=ifcfile.getProperty(IfcFileMeasurements.jena_property_ifcowl_triplescount).getObject().asLiteral().getDouble();
						if(triples>0)
						{
							System.out.println("triples were:"+triples);
							System.out.println(ifcowl_file.exists());
						}
						
						
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		save();
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
		IfcDetailsCollector_2 m = new IfcDetailsCollector_2();
		m.addFiles("c:/ifc/");
		m.save();

	}

}

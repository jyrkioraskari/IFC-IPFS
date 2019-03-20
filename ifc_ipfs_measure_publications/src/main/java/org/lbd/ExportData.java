package org.lbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.StmtIterator;
import org.lbd.ifc.IfcFileMeasurements;

public class ExportData {
	private final Model jena_model = ModelFactory.createDefaultModel();
	private File database_file = new File("c:/jo/measures.ttl");
	private final Property jena_property_ifcfile;

	
	public ExportData()
	{
		this.jena_property_ifcfile = jena_model.createProperty("http://measurements/ifcfile");

		if (!database_file.exists())
			return;
		InputStream in;
		try {
			in = new FileInputStream(database_file);
			jena_model.read(in, null, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		ResIterator rit = jena_model.listResourcesWithProperty(this.jena_property_ifcfile);
		System.out.println(IfcFileMeasurements.headers());
		rit.forEachRemaining(x -> {
			
			System.out.println(new IfcFileMeasurements(x));
		});

	}
	 
	
	
	public static void main(String[] args) {
		new ExportData();
	}

}

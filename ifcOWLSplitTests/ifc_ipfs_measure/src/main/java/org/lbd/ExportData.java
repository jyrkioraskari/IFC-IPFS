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
	private final Property jena_property_ifcfile;

	
	public ExportData()
	{
		this.jena_property_ifcfile = jena_model.createProperty("http://measurements/ifcfile");

		readModel("c:/jo","measures.ttl");
		readModel("c:/jo","result3.ttl");
		readModel("c:/jo","result4.ttl");
		readModel("c:/jo","result5.ttl");
		readModel("c:/jo","result6.ttl");
		readModel("c:/jo","result10.ttl");
		readModel("c:/jo","result12.ttl");
		
		readModel("c:/jo","result13.ttl");
		readModel("c:/jo","result14.ttl");
		readModel("c:/jo","result15.ttl");
		readModel("c:/jo","result16.ttl");
		
		readModel("c:/jo","result10NoGeo.ttl");
		readModel("c:/jo","resultWF.ttl");
		ResIterator rit = jena_model.listResourcesWithProperty(this.jena_property_ifcfile);
		System.out.println(IfcFileMeasurements.headers());
		rit.forEachRemaining(x -> {
			IfcFileMeasurements line=new IfcFileMeasurements(x);
			if(line.getIfcowl_triplescount().isPresent())
			  System.out.println(line);
		});

	}



	private void readModel(String dir,String file_name) {
		File database_file = new File(dir+"/"+file_name);
		if (!database_file.exists())
		{
			System.err.println("File "+database_file.getAbsolutePath()+" does not exist.");
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
	 
	
	
	public static void main(String[] args) {
		new ExportData();
	}

}

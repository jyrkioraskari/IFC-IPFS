package org.lbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.lbd.ifc.IfcFileMeasurements;
import org.lbd.ifc.RootEntity;

public class IFCtoLBDFiles_Measurer {
	private File database_file = new File("c:/jo/measures.ttl");

	private final Model jena_model = ModelFactory.createDefaultModel();

	private final Model jena_guid_directory_model = ModelFactory.createDefaultModel();
	private final Property jena_property_entity;

	private final String version_name;
	private final String baseURI = "http://ipfs/bim/";
	private final String output_directory;

	public IFCtoLBDFiles_Measurer(String output_directory, String version_name) {
		this.jena_property_entity = jena_guid_directory_model.createProperty("http://ipfs/entity");

		this.version_name = version_name;
		this.output_directory = output_directory;
		if (!output_directory.endsWith("/"))
			output_directory += "/";

		if (!database_file.exists())
			return;
		InputStream in;
		try {
			in = new FileInputStream(database_file);
			jena_model.read(in, null, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		final List<Resource> resource_list = new ArrayList<>();
		ResIterator rit = jena_model.listResourcesWithProperty(IfcFileMeasurements.jena_property_ifcfile);
		rit.forEachRemaining(x -> {
			resource_list.add(x);
		});

		resource_list.parallelStream().forEach(x -> {
			if (x.hasProperty(IfcFileMeasurements.jena_property_ifcowl_version))
				return;
			try {
				add(x, x.getProperty(IfcFileMeasurements.jena_property_ifcfile).getObject().toString());
				save();
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}

		});
	}

	public void add(Resource x, String ifc_file) throws InterruptedException, IOException {
		Splitted_IfcOWL ifcrdf = new Splitted_IfcOWL(ifc_file);
		
		
		x.addLiteral(IfcFileMeasurements.jena_property_ifcowl_root_elements, ifcrdf.getEntitys().size());
		x.addLiteral(IfcFileMeasurements.jena_property_ifcowl_version, ifcrdf.getIfc_version());
		if (ifcrdf.getEntitys().size() > 0) {

			long triples_max = ifcrdf.getEntitys().parallelStream().mapToLong(y -> y.getTriples().size()).max()
					.getAsLong();
			long triples_min = ifcrdf.getEntitys().parallelStream().mapToLong(y -> y.getTriples().size()).min()
					.getAsLong();

			System.out.println(triples_min + " < " + triples_max);
			x.addLiteral(IfcFileMeasurements.jena_property_ifcowl_entity_triples_min, triples_min);
			x.addLiteral(IfcFileMeasurements.jena_property_ifcowl_entity_triples_max, triples_max);
		} else {
			x.addLiteral(IfcFileMeasurements.jena_property_ifcowl_entity_triples_min, 0);
			x.addLiteral(IfcFileMeasurements.jena_property_ifcowl_entity_triples_max, 0);
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

		IFCtoLBDFiles_Measurer ifc_ipfs = new IFCtoLBDFiles_Measurer("c:/jo/output/", "V1");
	}
}

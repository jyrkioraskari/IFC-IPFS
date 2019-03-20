package org.lbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.lbd.ifc.IfcFileMeasurements;
import org.lbd.ifc.RootEntity;
import org.lbd.rdf.CanonizedPattern;
import org.rdfcontext.signing.RDFC14Ner;


public class IFCtoLBDFiles_MeasureMerkleNodeSizes {
	private File database_file = new File("c:/jo/measures.ttl");

	private final Model jena_model = ModelFactory.createDefaultModel();

	private final Model jena_guid_directory_model = ModelFactory.createDefaultModel();
	private final Property jena_property_entity;

	private final String version_name;
	private final String baseURI = "http://ipfs/bim/";
	private final String output_directory;
	
	CanonizedPattern canonized_pattern=new CanonizedPattern();

	public IFCtoLBDFiles_MeasureMerkleNodeSizes(String output_directory, String version_name) {
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
			if (x.hasProperty(IfcFileMeasurements.jena_property_ipfs_one_block_percentage))
				return;
			try {
				addNodeSizes(x, x.getProperty(IfcFileMeasurements.jena_property_ifcfile).getObject().toString());
				save();
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}

		});
		save();
	}

	public void addNodeSizes(Resource filex, String ifc_file) throws InterruptedException, IOException {
		Splitted_IfcOWL ifcrdf = new Splitted_IfcOWL(ifc_file);
	
		if (ifcrdf.getEntitys().size() > 0) {
           System.out.println(ifc_file);
			publishEntityNodes2IPFS(filex,ifcrdf.getEntitys(), ifcrdf.getURI2GUID_map());
		} 
		
		 
	}

	private void  publishEntityNodes2IPFS(Resource filex,List<RootEntity> root_entitys, Map<String, String> uri2guid) {
		long min=Long.MAX_VALUE;
		long max=Long.MIN_VALUE;
		float all_count=0;
		float under_count=0;
		for (RootEntity g : root_entitys) {
			com.hp.hpl.jena.rdf.model.Resource hp_guid_subject = null;
			com.hp.hpl.jena.rdf.model.Model new_model = com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel();
			System.out.println("triples: "+g.getTriples().size());
			for (Statement apache_triple : g.getTriples()) {
				
				com.hp.hpl.jena.rdf.model.Resource hp_subject = new_model.createResource(apache_triple.getSubject().getURI());
				String sg = uri2guid.get(hp_subject.getURI());  // The map sets the coding
				if (sg != null) {
					String sn = hp_subject.getURI().substring(0, (hp_subject.getURI().lastIndexOf("/") + 1)) + sg;
					hp_subject = new_model.createResource(sn);
					hp_guid_subject = hp_subject;
				}

				com.hp.hpl.jena.rdf.model.Property hp_property = new_model.createProperty(apache_triple.getPredicate().getURI());
				RDFNode apache_object = apache_triple.getObject();
				if (apache_object.isResource()) {
					com.hp.hpl.jena.rdf.model.Resource or = new_model.createResource(apache_object.asResource().getURI());
					String og = uri2guid.get(or.getURI());
					if (og != null) {
						String on = or.getURI().substring(0, (or.getURI().lastIndexOf("/") + 1)) + og;
						or = new_model.createResource(on);
					}
					new_model.add(new_model.createStatement(hp_subject, hp_property, or));
				}
				else
				{
				   com.hp.hpl.jena.rdf.model.Literal hp_literal=new_model.createLiteral(apache_object.toString());
				   new_model.add(new_model.createStatement(hp_subject, hp_property, hp_literal));
				}

			}
			System.out.println("new model: statements "+new_model.size());
			 long ret=createMerkleNode(g.getGuid(), new_model, hp_guid_subject);
			 System.out.println(ret);
			 if(ret>max)
				 max=ret;
			 if(ret<min)
				 min=ret;
			 all_count++;
			 if(ret<(1024*256))
				 under_count++;

		}
		//filex.addLiteral(IfcFileMeasurements.jena_property_ipfs_entity_nodesize_min_filtered, min);
		//filex.addLiteral(IfcFileMeasurements.jena_property_ipfs_entity_nodesize_max_filtered, max);
		//filex.addLiteral(IfcFileMeasurements.jena_property_filtered_ipfs_one_block_percentage, (under_count/all_count)*100);
		filex.addLiteral(IfcFileMeasurements.jena_property_ipfs_one_block_percentage, (under_count/all_count)*100);
	}

	private long createMerkleNode(String guid, com.hp.hpl.jena.rdf.model.Model hp_model, com.hp.hpl.jena.rdf.model.Resource hp_guid_subject) {
		try {
			RDFC14Ner r1=new RDFC14Ner(hp_model);

			String cleaned=canonized_pattern.clean(r1.getCanonicalString());
			return cleaned.length();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
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

		IFCtoLBDFiles_MeasureMerkleNodeSizes ifc_ipfs = new IFCtoLBDFiles_MeasureMerkleNodeSizes("c:/jo/output/", "V1");
	}
}

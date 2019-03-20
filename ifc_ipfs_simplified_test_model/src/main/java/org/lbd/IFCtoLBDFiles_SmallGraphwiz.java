package org.lbd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lbd.ifc.RootEntity;
import org.lbd.rdf.CanonizedPattern;
import org.rdfcontext.signing.RDFC14Ner;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class IFCtoLBDFiles_SmallGraphwiz {
	private final Model original_entities_model = ModelFactory.createDefaultModel();
	private final Model combined_entities_model = ModelFactory.createDefaultModel();

	private final Model jena_guid_directory_model = ModelFactory.createDefaultModel();
	private final Property jena_property_random;
	private final Property jena_property_entity;
	private final String version_name;
	private final String baseURI = "http://example.com/";
	private String output_directory;

	private final CanonizedPattern canonized_pattern = new CanonizedPattern();

	public IFCtoLBDFiles_SmallGraphwiz(String output_directory, String version_name) {
		
		this.jena_property_random = jena_guid_directory_model.createProperty("http://ipfs/random");
		this.jena_property_entity = jena_guid_directory_model.createProperty("http://ipfs/entity");
		this.version_name = version_name;
		this.output_directory = output_directory;
		if (!output_directory.endsWith("/"))
			this.output_directory += "/";
		System.out.println("output: "+output_directory);

	}

	public void add(String ifc_file) throws InterruptedException, IOException {
		Splitted_IfcOWL ifcrdf = new Splitted_IfcOWL(ifc_file);
		ifcrdf.writeOWLFile(output_directory);
		original_entities_model.add(ifcrdf.getModel());
		publishEntityNodes2Files(ifcrdf.getEntitys(), ifcrdf.getURI2GUID_map());
		publishDirectoryNode2Files(this.version_name, jena_guid_directory_model);
		
		long i=0;
		for (RootEntity g : ifcrdf.getEntitys()) {
			for (Statement triple : g.getTriples()) {
		     	i++;	
			}
		}
		System.out.println("All together: "+i);
		System.out.println("Difference:");
		ifcrdf.printDifference();
	}

	private Map<String, Resource> resources_map = new HashMap<>();

	private void publishEntityNodes2Files(List<RootEntity> root_entitys, Map<String, String> uri2guid) {

		for (RootEntity g1 : root_entitys) {
			for (Statement triple : g1.getTriples()) {

				String s_uri = triple.getSubject().getURI();
				Resource subject = null;
				String sg = uri2guid.get(s_uri); // The map sets the coding
				if (sg != null) {
					String sn = s_uri.substring(0, (s_uri.lastIndexOf("/") + 1)) + sg;
					subject = ResourceFactory.createResource(sn);
					resources_map.put(s_uri, subject);
				}
			}
		}

		for (RootEntity g : root_entitys) {
			Resource guid_subject = null;
			Model entity_model = ModelFactory.createDefaultModel();
			boolean random_added = false;
			for (Statement triple : g.getTriples()) {

				String s_uri = triple.getSubject().getURI();
				System.out.println("S_uri: "+s_uri);
				Resource subject = null;
				String subject_quid = uri2guid.get(s_uri); // The map sets the coding
				if (subject_quid != null) {
					String sn = s_uri.substring(0, (s_uri.lastIndexOf("/") + 1)) + subject_quid;
					if(subject_quid.length()==0)
					{
						subject = resources_map.get(s_uri);
						if (subject == null) {
							subject = entity_model.createResource();
						}
					}
					else
						subject = entity_model.createResource(sn);
					guid_subject = subject;
					resources_map.put(s_uri, subject);

				}

				if (subject == null) {
					subject = resources_map.get(s_uri);
					if (subject == null) {
						subject = entity_model.createResource();
						resources_map.put(s_uri, subject);
					}
				}

				Property property = entity_model.getProperty(triple.getPredicate().getURI());
				RDFNode object = triple.getObject();
				if (object.isResource()) {
					Resource or = resources_map.get(object.asResource().getURI());
					if (or == null) {
						try {
							if (object.asResource().isAnon())
								or = entity_model.createResource();
							else {
								char last = object.asResource().getURI()
										.charAt(object.asResource().getURI().length() - 1);
								if (object.asResource().getURI().contains("_") && Character.isDigit(last)) {
									or = entity_model.createResource();
								} else
									or = entity_model.createResource(object.asResource().getURI());
								resources_map.put(object.asResource().getURI(), or);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					String og = uri2guid.get(or.getURI());
					if (og != null) {
						String on = or.getURI().substring(0, (or.getURI().lastIndexOf("/") + 1)) + og;
						or = entity_model.createResource(on);
					}
					entity_model.add(entity_model.createStatement(subject, property, or));
				} else {
					Literal literal = entity_model.createLiteral(object.toString());
					entity_model.add(entity_model.createStatement(subject, property, literal));
				}

			}
			createEntityNode(g.getGuid(), entity_model, guid_subject);
		}
	}


	private void createEntityNode(String guid, Model entity_model, Resource guid_subject) {
		try {
			GraphMaker gm=new GraphMaker();
			String graph=gm.create(entity_model);
			System.out.println(graph);
			
			
			File f = new File(output_directory + this.version_name + "_" + guid + ".ttl");
			System.out.println("Create file: "+f.getAbsolutePath()+" "+entity_model.size());
			FileOutputStream buf = new FileOutputStream(f);
			//model.write(buf, "TTL");*/
			
			RDFC14Ner r1 = new RDFC14Ner(entity_model);
			String cleaned = canonized_pattern.clean(r1.getCanonicalString());
			buf.write(cleaned.getBytes());
			buf.close();
			this.combined_entities_model.add(entity_model);

			
			if (guid_subject != null) {
				Resource guid_resource = jena_guid_directory_model.createResource(baseURI + URLEncoder.encode(guid));
				Literal hash_literal = jena_guid_directory_model.createLiteral(f.getName());
				jena_guid_directory_model.add(jena_guid_directory_model.createStatement(guid_resource,
						this.jena_property_entity, hash_literal));

				Property hp_type = entity_model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				RDFNode guid_class = null;

				for (Statement st : guid_subject.listProperties(hp_type).toList())
					guid_class = st.getObject();
				if (guid_class == null) {
					System.err.println("No GUID class type."+guid_subject);
					return;
				}

				if (!guid_class.isResource())
					return;
				jena_guid_directory_model
						.add(jena_guid_directory_model.createStatement(guid_resource, RDF.type, guid_class));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void publishDirectoryNode2Files(String project_name, Model m) {

		try {
			FileOutputStream buf = new FileOutputStream(new File(output_directory + project_name + "_directory.ttl"));
			RDFC14Ner r1 = new RDFC14Ner(m);
			String cleaned = canonized_pattern.clean(r1.getCanonicalString());
			buf.write(cleaned.getBytes());
			buf.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			if (args.length > 1) {
				IFCtoLBDFiles_SmallGraphwiz ifc_ipfs = new IFCtoLBDFiles_SmallGraphwiz(args[1], "V1");
				ifc_ipfs.add(args[0]);
			}
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

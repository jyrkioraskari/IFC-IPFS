package org.lbd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class IFCtoIPFS_WholeFIlesSplitSerializer {
	private Model jena_guid_directory_model = ModelFactory.createDefaultModel();
	private final Property jena_property_merkle_node;

	private final String name;
	private final String baseURI = "http://ipfs/bim/";

	private final CanonizedPattern canonized_pattern = new CanonizedPattern();
	

	public IFCtoIPFS_WholeFIlesSplitSerializer(String name) {
		this.jena_property_merkle_node = jena_guid_directory_model.createProperty("http://ipfs/merkle_node");
		this.name = name;
	}

	private String dir = ".";

	public void add(String directory) throws InterruptedException, IOException {
		File curDir = new File(directory);
		this.dir = directory;
		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isFile()) {
				handle(f.getAbsolutePath());
				clean();
			}
		}
	}

	private void clean() {
		jena_guid_directory_model = ModelFactory.createDefaultModel();
		resources_map.clear();

	}

	public void handle(String ifc_file) throws FileNotFoundException {
		if (!ifc_file.endsWith(".ifc"))
			return;
		Splitted_NoGeometryIfcOWL ifcrdf = new Splitted_NoGeometryIfcOWL(ifc_file);

		File f = new File(ifc_file + ".filtered.ttl");
		FileOutputStream buf = new FileOutputStream(f);
		ifcrdf.getFilteredModel().write(buf, "TTL");
		publishEntityNodes2IPFS(ifcrdf.getEntitys(), ifcrdf.getURI2GUID_map(), ifc_file);

	}

	private Map<String, Resource> resources_map = new HashMap<>();

	private void publishEntityNodes2IPFS(List<RootEntity> root_entitys, Map<String, String> uri2guid, String ifc_file) {
		Map<String, String> guid_file_map = new HashMap<>();


		for (RootEntity g1 : root_entitys) {
			Resource guid_subject = null;
			for (Statement triple : g1.getTriples()) {

				String s_uri = triple.getSubject().getURI();
				Resource subject = null;
				String sg = uri2guid.get(s_uri); // The map sets the coding
				if (sg != null) {
					String sn = s_uri.substring(0, (s_uri.lastIndexOf("/") + 1)) + sg;
					subject = ResourceFactory.createResource(sn);
					guid_subject = subject;
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
				Resource subject = null;
				String sg = uri2guid.get(s_uri); // The map sets the coding
				if (sg != null) {
					String sn = s_uri.substring(0, (s_uri.lastIndexOf("/") + 1)) + sg;
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
						if (property.toString().equals(
								"http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#relatedObjects_IfcRelDecomposes"))
							System.out.println("decompo: " + object.asResource().getURI());
						char last = object.asResource().getURI().charAt(object.asResource().getURI().length() - 1);
						if (object.asResource().getURI().contains("_") && Character.isDigit(last)) {
							or = entity_model.createResource();
						} else
							or = entity_model.createResource(object.asResource().getURI());
						resources_map.put(object.asResource().getURI(), or);
					}

					String og = uri2guid.get(or.getURI());
					if (og != null) {
						String on = or.getURI().substring(0, (or.getURI().lastIndexOf("/") + 1)) + og;
						or = entity_model.createResource(on);
					}
					entity_model.add(entity_model.createStatement(subject, property, or));
				} else {
					Literal hp_literal = entity_model.createLiteral(object.toString());
					entity_model.add(entity_model.createStatement(subject, property, hp_literal));
				}

			}
			
			 try {
				 RDFC14Ner r1 = new RDFC14Ner(entity_model);
			     String cleaned = canonized_pattern.clean(r1.getCanonicalString());
			     File file=new File(ifc_file + "_" + g.getGuid()+".ttl.ser");
		         FileOutputStream fileOut =
		         new FileOutputStream(file);
		         ObjectOutputStream out = new ObjectOutputStream(fileOut);
		         out.writeObject(cleaned);
		         out.close();
		         fileOut.close();
		         guid_file_map.put(g.getGuid(), file.getName());
		      } catch (IOException i) {
		         i.printStackTrace();
		      }
		}
		
		
		 try {
	         FileOutputStream fileOut =
	         new FileOutputStream(ifc_file+"_guid-file-map.ser");
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(guid_file_map);
	         out.close();
	         fileOut.close();
	      } catch (IOException i) {
	         i.printStackTrace();
	      }
	}

	public static void main(String[] args) {
		try {
			IFCtoIPFS_WholeFIlesSplitSerializer ifc_ipfs = new IFCtoIPFS_WholeFIlesSplitSerializer("IFC project");
			ifc_ipfs.add(".");
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

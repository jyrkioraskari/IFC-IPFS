package org.lbd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
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
import com.hp.hpl.jena.vocabulary.RDF;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;

public class IFCtoIPFS_WholePublisher {
	private Model jena_guid_directory_model = ModelFactory.createDefaultModel();
	private final Property jena_property_merkle_node;
	
	private final IPFS ipfs;
	private final String name;
	private final String baseURI = "http://ipfs/bim/";
	
	private final CanonizedPattern canonized_pattern=new CanonizedPattern();
	private Map<String, String> dir_hashes_map = new HashMap<>();

	public IFCtoIPFS_WholePublisher(String name) {
		this.jena_property_merkle_node = jena_guid_directory_model.createProperty("http://ipfs/merkle_node");
		this.ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
		this.name=name;
	}


	public void add(String directory) throws InterruptedException, IOException {
		File curDir = new File(directory);
		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isFile()) {
				handle(f.getAbsolutePath());
				clean();
			}
		}
		
		writeDirectoryFile(directory);
	}
	
	
	private void writeDirectoryFile(String directory)
	{
		try {
			PrintWriter pw = new PrintWriter(
					new FileOutputStream(new File(directory+"/dir_list.txt"), true));
			for(String key:dir_hashes_map.keySet())
			{
				pw.write(key+" "+dir_hashes_map.get(key));
				pw.write("\n");
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void clean() 
	{
		jena_guid_directory_model = ModelFactory.createDefaultModel();
		resources_map.clear();
		
	}
	
	public void handle(String ifc_file) throws FileNotFoundException
	{
		if(!ifc_file.endsWith(".ifc"))
			return;
		Splitted_NoGeometryIfcOWL ifcrdf = new Splitted_NoGeometryIfcOWL(ifc_file);
		
		File f = new File(ifc_file+ ".filtered.ttl");
		FileOutputStream buf = new FileOutputStream(f);
		ifcrdf.getFilteredModel().write(buf,"TTL");
		publishEntityNodes2IPFS(ifcrdf.getEntitys(), ifcrdf.getURI2GUID_map());
		
		MerkleNode project_table = publishDirectoryNode2IPFS(this.name, jena_guid_directory_model);
		dir_hashes_map.put(f.getName(), project_table.hash.toBase58());
	}
	
	private Map<String, Resource> resources_map = new HashMap<>();

	private void publishEntityNodes2IPFS(List<RootEntity> root_entitys, Map<String, String> uri2guid) {

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

				Property property = entity_model
						.getProperty(triple.getPredicate().getURI());
				RDFNode object = triple.getObject();
				if (object.isResource()) {
					Resource or = resources_map.get(object.asResource().getURI());
					if (or == null) {
						if (property.toString().equals(
								"http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#relatedObjects_IfcRelDecomposes"))
							System.out.println("decompo: " + object.asResource().getURI());
						char last = object.asResource().getURI()
								.charAt(object.asResource().getURI().length() - 1);
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
			createMerkleNode(g.getGuid(), entity_model, guid_subject);

		}
	}
	
	private void createMerkleNode(String guid, Model model, Resource guid_subject) {
		try {
			RDFC14Ner r1=new RDFC14Ner(model);
			String cleaned=canonized_pattern.clean(r1.getCanonicalString());
			
			NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(guid,
					cleaned.getBytes());
			List<MerkleNode> node = ipfs.add(file);
			if(node.size()==0)
				return;
			if (guid_subject != null) {
				Resource guid_resource = jena_guid_directory_model.createResource(baseURI + URLEncoder.encode(guid));
				Literal hash_literal = jena_guid_directory_model.createLiteral(node.get(0).hash.toBase58());
				jena_guid_directory_model.add(jena_guid_directory_model.createStatement(guid_resource, this.jena_property_merkle_node, hash_literal));

				Property hp_type = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				RDFNode guid_class = null;
				
				for (Statement st : guid_subject.listProperties(hp_type).toList())
					guid_class = st.getObject();
				Resource apache_guid_resource = jena_guid_directory_model.createResource(guid_resource.getURI());
				if(guid_class==null)
				{
					System.err.println("No GUID type.");
					return;
				}

				if(!guid_class.isResource())
					return;
				jena_guid_directory_model.add(jena_guid_directory_model.createStatement(apache_guid_resource, RDF.type, guid_class));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private MerkleNode publishDirectoryNode2IPFS(String project_name, Model model) {
		List<MerkleNode> node = null;

		try {
			RDFC14Ner r1=new RDFC14Ner(model);
			String cleaned=canonized_pattern.clean(r1.getCanonicalString());

			NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(project_name,
					cleaned.getBytes());
			node = ipfs.add(file);

		} catch (Exception e) {
			e.printStackTrace();
		}
		if(node==null || node.size()==0)
			return null;
		return node.get(0);
	}
	
	

	public static void main(String[] args) {
		try {
			IFCtoIPFS_WholePublisher ifc_ipfs=new IFCtoIPFS_WholePublisher("IFC project");			
			ifc_ipfs.add(".");
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

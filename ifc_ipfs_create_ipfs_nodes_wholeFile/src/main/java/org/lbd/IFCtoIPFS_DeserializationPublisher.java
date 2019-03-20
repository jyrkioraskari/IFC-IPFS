package org.lbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.lbd.rdf.CanonizedPattern;
import org.rdfcontext.signing.RDFC14Ner;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;

public class IFCtoIPFS_DeserializationPublisher {
	private Model jena_guid_directory_model = ModelFactory.createDefaultModel();
	private final Property jena_property_merkle_node;
	private final IPFS ipfs;
	private final CanonizedPattern canonized_pattern=new CanonizedPattern();
	private final String baseURI = "http://ipfs/bim/";
	private Map<String, String> dir_hashes_map = new HashMap<>();



	public IFCtoIPFS_DeserializationPublisher(String directory) {
		this.jena_property_merkle_node = jena_guid_directory_model.createProperty("http://ipfs/merkle_node");
		this.ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
		File curDir = new File(directory);
		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isFile()) {
				handle(f.getAbsolutePath());
			}
		}
		 try {
	         FileOutputStream fileOut =
	         new FileOutputStream(directory+"/dir-map.ser");
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(dir_hashes_map);
	         out.close();
	         fileOut.close();
	      } catch (IOException i) {
	         i.printStackTrace();
	      }
	}
	

	private void handle(String ifc_file) {
		if (!ifc_file.endsWith(".ifc"))
			return;
		Map<String, String> guid_file_map;

		try {
			FileInputStream fileIn = new FileInputStream(ifc_file + "_guid-file-map.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			guid_file_map = (Map<String, String>) in.readObject();
			for (String guid : guid_file_map.keySet()) {
				System.out.println("f: " + guid);
				String parent = (new File(ifc_file)).getParent();
				readEntitySerFile(parent + "/" + guid_file_map.get(guid),guid);
			}
			in.close();
			fileIn.close();
		} catch (IOException i) {
			i.printStackTrace();
			return;
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
			return;
		}
		publishDirectoryNode2IPFS();
		MerkleNode project_table = publishDirectoryNode2IPFS();
		dir_hashes_map.put(new File(ifc_file).getName(), project_table.hash.toBase58());
		clean();
	}
	
	private void clean()
	{
		jena_guid_directory_model.removeAll();
	}


	private void readEntitySerFile(String file_name,String guid) {
		try {
			FileInputStream fileIn = new FileInputStream(file_name);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			String content = (String) in.readObject();
			createMerkleNode(guid, content);
			in.close();
			fileIn.close();
		} catch (IOException i) {
			i.printStackTrace();
			return;
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
			return;
		}
	}
	
	private void createMerkleNode(String guid, String content) {
		try {
			
			NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(guid,
					content.getBytes());
			List<MerkleNode> node = ipfs.add(file);
			if(node.size()==0)
				return;
			
			if (guid != null) {
				System.out.println("guid oli: "+guid);
				String enc_guid=guid;
				if(guid.length()>5)
					enc_guid=URLEncoder.encode(guid);
				Resource guid_resource = jena_guid_directory_model.createResource(baseURI + enc_guid);
				Literal hash_literal = jena_guid_directory_model.createLiteral(node.get(0).hash.toBase58());
				jena_guid_directory_model.add(jena_guid_directory_model.createStatement(guid_resource, this.jena_property_merkle_node, hash_literal));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private MerkleNode publishDirectoryNode2IPFS() {
		List<MerkleNode> node = null;

		try {
			RDFC14Ner r1=new RDFC14Ner(this.jena_guid_directory_model);
			String cleaned=canonized_pattern.clean(r1.getCanonicalString());
			NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper("bim",
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
		IFCtoIPFS_DeserializationPublisher ifc_ipfs = new IFCtoIPFS_DeserializationPublisher(".");
	}

}

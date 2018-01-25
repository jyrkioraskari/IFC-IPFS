package org.lbd;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.lbd.ifc.GUIDSet;
import org.lbd.statistics.Statistics;
import org.rdfcontext.signing.RDFC14Ner;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;

/*
* The GNU Affero General Public License
* 
* Copyright (c) 2018 Jyrki Oraskari (Jyrki.Oraskari@gmail.f)
* 
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
* 
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

public class IfcIPFS {
	static List<Statistics> statistics = new ArrayList<>();
	static Statistics current_process;;

	private final Model guid_directory_model = ModelFactory.createDefaultModel();
	private final Property merkle_node;
	private final IPFS ipfs;
	private final String baseURI = "http://ipfs/bim/";
	static long start_time = System.currentTimeMillis();

	private IfcIPFS(String ifcrdf_file) throws InterruptedException, IOException {
		current_process = new Statistics(ifcrdf_file);
		statistics.add(current_process);
		current_process.setProcess_start_time(System.currentTimeMillis());
		this.merkle_node = guid_directory_model.createProperty("http://ipfs/MerkleNode_Hash");

		ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");

		processIfc(ifcrdf_file);
		current_process.setProcess_stop_time(System.currentTimeMillis());
	}

	private void processIfc(String ifc_file) {
		System.out.println("File: "+ifc_file);
		IfcRDFCat ifcrdf = new IfcRDFCat(ifc_file);

		long start = System.currentTimeMillis();
		extractMerkleNodes(ifcrdf.getGuid_sets(), ifcrdf.getUri_guid());
		IfcIPFS.current_process.setPublish_merkle_nodes(System.currentTimeMillis() - start);

		start = System.currentTimeMillis();
		MerkleNode project_table = createProjectMerkleNode("IFC Project", guid_directory_model);
		//System.out.println("pt:"+project_table.hash.toBase58());
		IfcIPFS.current_process.setPublish_directory_node(System.currentTimeMillis() - start);
		start = System.currentTimeMillis();
		String ipns_name=null;
		try {
			Map pub = ipfs.name.publish(project_table.hash);
			ipns_name=(String)pub.get("Name");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		//System.out.println("IPNS publish time:"+(System.currentTimeMillis() - start));
		IfcIPFS.current_process.setIpns_name_publish(System.currentTimeMillis() - start);

		/*
		if(ipns_name==null)
			return;
		start = System.currentTimeMillis();
		try {
			Multihash filePointer = Multihash.fromBase58(ipns_name);
			String resolved = ipfs.name.resolve(filePointer);
			System.out.println("Resolved: "+resolved);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("IPNS resolve time:"+(System.currentTimeMillis() - start));
		*/

	}

	Map<String, com.hp.hpl.jena.rdf.model.Resource> blank_nodes = new HashMap<>();

	private com.hp.hpl.jena.rdf.model.Resource getResource(Map<String, String> uri_guid, Resource r,
			com.hp.hpl.jena.rdf.model.Model model) {

		String sg = uri_guid.get(r.getURI());
		if (sg != null) {
			String sn = r.getURI().substring(0, (r.getURI().lastIndexOf("/") + 1)) + sg;
			return model.createResource(sn);
		}

		if (r.getURI().startsWith(this.baseURI) && r.getURI().contains("_")) {
			com.hp.hpl.jena.rdf.model.Resource ret = blank_nodes.getOrDefault(r.getURI(), model.createResource());
			blank_nodes.put(r.getURI(), ret);
			return ret;
		}

		return model.createResource(r.getURI());
	}

	private void extractMerkleNodes(List<GUIDSet> guid_sets, Map<String, String> uri_guid) {
		blank_nodes.clear();
		for (GUIDSet g : guid_sets) {
			com.hp.hpl.jena.rdf.model.Resource hp_guid_subject = null;
			com.hp.hpl.jena.rdf.model.Model new_model = com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel();
			for (Statement apache_triple : g.getTriples()) {

				com.hp.hpl.jena.rdf.model.Resource hp_subject = getResource(uri_guid, apache_triple.getSubject(),
						new_model);
				String sg = uri_guid.get(apache_triple.getSubject().getURI());
				if (sg != null) {
					hp_guid_subject = hp_subject;
				}

				com.hp.hpl.jena.rdf.model.Property hp_property = new_model
						.createProperty(apache_triple.getPredicate().getURI());
				RDFNode apache_object = apache_triple.getObject();
				if (apache_object.isResource()) {
					com.hp.hpl.jena.rdf.model.Resource hp_object = getResource(uri_guid,
							apache_triple.getObject().asResource(), new_model);

					String og = uri_guid.get(hp_object.getURI());
					if (og != null) {
						String on = hp_object.getURI().substring(0, (hp_object.getURI().lastIndexOf("/") + 1)) + og;
						hp_object = new_model.createResource(on);
					}
					new_model.add(new_model.createStatement(hp_subject, hp_property, hp_object));
				} else {
					com.hp.hpl.jena.rdf.model.Literal hp_literal = new_model.createLiteral(apache_object.toString());
					new_model.add(new_model.createStatement(hp_subject, hp_property, hp_literal));
				}

			}
			createMerkleNode(g.getGuid(), new_model, hp_guid_subject);

		}
	}

	private void createMerkleNode(String guid, com.hp.hpl.jena.rdf.model.Model hp_model,
			com.hp.hpl.jena.rdf.model.Resource hp_guid_subject) {
		try {
			RDFC14Ner r1 = new RDFC14Ner(hp_model);

			// Converts C14 into N3
			String output_format = r1.getCanonicalString().replaceAll("\\[", "").replaceAll("\\]", " \n")
					.replaceAll(",", " \n");
			List<String> splitted = split(output_format,'\n');
			StringBuilder sp = new StringBuilder();
			for (String s : splitted) {
				List<String> list=null;
				try {
				list =split(s,' ');				

				String ls = list.get(0);
				if (ls.matches("^(http|https|ftp)://.*$"))
					ls = "<" + ls + ">";
				String lp = list.get(1);
				if (lp.matches("^(http|https|ftp)://.*$"))
					lp = "<" + lp + ">";
				String lo = list.get(2);
				if (lo.matches("^(http|https|ftp)://.*$"))
					lo = "<" + lo + ">";
				sp.append(ls + " " + lp + " " + lo + " .\n");
				}
				
				catch (Exception e) {
					System.err.println("Bad: pattern: "+s.trim());
					if(list!=null)
					System.err.println("list was"+list.toString());
					System.err.println("C14 string was: "+r1.getCanonicalString());
					System.err.println("Output was: "+output_format);
					e.printStackTrace();
					System.exit(1);
				}
			}
			NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(guid,
					sp.toString().getBytes());
			List<MerkleNode> node = ipfs.add(file);
			if (node.size() == 0)
				return;

			if (hp_guid_subject != null) {
				Resource guid_resource = guid_directory_model.createResource(baseURI + URLEncoder.encode(guid));
				Literal hash_literal = guid_directory_model.createLiteral(node.get(0).hash.toBase58());
				guid_directory_model
						.add(guid_directory_model.createStatement(guid_resource, this.merkle_node, hash_literal));

				com.hp.hpl.jena.rdf.model.Property hp_type = hp_model
						.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				com.hp.hpl.jena.rdf.model.RDFNode hp_guid_class = null;

				for (com.hp.hpl.jena.rdf.model.Statement st : hp_guid_subject.listProperties(hp_type).toList())
					hp_guid_class = st.getObject();
				Resource apache_guid_resource = guid_directory_model.createResource(guid_resource.getURI());
				if (hp_guid_class == null) {
					System.err.println("No GUID type.");
					return;
				}

				if (!hp_guid_class.isResource())
					return;
				Resource apache_g_class = guid_directory_model.createResource(hp_guid_class.asResource().getURI());
				guid_directory_model
						.add(guid_directory_model.createStatement(apache_guid_resource, RDF.type, apache_g_class));
			} else
				System.err.println("hp_guid_subject null");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	private static List<String> split(String s,char splitchr) {
		List<String> list = new ArrayList<String>();
		int mode=0;
		StringBuilder sb=new StringBuilder();
		for(int ci:s.getBytes())
		{
			char c=(char)ci;
			switch(mode) {
			case 0:
				if(c=='\"')
					mode =1;
				if(c=='\'')
					mode =1;
				if(c==splitchr)
				{
				  list.add(sb.toString());
				  sb.setLength(0);
				}
				else
				  sb.append(c);	
				
				break;
			case 1: 
				sb.append(c);	
				if(c=='\"')
					mode =0;
				if(c=='\'')
					mode =0;
				break;
			}
		}	
		if(sb.toString().trim().length()>0)
 		  list.add(sb.toString());
		if(list.size()==0)
			System.err.println("String: "+s+" split:"+splitchr);
		return list;
	}
	
	private MerkleNode createProjectMerkleNode(String project_version, Model m) {
		List<MerkleNode> node = null;

		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			RDFDataMgr.write(buf, m, RDFFormat.TTL);
			NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(project_version,
					buf.toString("UTF-8").getBytes());
			node = ipfs.add(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (node == null | node.size() == 0)
			return null;
		return node.get(0);
	}

	public static String publish(String topic, String content) {
		String urlToRead = "http://127.0.0.1:5001/api/v0/pubsub/pub?arg=" + URLEncoder.encode(topic) + "&arg="
				+ URLEncoder.encode(content);
		StringBuilder result = new StringBuilder();
		URL url;
		try {
			url = new URL(urlToRead);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString();
	}

	private static void testAllFiles(File curDir) {
		Scanner scanner = new Scanner(System.in);
		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isFile()) {
				try {
					new IfcIPFS(f.getAbsolutePath());
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
				}
				System.out.println(IfcIPFS.current_process.toString());
			}
		}

	}
	
	private static void stringTests() {
		List<String> tulos=split("<jsjsks,koe,\"abc\",'222',>", ',');
		for(String s:tulos)
		   System.out.println(s);
		System.out.println("--------------");

		List<String> tulos2=split("<jsjsks koe \"abc\" 'it is cool' \"\"", ' ');
		for(String s:tulos2)
		   System.out.println(s);
	}

	public static void main(String[] args) {
		File testset = new File("c:\\jo\\testset\\2\\");
	    testAllFiles(testset);
		
		/*try {
			new IfcIPFS("c:\\jo\\Upload\\IfcOpenHouse_IFC2x3.ifc"); 
			//new IfcIPFS("c:\\jo\\testset\\091210ISO9705revit9-c.ifc"); 
			//new IfcIPFS("c:\\jo\\Upload\\Duplex_Electrical_20121207.ifc"); 
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}*/
		

		Collections.sort(statistics);
		for (Statistics s : statistics)
			System.out.println(s);
	}

}

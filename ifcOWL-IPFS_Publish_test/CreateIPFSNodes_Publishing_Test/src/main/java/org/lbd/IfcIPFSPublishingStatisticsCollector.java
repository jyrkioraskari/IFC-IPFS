package org.lbd;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lbd.ifc.CurrentRootEntityTripleSet;
import org.lbd.rdf.CanonizedPattern;
import org.lbd.statistics.Statistics;
import org.rdfcontext.signing.RDFC14Ner;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

import fi.aalto.lbd.lib.AaltoIPFSConnection;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;

/*
* The GNU Affero General Public License
* 
* Copyright (c) 2018, 2019 Jyrki Oraskari (Jyrki.Oraskari@aalto.fi)
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

public class IfcIPFSPublishingStatisticsCollector extends TestLogger implements IPFSPublisherInterface {
	static List<Statistics> statistics = new ArrayList<>();
	Statistics current_process;;
	private final AaltoIPFSConnection ipfs = AaltoIPFSConnection.getInstance();

	private final Model jena_guid_directory_model = ModelFactory.createDefaultModel();
	private final Property jena_property_merkle_node;
	private final Property jena_property_random;
	private final String baseURI = "http://ipfs/bim/";
	static long start_time = System.currentTimeMillis();
	private final CanonizedPattern canonized_pattern = new CanonizedPattern();
	private final Random random_number_generator = new Random(System.currentTimeMillis());

	private IfcIPFSPublishingStatisticsCollector(String ifcrdf_file) throws InterruptedException, IOException {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");

		System.out.println("IFC file: " + ifcrdf_file);
		this.jena_property_random = ResourceFactory.createProperty("http://ipfs/random");
		this.jena_property_merkle_node = ResourceFactory.createProperty("http://ipfs/MerkleNode_Hash");
		current_process = new Statistics(ifcrdf_file);
		statistics.add(current_process);
		current_process.setProcess_start_time(System.currentTimeMillis());

		processIfc(ifcrdf_file);
		if (current_process.getIfcowl_triples() > 0) {
			timelog.add(current_process.toString());
			timelog.stream().forEach(txt -> writeToFile(txt));
		}

		// Clean database
		this.jena_guid_directory_model.listStatements().toList().stream()
				.filter(s -> s.getPredicate().equals(this.jena_property_merkle_node)).map(s -> s.getObject())
				.forEach(x -> {
					Multihash filePointer = Multihash.fromBase58(x.asLiteral().getLexicalForm());
					try {
						ipfs.getIpfs().pin.rm(filePointer);
					} catch (Exception e) {

					}
				});
		try {
			ipfs.getIpfs().repo.gc();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processIfc(String ifc_file) {

		this.current_process.setProcess_start_time(System.nanoTime());
		long start = System.nanoTime();
		IfcOWL_Segmenting_Rule10 ifcrdf = new IfcOWL_Segmenting_Rule10(ifc_file, this);

		System.out.println("model: " + ifcrdf.getModel_size());

		this.current_process.setIfc_convert((System.nanoTime() - start) / 1000000l);

		ifcrdf.split_and_publish();
		if (ifcrdf.getTotal_count() <= 0)
			return;
		System.out.println("roots: " + ifcrdf.getRoots_size());
		System.out.println("total: " + ifcrdf.getTotal_count());
		this.current_process.setIfc_root_entities(ifcrdf.getRoots_size());
		this.current_process.setIfcowl_triples(ifcrdf.getModel_size());

		this.current_process.setRewritingtriples(this.total_rewritingtriples_time);
		this.current_process.setCanonization(this.total_canonization_time);
		this.current_process.setPublish_merkle_nodes(this.total_publication_time);
		this.current_process.setIpfs_node_count(this.ipfs_node_count);

		start = System.nanoTime();
		MerkleNode project_table = publishDirectoryNode2IPFS("IFC Project", jena_guid_directory_model);
		this.current_process.setPublish_directory_node((System.nanoTime() - start) / 1000000l);
		this.current_process.setProcess_stop_time(System.nanoTime());
	}

	private Map<String, Resource> resources_map = new HashMap<>();
	private Map<String, String> guid_map = new HashMap<>();

	public void createRootResource(String uri, String guid) {
		String sn = uri.substring(0, (uri.lastIndexOf("/") + 1)) + guid;
		Resource subject = ResourceFactory.createResource(sn);
		resources_map.put(uri, subject);
		guid_map.put(uri, guid);
	}

	private long total_rewritingtriples_time = 0l;
	private long total_publication_time = 0l;
	private long total_canonization_time = 0l;
	private long ipfs_node_count = 0l;

	private long created_guids = 0l;

	public void publishEntityNode2IPFS(CurrentRootEntityTripleSet entity_triples) {
		String guid = this.guid_map.get(entity_triples.getURI());
		if (guid == null) {
			guid = "created_+" + this.created_guids++;
			this.guid_map.put(entity_triples.getURI(), guid);
		}
		long start = System.nanoTime();
		Map<String, Resource> local_resources_map = new HashMap<>();
		local_resources_map.putAll(this.resources_map);
		Resource guid_subject = null;
		Model entity_model = ModelFactory.createDefaultModel();
		Resource directory_recource = jena_guid_directory_model.createResource(); // empty
		Literal random_number_literal = jena_guid_directory_model.createLiteral("" + random_number_generator.nextInt());
		jena_guid_directory_model.add(jena_guid_directory_model.createStatement(directory_recource,
				this.jena_property_random, random_number_literal));

		for (org.apache.jena.rdf.model.Statement triple : entity_triples.getTriples()) {

			String s_uri = triple.getSubject().getURI();
			Resource subject = null;

			if (subject == null) {
				subject = local_resources_map.get(s_uri);
				if (subject == null) {
					subject = entity_model.createResource();
					local_resources_map.put(s_uri, subject);
				}
			}

			Property property = entity_model.getProperty(triple.getPredicate().getURI());
			org.apache.jena.rdf.model.RDFNode object = triple.getObject();
			if (object.isResource()) {
				Resource or = local_resources_map.get(object.asResource().getURI());
				if (or == null) {
					// Is it a numbered line
					char last = object.asResource().getURI().charAt(object.asResource().getURI().length() - 1);
					if (object.asResource().getURI().contains("_") && Character.isDigit(last)) {
						or = entity_model.createResource(); // Nameless node here
					} else
						or = entity_model.createResource(object.asResource().getURI());
					local_resources_map.put(object.asResource().getURI(), or);
				}

				entity_model.add(entity_model.createStatement(subject, property, or));
			} else {
				Literal hp_literal = entity_model.createLiteral(object.toString());
				entity_model.add(entity_model.createStatement(subject, property, hp_literal));
			}

		}
		this.total_rewritingtriples_time += (System.nanoTime() - start) / 1000000f;
		createMerkleNode(guid, entity_model, guid_subject);

	}

	private boolean directory_random_created = false;

	private void createMerkleNode(String guid, Model model, Resource guid_subject) {
		try {
			RDFC14Ner r1 = new RDFC14Ner(model);
			long start = System.nanoTime();
			String cleaned = canonized_pattern.clean(r1.getCanonicalString());
			this.total_canonization_time += (System.nanoTime() - start) / 1000000f;

			NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(guid, cleaned.getBytes());
			start = System.nanoTime();
			List<MerkleNode> node = ipfs.add(file);
			this.total_publication_time += (System.nanoTime() - start) / 1000000f;
			if (node.size() == 0) {
				System.out.println("No node created!!!");
				System.exit(10);
				// return;
			}
			this.ipfs_node_count++;

			// Directory update
			if (!directory_random_created) {
				Resource directory_recource = jena_guid_directory_model.createResource(); // empty
				Literal random_number_literal = jena_guid_directory_model
						.createLiteral("" + random_number_generator.nextInt());
				jena_guid_directory_model.add(jena_guid_directory_model.createStatement(directory_recource,
						this.jena_property_random, random_number_literal));
				directory_random_created = true;
			}
			if (guid_subject != null) {
				Resource guid_resource = jena_guid_directory_model.createResource(baseURI + URLEncoder.encode(guid));
				Literal hash_literal = jena_guid_directory_model.createLiteral(node.get(0).hash.toBase58());
				jena_guid_directory_model.add(jena_guid_directory_model.createStatement(guid_resource,
						this.jena_property_merkle_node, hash_literal));

				Property hp_type = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				RDFNode guid_class = null;

				for (Statement st : guid_subject.listProperties(hp_type).toList())
					guid_class = st.getObject();
				Resource apache_guid_resource = jena_guid_directory_model.createResource(guid_resource.getURI());
				if (guid_class == null) {
					System.err.println("No GUID type.");
					return;
				}

				if (!guid_class.isResource())
					return;
				jena_guid_directory_model
						.add(jena_guid_directory_model.createStatement(apache_guid_resource, RDF.type, guid_class));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private MerkleNode publishDirectoryNode2IPFS(String project_name, Model model) {
		List<MerkleNode> node = null;

		try {
			RDFC14Ner r1 = new RDFC14Ner(model);
			String cleaned = canonized_pattern.clean(r1.getCanonicalString());

			NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(project_name,
					cleaned.getBytes());
			node = ipfs.add(file);

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (node == null || node.size() == 0)
			return null;
		return node.get(0);
	}

	private static void testAllFiles(File curDir) {
		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isFile()) {
				try {
					if (f.getAbsolutePath().endsWith(".ifc")) {
						if (f.length() < 14055947) // less than the average size to speed up the process
							new IfcIPFSPublishingStatisticsCollector(f.getAbsolutePath());
					}
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		File testset = new File("c:\\ifc\\");
		testAllFiles(testset);

		Collections.sort(statistics);
		for (Statistics s : statistics)
			System.out.println(s);
	}

}

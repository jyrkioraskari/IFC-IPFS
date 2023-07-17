package org.lbd;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.riot.web.HttpOp;
import org.lbd.ifc.RootEntity;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.vocabulary.RDF;

import be.ugent.IfcSpfReader;

public class Splitted_NoGeometryIfcOWL {

	private  Model model;
	private  Model model_inference;
	private final Set<Statement> processed = new HashSet<>();
	private  InfModel inference_model;

	private final List<RootEntity> guid_sets = new ArrayList<>();
	private final Map<String, String> uri_guid = new HashMap<>();

	private final Map<String, Resource> rootmap = new HashMap<>();
	private final Set<String> roots = new HashSet<>();
	
	private final Set<String> geometry = new HashSet<>();
	private final Set<Resource> common = new HashSet<>();


	private int total_count = 0;
	
	long generated_id=0;

	public Splitted_NoGeometryIfcOWL(String ifc_file) {
		model = createJenaModel(ifc_file);
		model_inference = ModelFactory.createDefaultModel();
		model.listStatements().forEachRemaining(x->model_inference.add(x));
		
		Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
		inference_model = ModelFactory.createInfModel(reasoner, model_inference);

		String exp = getExpressSchema(ifc_file);
		InputStream in = null;
		try {
			HttpOp.setDefaultHttpClient(HttpClientBuilder.create().build());
			in = IfcSpfReader.class.getResourceAsStream("/" + exp + ".ttl");

			if (in == null)
				in = IfcSpfReader.class.getResourceAsStream("/resources/" + exp + ".ttl");
			inference_model.read(in, null, "TTL");
		} finally {
			try {
				in.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		split();
	}

	public Model createJenaModel(String ifc_file) {
		try {
			IfcSpfReader rj = new IfcSpfReader();
			try {

				String uriBase = "http://ipfs/bim/";
				File tempFile = File.createTempFile("ifc", ".ttl");
				Model m = ModelFactory.createDefaultModel();
				rj.convert(ifc_file, tempFile.getAbsolutePath(), uriBase);
				
				InputStream stream = new ByteArrayInputStream(FileUtils.readFileToByteArray(tempFile));
				m.read(stream, null, "TTL");
				return m;
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}
		System.out.println("IFC-RDF conversion not done");
		return ModelFactory.createDefaultModel();
	}
	private RootEntity current_root_entity;
	private final Set<Statement> current_root_entity_triples = new HashSet<>();

	long empty_item = 0;

	private void split() {
		String ifcowl = model.getNsPrefixMap().get("ifcowl");
		Resource ifcRoot = inference_model.createResource(ifcowl+ "IfcRoot");
		ResIterator rit = inference_model.listResourcesWithProperty(RDF.type, ifcRoot);
		rit.forEachRemaining(x -> {
			roots.add(x.getURI());
			rootmap.put(x.getURI(), x);

		});

		/*
		 * Naming cannot be made global.
		 */
		
		Resource ifcProductRepresentation = inference_model
				.createResource(ifcowl+ "IfcProductRepresentation");
		ResIterator rpt = inference_model.listResourcesWithProperty(RDF.type, ifcProductRepresentation);
		rpt.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});
		
		Resource ifcRepresentation = inference_model
				.createResource(ifcowl+ "IfcRepresentation");
		ResIterator rep = inference_model.listResourcesWithProperty(RDF.type, ifcRepresentation);
		rep.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});

		Resource ifcGeometricRepresentationItem = inference_model
				.createResource(ifcowl+ "IfcGeometricRepresentationItem");
		ResIterator gr = inference_model.listResourcesWithProperty(RDF.type, ifcGeometricRepresentationItem);
		gr.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});

		Resource ifcRepresentationContext = inference_model
				.createResource(ifcowl+ "IfcRepresentationContext");
		ResIterator rc = inference_model.listResourcesWithProperty(RDF.type, ifcRepresentationContext);
		rc.forEachRemaining(x -> {
			geometry.add(x.getURI());

		});

		Resource ifcRepresentationMap = inference_model
				.createResource(ifcowl+ "IfcRepresentationMap");
		ResIterator rmp = inference_model.listResourcesWithProperty(RDF.type, ifcRepresentationMap);
		rmp.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});

		Resource ifcObjectPlacement = inference_model
				.createResource(ifcowl+ "IfcObjectPlacement");
		ResIterator op = inference_model.listResourcesWithProperty(RDF.type, ifcObjectPlacement);
		op.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});

		Resource ifcSurfaceStyleShading = inference_model
				.createResource(ifcowl+ "IfcSurfaceStyleShading");
		ResIterator sss = inference_model.listResourcesWithProperty(RDF.type, ifcSurfaceStyleShading);
		sss.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});
		
		Resource ifcPresentationStyleAssignment = inference_model
				.createResource(ifcowl+ "IfcPresentationStyleAssignment");
		ResIterator psa = inference_model.listResourcesWithProperty(RDF.type, ifcPresentationStyleAssignment);
		psa.forEachRemaining(x -> {
			geometry.add(x.getURI());

		});
		
		Resource ifcPresentationLayerAssignment = inference_model
				.createResource(ifcowl+ "IfcPresentationLayerAssignment");
		ResIterator pla = inference_model.listResourcesWithProperty(RDF.type, ifcPresentationLayerAssignment);
		pla.forEachRemaining(x -> {
			geometry.add(x.getURI());

		});
		
		
		
		Resource ifcMaterial = inference_model
				.createResource(ifcowl+ "IfcMaterial");
		ResIterator material = inference_model.listResourcesWithProperty(RDF.type, ifcMaterial);
		material.forEachRemaining(x -> {
			common.add(x);
		});

		Set<Resource> unreferenced = new HashSet<>();

		
		roots.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			traverse(x);
			current_root_entity.setURI(x);
			current_root_entity.setResource(rootmap.get(x));
			current_root_entity.addTriples(current_root_entity_triples);
			if(current_root_entity.getGuid()!=null)
			   uri_guid.put(current_root_entity.getResource().getURI(), current_root_entity.getGuid());
			else
			{
			   uri_guid.put(current_root_entity.getResource().getURI(), ""+generated_id++);
			}
			guid_sets.add(current_root_entity);
			current_root_entity_triples.clear();
		});

		unreferenced.addAll(getlistOfNonGUIDSubjectsNotReferenced());
		unreferenced.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			current_root_entity.setGuid("" + empty_item++);
			traverse(x.getURI());
			current_root_entity.setURI(x.getURI());
			current_root_entity.setResource(x);
			current_root_entity.addTriples(current_root_entity_triples);
			guid_sets.add(current_root_entity);
			current_root_entity_triples.clear();
		});
		
		common.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			current_root_entity.setGuid("");
			traverse(x.getURI());
			current_root_entity.setURI(x.getURI());
			current_root_entity.setResource(x);
			current_root_entity.addTriples(current_root_entity_triples);
			guid_sets.add(current_root_entity);
			current_root_entity_triples.clear();
		});
	}

	private void traverse(String r) {
		Resource rm = model.getResource(r); // The same without inferencing
		rm.listProperties().forEachRemaining(x -> {
			if (x.getPredicate().toString().endsWith("#ownerHistory_IfcRoot"))
				return;

			if (x.getPredicate().toString().endsWith("#globalId_IfcRoot")) {
				String guid = x.getObject().asResource()
						.getProperty(model.getProperty("https://w3id.org/express#hasString")).getObject().asLiteral()
						.getLexicalForm();
				current_root_entity.setAdjustedGuid(guid); // just create a new GUIDSet Note: there should not be many
			}

			// These are version independent:
			if (x.getPredicate().toString().endsWith("#representation_IfcProduct")) // no complete graph filtering
				return;
			if (x.getPredicate().toString().endsWith("#objectPlacement_IfcProduct"))
				return;
				

			processed.add(x);

			if (current_root_entity_triples.add(x)) {
				this.total_count += 1;
				if (x.getObject().isResource()) {
					if (!roots.contains(x.getObject().asResource().getURI())&& !geometry.contains(x.getObject().asResource().getURI())&& !common.contains(x.getObject().asResource()))
						traverse(x.getObject().asResource().getURI());
				}

			}
		});

	}

	
	
	public Set<Statement> getProcessed() {
		return processed;
	}
	
	public Model getFilteredModel() {
		final Model model = ModelFactory.createDefaultModel();
		processed.stream().forEach(x->model.add(x));
		return model;
	}

	private void readInOntologyTTL(Model model, String ontology_file) {

		InputStream in = null;
		try {
			in = Splitted_NoGeometryIfcOWL.class.getResourceAsStream("/" + ontology_file);
			if (in == null) {
				try {
					in = Splitted_NoGeometryIfcOWL.class.getResourceAsStream("/resources/" + ontology_file);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
			model.read(in, null, "TTL");
			in.close();

		} catch (Exception e) {
			System.out.println("missing file: " + ontology_file);
			e.printStackTrace();
		}

	}

	public List<RootEntity> getEntitys() {
		return guid_sets;
	}

	public Map<String, String> getURI2GUID_map() {
		return uri_guid;
	}


	private Set<Resource> getlistOfNonGUIDSubjectsNotReferenced() {
		final Set<Resource> list = new HashSet<>();
		model.listStatements().forEachRemaining(x -> {
			if (!model.listStatements(null, null, x.getSubject()).hasNext())
				if (!this.rootmap.containsKey(x.getSubject().toString()))
					list.add(x.getSubject());
		});
		return list;
	}

	public Model getModel() {
		return model;
	}

	public void writeOWLFile(String output_directory) {

		try {
			FileOutputStream bufout = new FileOutputStream(new File(output_directory + "ifcOwl.ttl"));
			model.write(bufout, "TTL");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void printDifference() {
		final Set<Statement> temp = new HashSet<>();

		System.out.println("1");
		model.listStatements().forEachRemaining(x -> {
			if (x.getPredicate().toString().endsWith("#ownerHistory_IfcRoot"))
				return;
			temp.add(x);
		});

		System.out.println("2");
		processed.forEach(x -> {
			temp.remove(x);
		});

		System.out.println("3");

		System.out.println("Difference: " + temp.size());
		temp.forEach(x -> {
			System.out.println("-- " + x);
		});
		System.out.println("done");

	}

	public InfModel getInference_model() {
		return inference_model;
	}

	public int getTotal_count() {
		return total_count;
	}

	private static String getExpressSchema(String ifcFile) {
		try (FileInputStream fstream = new FileInputStream(ifcFile)) {
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			try {
				String strLine;
				while ((strLine = br.readLine()) != null) {
					if (strLine.length() > 0) {
						if (strLine.startsWith("FILE_SCHEMA")) {
							if (strLine.indexOf("IFC2X3") != -1)
								return "IFC2X3_TC1";
							if (strLine.indexOf("IFC4") != -1)
								return "IFC4_ADD1";
							else
								return "";
						}
					}
				}
			} finally {
				br.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
}

package org.lbd;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.vocabulary.RDF;
import org.ifcrdf.EventBusService;
import org.lbd.ifc.IfcFileMeasurements;
import org.lbd.ifc.RootEntity;

import com.google.common.eventbus.EventBus;

import be.ugent.IfcSpfReader;

public class IfcOWL_SegmentingStatistics_not_referenced_rule_5 {
	private final EventBus eventBus = EventBusService.getEventBus();
	private File database_file = new File("c:/jo/measures.ttl");
	private File result_file = new File("c:/jo/result5.ttl");

	private final Model jena_model = ModelFactory.createDefaultModel();
	private final Model result_model = ModelFactory.createDefaultModel();
	List<Statement> added_statements = new LinkedList<>();

	public IfcOWL_SegmentingStatistics_not_referenced_rule_5() {
		eventBus.register(this);
		if (!database_file.exists()) {
			return;
		}
		InputStream in;
		try {
			in = new FileInputStream(database_file);
			this.jena_model.read(in, null, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	public void calculate() {
		this.jena_model.listStatements().forEachRemaining(x -> {
			if (x.getPredicate().equals(IfcFileMeasurements.jena_property_ifcfile)) {
				File f = new File(x.getObject().asLiteral().getLexicalForm() + ".ttl");
				if (f.exists()) {
					try {
						handle(x.getSubject(), x.getObject().asLiteral().getLexicalForm(), f);
					} catch (Exception e) {
						Literal value = ResourceFactory.createTypedLiteral(e.getMessage());
						Statement error_statement = ResourceFactory.createStatement(x.getSubject(),
								IfcFileMeasurements.jena_property_error, value);
						this.added_statements.add(error_statement);

					}
				}
			}
		});
		added_statements.forEach(x -> result_model.add(x));
	    save_result();
	}

	private void handle(Resource ifcfile_subject, String ifc_file, File f) {
		System.out.println(ifcfile_subject);
		Model model = ModelFactory.createDefaultModel();
		System.out.println("Read in TTL: " + f.getAbsolutePath());
		RDFDataMgr.read(model, f.getAbsolutePath());
		System.out.println("TTL done");
		if(model.size()<10)
		{
			System.err.println("No triples (less than 10)");
			return;
		}

		final Set<String> roots = findRoots(ifcfile_subject, ifc_file, model);
		split(roots,model,ifcfile_subject);
		added_statements.forEach(x -> result_model.add(x));
		added_statements.clear();
	    save_result();
	}

	private Set<String> findRoots(Resource ifcfile_subject, String ifc_file, Model model) {
		Model model_inference = ModelFactory.createDefaultModel();
		model.listStatements().forEachRemaining(x->model_inference.add(x));
		
		Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
		InfModel inference_model = ModelFactory.createInfModel(reasoner, model_inference);
		System.out.println("Read in ontology");
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
		
		System.out.println("Ontology read");
		System.out.println("Counting roots:");
		String ifcowl = model.getNsPrefixMap().get("ifcowl");
		System.out.println("ifcowl: "+ifcowl);
		Resource ifcRoot = inference_model.createResource(ifcowl+ "IfcRoot");
		ResIterator rit = inference_model.listResourcesWithProperty(RDF.type, ifcRoot);
		final Map<String, Resource> rootmap = new HashMap<>();
		final Set<String> roots = new HashSet<>();
		rit.forEachRemaining(x -> {
			roots.add(x.getURI());
			rootmap.put(x.getURI(), x);
		});
		
		return roots;
	}
	RootEntity current_root_entity;
	long generated_id=0;
	long empty_item = 0;

	private void split(final Set<String> roots, Model model, Resource ifcfile_subject) {
		total_count = 0;
		final Set<Statement> current_root_entity_triples = new HashSet<>();
		final Set<Statement> processed = new HashSet<>();

		roots.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			traverse(x, model, current_root_entity_triples, processed, roots);
			current_root_entity.setURI(x);
			current_root_entity.addTriples(current_root_entity_triples);
			if (current_root_entity.getGuid() == null)
				if (current_root_entity.getResource() == null) {
					current_root_entity_triples.clear();
					return;
				}
			current_root_entity_triples.clear();
		});
		
		Set<Resource> unreferenced = new HashSet<>();
		unreferenced.addAll(getlistOfNonGUIDSubjectsNotReferenced(model, roots));
		unreferenced.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			current_root_entity.setGuid("" + empty_item++);
			traverse(x.getURI(), model, current_root_entity_triples, processed, roots);
			current_root_entity.setURI(x.getURI());
			current_root_entity.setResource(x);
			current_root_entity.addTriples(current_root_entity_triples);
			current_root_entity_triples.clear();
		});
		System.out.println("processed: " + processed.size());
		System.out.println("before: " + model.size());
		processed.stream().forEach(x -> model.remove(x));
		System.out.println("after: " + model.size());

		Literal remainder_value = ResourceFactory.createTypedLiteral(model.size());
		Statement remainder_statement = ResourceFactory.createStatement(ifcfile_subject,
				IfcFileMeasurements.jena_property_ifcowl_remainder_triplecount_notref_rule , remainder_value);
		this.added_statements.add(remainder_statement);
	}

	private int total_count = 0;

	private void traverse(String r, Model model, final Set<Statement> current_root_entity_triples,
			Set<Statement> processed, Set<String> roots) {
		Resource rm = model.getResource(r); // The same without inferencing
		rm.listProperties().forEachRemaining(x -> {
			if (x.getPredicate().toString().endsWith("#globalId_IfcRoot")) {
				String guid = x.getObject().asResource()
						.getProperty(model.getProperty("https://w3id.org/express#hasString")).getObject().asLiteral()
						.getLexicalForm();
				current_root_entity.setAdjustedGuid(guid); // just create a new GUIDSet Note: there should not be many
			}

			processed.add(x);

			if (current_root_entity_triples.add(x)) {
				this.total_count += 1;
				if (x.getObject().isResource()) {
					if (!roots.contains(x.getObject().asResource().getURI()))
						traverse(x.getObject().asResource().getURI(), model, current_root_entity_triples, processed,
								roots);
				}

			}
		});

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


	private Set<Resource> getlistOfNonGUIDSubjectsNotReferenced(Model model, Set<String> roots ) {
		final Set<Resource> list = new HashSet<>();
		model.listStatements().forEachRemaining(x -> {
			if (!model.listStatements(null, null, x.getSubject()).hasNext())
				if (!roots.contains(x.getSubject().toString()))
					list.add(x.getSubject());
		});
		return list;
	}
	
	public void save_result() {

		OutputStream out;
		try {
			out = new FileOutputStream(result_file);
			result_model.write(out, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		IfcOWL_SegmentingStatistics_not_referenced_rule_5 m = new IfcOWL_SegmentingStatistics_not_referenced_rule_5();
		m.calculate();

	}

}

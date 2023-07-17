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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

public class IfcOWL_SegmentingStatistics_10NoGeometry {
	private final EventBus eventBus = EventBusService.getEventBus();
	private File database_file = new File("c:/jo/measures.ttl");
	private File result_file = new File("c:/jo/result10NoGeo.ttl");

	private final Model jena_model = ModelFactory.createDefaultModel();
	private final Model result_model = ModelFactory.createDefaultModel();
	List<Statement> added_statements = new LinkedList<>();

	public IfcOWL_SegmentingStatistics_10NoGeometry() {
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
		Model model = ModelFactory.createDefaultModel();
		System.out.println("Read in TTL: " + f.getAbsolutePath());
		RDFDataMgr.read(model, f.getAbsolutePath());
		System.out.println("TTL done");
		if (model.size() < 10) {
			System.err.println("No triples (less than 10)");
			return;
		}
		System.out.println("org: " + model.size());
		final Set<String> roots = findRoots(ifcfile_subject, ifc_file, model);
		try {
			split(roots, model, ifcfile_subject);
		} catch (Exception e) {
			e.printStackTrace();
		}
		added_statements.forEach(x -> result_model.add(x));
		added_statements.clear();
		save_result();
	}

	String ifcowl = null;

	private Set<String> findRoots(Resource ifcfile_subject, String ifc_file, Model model) {
		Model model_inference = ModelFactory.createDefaultModel();
		model.listStatements().forEachRemaining(x -> model_inference.add(x));

		Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
		InfModel inference_model = ModelFactory.createInfModel(reasoner, model_inference);
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
		this.ifcowl = model.getNsPrefixMap().get("ifcowl");
		Resource ifcRoot = inference_model.createResource(ifcowl + "IfcRoot");
		ResIterator rit = inference_model.listResourcesWithProperty(RDF.type, ifcRoot);
		final Set<String> roots = new HashSet<>();
		rit.forEachRemaining(x -> {
			roots.add(x.getURI());
		});

		return roots;
	}

	RootEntity current_root_entity;
	long generated_id = 0;
	long empty_item = 0;
	long guid_sets = 0;

	private void split(final Set<String> roots, Model model, Resource ifcfile_subject) {
		long org_size = model.size();
		total_count = 0;
		guid_sets = 0;
		final Set<String> geometry = new HashSet<>();
		if (this.ifcowl != null) {
			Resource ifcProductRepresentation = model.createResource(ifcowl + "IfcProductRepresentation");
			ResIterator rpt = model.listResourcesWithProperty(RDF.type, ifcProductRepresentation);
			rpt.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});

			Resource ifcRepresentation = model.createResource(ifcowl + "IfcRepresentation");
			ResIterator rep = model.listResourcesWithProperty(RDF.type, ifcRepresentation);
			rep.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});

			Resource ifcGeometricRepresentationItem = model.createResource(ifcowl + "IfcGeometricRepresentationItem");
			ResIterator gr = model.listResourcesWithProperty(RDF.type, ifcGeometricRepresentationItem);
			gr.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});

			Resource ifcRepresentationContext = model.createResource(ifcowl + "IfcRepresentationContext");
			ResIterator rc = model.listResourcesWithProperty(RDF.type, ifcRepresentationContext);
			rc.forEachRemaining(x -> {
				geometry.add(x.getURI());

			});

			Resource ifcRepresentationMap = model.createResource(ifcowl + "IfcRepresentationMap");
			ResIterator rmp = model.listResourcesWithProperty(RDF.type, ifcRepresentationMap);
			rmp.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});

			Resource ifcObjectPlacement = model.createResource(ifcowl + "IfcObjectPlacement");
			ResIterator op = model.listResourcesWithProperty(RDF.type, ifcObjectPlacement);
			op.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});

			Resource ifcSurfaceStyleShading = model.createResource(ifcowl + "IfcSurfaceStyleShading");
			ResIterator sss = model.listResourcesWithProperty(RDF.type, ifcSurfaceStyleShading);
			sss.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});

			Resource ifcPresentationStyleAssignment = model.createResource(ifcowl + "IfcPresentationStyleAssignment");
			ResIterator psa = model.listResourcesWithProperty(RDF.type, ifcPresentationStyleAssignment);
			psa.forEachRemaining(x -> {
				geometry.add(x.getURI());

			});

			Resource ifcPresentationLayerAssignment = model.createResource(ifcowl + "IfcPresentationLayerAssignment");
			ResIterator pla = model.listResourcesWithProperty(RDF.type, ifcPresentationLayerAssignment);
			pla.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});

		} else {
			System.err.println("No ifowl");
			System.exit(10);
		}

		final Set<Statement> current_root_entity_triples = new HashSet<>();
		final Set<Statement> processed = new HashSet<>();

		// Tests if any triple is shared
		final Set<String> shared = new HashSet<>();
		roots.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			pre_traverse(x, model, current_root_entity_triples, processed, roots, shared,geometry);
			current_root_entity_triples.clear();
		});

		Set<Resource> unreferenced = new HashSet<>();
		unreferenced.addAll(getlistOfNonGUIDSubjectsNotReferenced(model, roots));
		shared.stream().forEach(x -> roots.add(x)); // 10
		shared.clear();

		unreferenced.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			pre_traverse(x.getURI(), model, current_root_entity_triples, processed, roots, shared,geometry);
			current_root_entity_triples.clear();
		});

		processed.clear();
		shared.stream().forEach(x -> roots.add(x)); // 10
		shared.clear();

		
		unreferenced.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			guid_sets++;
			current_root_entity.setGuid("" + empty_item++);
			traverse(x.getURI(), model, current_root_entity_triples, processed, roots,geometry);
			current_root_entity.setURI(x.getURI());
			current_root_entity.setResource(x);
			current_root_entity.addTriples(current_root_entity_triples);
			current_root_entity_triples.clear();
		});
		unreferenced.clear();

		roots.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			guid_sets++;
			traverse(x, model, current_root_entity_triples, processed, roots,geometry);
			current_root_entity.setURI(x);
			current_root_entity.addTriples(current_root_entity_triples);
			current_root_entity_triples.clear();
		});

		System.out.println("processed: " + processed.size());
		System.out.println("before: " + model.size());
		processed.stream().forEach(x -> model.remove(x));
		System.out.println("after: " + model.size());

		Literal totalcount_value = ResourceFactory.createTypedLiteral(total_count);
		Statement totalcount_statement = ResourceFactory.createStatement(ifcfile_subject,
				IfcFileMeasurements.jena_property_ifcowl_total_entity_triplescount_shared_nogeometry, totalcount_value);
		this.added_statements.add(totalcount_statement);
		System.out.println("total: " + total_count);
		System.out.println("diff: " + (total_count - org_size));
		Literal difference_value = ResourceFactory.createTypedLiteral((total_count - org_size));
		Statement difference_statement = ResourceFactory.createStatement(ifcfile_subject,
				IfcFileMeasurements.jena_property_ifcowl_overlap_triplecount__shared_nogeometry, difference_value);
		this.added_statements.add(difference_statement);  // should be a negative value because of geometry filtering

		Literal guidsetcount_value = ResourceFactory.createTypedLiteral(guid_sets);
		Statement guidsetcount_statement = ResourceFactory.createStatement(ifcfile_subject,
				IfcFileMeasurements.jena_property_ifcowl_guidsetcount_shared_nogeometry, guidsetcount_value);
		this.added_statements.add(guidsetcount_statement);
	}

	private void pre_traverse(String r, Model model, final Set<Statement> current_root_entity_triples,
			Set<Statement> processed, Set<String> roots, Set<String> shared, Set<String> geometry) {
		if(geometry.contains(r))  // Could be shared
			return;

		Resource rm = model.getResource(r); // The same without inferencing
		rm.listProperties().forEachRemaining(x -> {
			if (x.getPredicate().toString().endsWith("#ownerHistory_IfcRoot"))
				return;
			if (!processed.add(x)) {
				shared.add(x.getSubject().getURI());
				return; // only first!
			}

			if (current_root_entity_triples.add(x)) {
				if (x.getObject().isResource()) {
					if (!roots.contains(x.getObject().asResource().getURI())&& !geometry.contains(x.getObject().asResource().getURI()))
						pre_traverse(x.getObject().asResource().getURI(), model, current_root_entity_triples, processed,
								roots, shared,geometry);
				}

			}
		});

	}

	private int total_count = 0;

	private void traverse(String r, Model model, final Set<Statement> current_root_entity_triples,
			Set<Statement> processed, Set<String> roots, Set<String> geometry) {
		if(geometry.contains(r))  // Could be shared
				return;
		
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

			processed.add(x);

			if (current_root_entity_triples.add(x)) {
				this.total_count += 1;
				if (x.getObject().isResource()&& !geometry.contains(x.getObject().asResource().getURI())) {
					if (!roots.contains(x.getObject().asResource().getURI()))
						traverse(x.getObject().asResource().getURI(), model, current_root_entity_triples, processed,
								roots,geometry);
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

	private Set<Resource> getlistOfNonGUIDSubjectsNotReferenced(Model model, Set<String> roots) {
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
		IfcOWL_SegmentingStatistics_10NoGeometry m = new IfcOWL_SegmentingStatistics_10NoGeometry();
		m.calculate();

	}

}

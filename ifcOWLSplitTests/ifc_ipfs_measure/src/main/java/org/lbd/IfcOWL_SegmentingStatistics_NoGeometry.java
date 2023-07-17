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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
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

public class IfcOWL_SegmentingStatistics_NoGeometry {
	private final EventBus eventBus = EventBusService.getEventBus();
	private File database_file = new File("c:/jo/measures.ttl");

	private final Model jena_model = ModelFactory.createDefaultModel();
	
	List<Statement> added_statements=new LinkedList<>();

	public IfcOWL_SegmentingStatistics_NoGeometry() {
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
		this.jena_model.listStatements().forEachRemaining(x->
        {
			if (x.getPredicate().equals(IfcFileMeasurements.jena_property_ifcfile)) {
				File f = new File(x.getObject().asLiteral().getLexicalForm() + ".ttl");
				if (f.exists()) {
					handle(x.getSubject(),x.getObject().asLiteral().getLexicalForm(), f);
				}
			}
		});
		added_statements.forEach(x->jena_model.add(x));
		save();
	}

	private void handle(Resource ifcfile_subject, String ifc_file, File f) {
		final Set<String> roots = new HashSet<>();
		FileInputStream input;

		String exp = getExpressSchema(ifc_file);

		Model model_inference = ModelFactory.createDefaultModel();
		System.out.println("Read in TTL: " + f.getAbsolutePath());
		RDFDataMgr.read(model_inference, f.getAbsolutePath());
		System.out.println("TTL done");

		Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
		InfModel inference_model = ModelFactory.createInfModel(reasoner, model_inference);
		System.out.println("Read in ontology");

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
		
	/*
		Literal ivalue=ResourceFactory.createTypedLiteral(inference_model.size());
		Statement inferencemodel_statement= ResourceFactory.createStatement(ifcfile_subject, IfcFileMeasurements.jena_property_ifcowl_inferencemodel_triplescount, ivalue);
		this.added_statements.add(inferencemodel_statement);
*/
	}
	RootEntity current_root_entity;
	long generated_id=0;
	long empty_item = 0;
	private void split(InfModel inference_model,Model model) {
		total_count=0;
		final Map<String, Resource> rootmap = new HashMap<>();
		final Set<String> roots = new HashSet<>();
		final List<RootEntity> guid_sets = new ArrayList<>();
		
		
		final Set<Statement> current_root_entity_triples = new HashSet<>();
		empty_item = 0;
		
		final Set<String> geometry = new HashSet<>();
		final Set<Resource> common = new HashSet<>();
		final Map<String, String> uri_guid = new HashMap<>();
		final Set<Statement> processed = new HashSet<>();
		
		
		String ifcowl = inference_model.getNsPrefixMap().get("ifcowl");

		Resource ifcRoot = inference_model.createResource(ifcowl+ "IfcRoot");
		ResIterator rit = inference_model.listResourcesWithProperty(RDF.type, ifcRoot);
		rit.forEachRemaining(x -> {
			roots.add(x.getURI());
			rootmap.put(x.getURI(), x);
		});

		/*
		 * Naming cannot be made global.
		 */
		
		Resource ifcProductRepresentation = model
				.createResource(ifcRoot + "IfcProductRepresentation");
		ResIterator rpt = model.listResourcesWithProperty(RDF.type, ifcProductRepresentation);
		rpt.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});
		
		Resource ifcRepresentation = model
				.createResource(ifcRoot + "IfcRepresentation");
		ResIterator rep = model.listResourcesWithProperty(RDF.type, ifcRepresentation);
		rep.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});

		Resource ifcGeometricRepresentationItem = model
				.createResource(ifcRoot + "IfcGeometricRepresentationItem");
		ResIterator gr = model.listResourcesWithProperty(RDF.type, ifcGeometricRepresentationItem);
		gr.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});

		Resource ifcRepresentationContext = model
				.createResource(ifcRoot + "IfcRepresentationContext");
		ResIterator rc = model.listResourcesWithProperty(RDF.type, ifcRepresentationContext);
		rc.forEachRemaining(x -> {
			geometry.add(x.getURI());

		});

		Resource ifcRepresentationMap = model
				.createResource(ifcRoot + "IfcRepresentationMap");
		ResIterator rmp = model.listResourcesWithProperty(RDF.type, ifcRepresentationMap);
		rmp.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});

		Resource ifcObjectPlacement = model
				.createResource(ifcRoot + "IfcObjectPlacement");
		ResIterator op = model.listResourcesWithProperty(RDF.type, ifcObjectPlacement);
		op.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});

		Resource ifcSurfaceStyleShading = model
				.createResource(ifcRoot + "IfcSurfaceStyleShading");
		ResIterator sss = model.listResourcesWithProperty(RDF.type, ifcSurfaceStyleShading);
		sss.forEachRemaining(x -> {
			geometry.add(x.getURI());
		});
		
		Resource ifcPresentationStyleAssignment = model
				.createResource(ifcRoot + "IfcPresentationStyleAssignment");
		ResIterator psa = model.listResourcesWithProperty(RDF.type, ifcPresentationStyleAssignment);
		psa.forEachRemaining(x -> {
			geometry.add(x.getURI());

		});
		
		Resource ifcPresentationLayerAssignment = model
				.createResource(ifcRoot + "IfcPresentationLayerAssignment");
		ResIterator pla = model.listResourcesWithProperty(RDF.type, ifcPresentationLayerAssignment);
		pla.forEachRemaining(x -> {
			geometry.add(x.getURI());

		});
		
		
		
		Resource ifcMaterial = inference_model
				.createResource(ifcRoot + "IfcMaterial");
		ResIterator material = inference_model.listResourcesWithProperty(RDF.type, ifcMaterial);
		material.forEachRemaining(x -> {
			common.add(x);
		});

		Set<Resource> unreferenced = new HashSet<>();

		
		roots.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			traverse(x, model, current_root_entity_triples, processed, geometry, common, roots);
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

		unreferenced.addAll(getlistOfNonGUIDSubjectsNotReferenced(model, rootmap));
		unreferenced.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			current_root_entity.setGuid("" + empty_item++);
			traverse(x.getURI(), model, current_root_entity_triples, processed, geometry, common, roots);
			current_root_entity.setURI(x.getURI());
			current_root_entity.setResource(x);
			current_root_entity.addTriples(current_root_entity_triples);
			guid_sets.add(current_root_entity);
			current_root_entity_triples.clear();
		});
		
		common.stream().forEach(x -> {
			current_root_entity = new RootEntity();
			current_root_entity.setGuid("");
			traverse(x.getURI(), model, current_root_entity_triples, processed, geometry, common, roots);
			current_root_entity.setURI(x.getURI());
			current_root_entity.setResource(x);
			current_root_entity.addTriples(current_root_entity_triples);
			guid_sets.add(current_root_entity);
			current_root_entity_triples.clear();
		});
	}
	private int total_count = 0;
	private void traverse(String r,Model model,final Set<Statement> current_root_entity_triples,Set<Statement> processed,Set<String> geometry, Set<Resource> common, Set<String> roots ) {
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
						traverse(x.getObject().asResource().getURI(), model, current_root_entity_triples, processed, geometry, common, roots);
				}

			}
		});

	}

	private Set<Resource> getlistOfNonGUIDSubjectsNotReferenced(Model model,Map<String, Resource> rootmap ) {
		final Set<Resource> list = new HashSet<>();
		model.listStatements().forEachRemaining(x -> {
			if (!model.listStatements(null, null, x.getSubject()).hasNext())
				if (!rootmap.containsKey(x.getSubject().toString()))
					list.add(x.getSubject());
		});
		return list;
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
		IfcOWL_SegmentingStatistics_NoGeometry m = new IfcOWL_SegmentingStatistics_NoGeometry();
		m.calculate();

	}

}

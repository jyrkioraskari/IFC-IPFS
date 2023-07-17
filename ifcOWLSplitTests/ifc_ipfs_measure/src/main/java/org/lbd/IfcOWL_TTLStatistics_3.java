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

import com.google.common.eventbus.EventBus;

import be.ugent.IfcSpfReader;

public class IfcOWL_TTLStatistics_3 {
	private final EventBus eventBus = EventBusService.getEventBus();
	private File database_file = new File("c:/jo/measures.ttl");
	private File result_file = new File("c:/jo/result3.ttl");

	private final Model jena_model = ModelFactory.createDefaultModel();
	private final Model result_model = ModelFactory.createDefaultModel();
	
	List<Statement> added_statements=new LinkedList<>();

	public IfcOWL_TTLStatistics_3() {
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
		added_statements.forEach(x -> result_model.add(x));

		save_result();
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
		String ifcowl = inference_model.getNsPrefixMap().get("ifcowl");

		Resource ifcRoot = inference_model.createResource(ifcowl+ "IfcRoot");
		ResIterator rit = inference_model.listResourcesWithProperty(RDF.type, ifcRoot);
		rit.forEachRemaining(x -> {
			roots.add(x.getURI());
		});

		Literal ivalue=ResourceFactory.createTypedLiteral(inference_model.size());
		Statement inferencemodel_statement= ResourceFactory.createStatement(ifcfile_subject, IfcFileMeasurements.jena_property_ifcowl_inferencemodel_triplescount, ivalue);
		this.added_statements.add(inferencemodel_statement);

		System.out.println("roots count: " + roots.size());
		Literal value=ResourceFactory.createTypedLiteral(roots.size());
		Statement roots_statement= ResourceFactory.createStatement(ifcfile_subject, IfcFileMeasurements.jena_property_ifcowl_root_elements, value);
		this.added_statements.add(roots_statement);
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
		IfcOWL_TTLStatistics_3 m = new IfcOWL_TTLStatistics_3();
		m.calculate();

	}

}

package org.lbd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.lbd.ifc.IfcFileMeasurements;

import com.javaworld.StreamGobbler;

import io.ipfs.api.IPFS;

public class IFCtoIPFS_WholeFilesPublishMeasure {
	private final Property merkle_node;
	private final Model guid_directory_model = ModelFactory.createDefaultModel();
	private String webBase = "http://94.237.54.151:8080/serialized_simple/";

	private File database_file = new File("c:/jo/measures.ttl");
	private File result_file = new File("c:/jo/resultWF.ttl");
	private final IPFS ipfs;

	private final Model jena_model = ModelFactory.createDefaultModel();
	private final Model result_model = ModelFactory.createDefaultModel();
	List<Statement> added_statements = new LinkedList<>();

	public IFCtoIPFS_WholeFilesPublishMeasure(String directory) {
		this.merkle_node = guid_directory_model.createProperty("http://ipfs/merkle_node");
		this.ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
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

						handleifc(x.getSubject(), x.getObject().asLiteral().getLexicalForm());
					} catch (Exception e) {
						Literal value = ResourceFactory.createTypedLiteral(e.getMessage());
						Statement error_statement = ResourceFactory.createStatement(x.getSubject(),
								IfcFileMeasurements.jena_property_error, value);
						this.added_statements.add(error_statement);
					}
				}
			}
		});

		this.jena_model.listStatements().forEachRemaining(x -> {
			if (x.getPredicate().equals(IfcFileMeasurements.jena_property_ifcfile)) {
				File f = new File(x.getObject().asLiteral().getLexicalForm() + ".ttl");
				if (f.exists()) {
					try {
						handleifcowl(x.getSubject(), f.getAbsolutePath());
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

	private void handleifc(Resource ifcfile_subject, String filename) {
		System.out.println("adding: " + filename);
		long start = System.nanoTime();
		doWork("c:\\ipfs\\go-ipfs\\ipfs.exe add \"" + filename+"\"");
		float time = (System.nanoTime() - start) / 1000000f;
		Literal guidsetcount_value = ResourceFactory.createTypedLiteral(time);
		Statement guidsetcount_statement = ResourceFactory.createStatement(ifcfile_subject,
				IfcFileMeasurements.jena_property_ifc_publishing_time, guidsetcount_value);
		this.added_statements.add(guidsetcount_statement);
		System.out.println("done: " + filename);
		added_statements.forEach(x -> result_model.add(x));
		added_statements.clear();
		save_result();
	}

	private void handleifcowl(Resource ifcfile_subject, String filename) {
		System.out.println("adding: " + filename);
		long start = System.nanoTime();
		doWork("c:\\ipfs\\go-ipfs\\ipfs.exe add \"" + filename+"\"");

		float time = (System.nanoTime() - start) / 1000000f;
		Literal guidsetcount_value = ResourceFactory.createTypedLiteral(time);
		Statement guidsetcount_statement = ResourceFactory.createStatement(ifcfile_subject,
				IfcFileMeasurements.jena_property_ifcowl_publishing_time, guidsetcount_value);
		this.added_statements.add(guidsetcount_statement);
		System.out.println("done: " + filename);
		added_statements.forEach(x -> result_model.add(x));
		added_statements.clear();
		save_result();
	}

	private void doWork(String command_message) {
		System.out.println("message: " + command_message);
		try {
			String osName = System.getProperty("os.name");
			String[] cmd = new String[3];
			System.out.println(osName);
			if (osName.equals("Windows 95")) {
				cmd[0] = "command.com";
				cmd[1] = "/C";
				cmd[2] = command_message;
			} else {
				cmd[0] = "cmd.exe";
				cmd[1] = "/C";
				cmd[2] = command_message;
			}

			Runtime rt = Runtime.getRuntime();
			System.out.println("Execing " + cmd[0] + " " + cmd[1] + " " + cmd[2]);
			Process proc = rt.exec(cmd);
			// any error message?
			StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");

			// any output?
			StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			// any error???
			int exitVal = proc.waitFor();
			System.out.println("ExitValue: " + exitVal);
		} catch (Throwable t) {
			t.printStackTrace();
		}
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
		IFCtoIPFS_WholeFilesPublishMeasure p = new IFCtoIPFS_WholeFilesPublishMeasure("c:\\ifc");
		p.calculate();
	}

}

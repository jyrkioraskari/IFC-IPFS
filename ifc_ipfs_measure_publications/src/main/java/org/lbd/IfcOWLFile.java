package org.lbd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.lbd.ifc.RootEntity;
import org.lbd.rdf.IFC_NS;

import be.ugent.IfcSpfReader;

public class IfcOWLFile {

	private final Model model;

	private int triples_count=-1;

	public IfcOWLFile(String ifc_file) {
		model=createJenaModel(ifc_file);
	}
	

	public Model createJenaModel(String ifc_file) {		
		try {
			IfcSpfReader rj = new IfcSpfReader();
			try {

				String uriBase = "http://ipfs/bim/";
				Model m=ModelFactory.createDefaultModel();
				ByteArrayOutputStream stringStream=new ByteArrayOutputStream();
				rj.convert(ifc_file, stringStream, uriBase);
				InputStream stream = new ByteArrayInputStream(stringStream.toString().getBytes(StandardCharsets.UTF_8.name()));
				m.read(stream,null,"TTL");
				this.triples_count=m.listStatements().toList().size();
				try
				{
					OutputStream out;
					try {
						out = new FileOutputStream(ifc_file+".ttl");
						m.write(out, "TTL");
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
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


	public int getTriples_count() {
		return triples_count;
	}


	public void setTriples_count(int triples_count) {
		this.triples_count = triples_count;
	}


	
	

}

package org.lbd.ifc;

import java.io.File;
import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

import be.ugent.IfcSpfReader;

public class IfcOWLFile {

	private long triples_count = -1;

	public IfcOWLFile(String ifc_file) {
		createJenaModel(ifc_file) ;
	}

	private Model createJenaModel(String ifc_file) {
		IfcSpfReader rj = new IfcSpfReader();
		String uriBase = "http://ipfs/bim/";
		Model m = readAndConvertIFC(ifc_file, ifc_file + ".ttl", uriBase);
		return m;
	}

	private Model readAndConvertIFC(String ifc_file, String out_file, String uriBase) {
		try {
			IfcSpfReader rj = new IfcSpfReader();
			File outFile = new File(out_file);
			try {
				rj.convert(ifc_file, outFile.getAbsolutePath(), uriBase);

				Model m = ModelFactory.createDefaultModel();
				
				if(!outFile.exists())
				{
					System.out.println("IFC-RDF conversion not done");
					return null;
				}
				RDFDataMgr.read(m, outFile.getAbsolutePath());
				this.triples_count=m.size();
				return m;
			} catch (Exception e) {
				System.out.println(e.getMessage());
			} 

		} catch (Exception e) {
			e.printStackTrace();

		}
		System.out.println("IFC-RDF conversion not done");
		return ModelFactory.createDefaultModel();
	}

	public long getTriples_count() {
		return triples_count;
	}

	public void setTriples_count(int triples_count) {
		this.triples_count = triples_count;
	}

}

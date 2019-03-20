package org.lbd.ifc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import guidcompressor.GuidCompressor;

public class RootEntity {
	private final List<Statement> triples=new ArrayList<>();
	private final String guid;
	private String URI;
	private Resource Resource;
	
	public RootEntity(String guid) {
		super();
		this.guid = GuidCompressor.uncompressGuidString(guid);
	}
	
	public void addTriples(Set<Statement> current_triples ) {
		triples.addAll(current_triples);
	}

	public List<Statement> getTriples() {
		return triples;
	}
	public String getGuid() {
		return guid;
	}

	public String getURI() {
		return URI;
	}

	public void setURI(String uRI) {
		URI = uRI;
	}

	public Resource getResource() {
		return Resource;
	}

	public void setResource(Resource resource) {
		Resource = resource;
	}

		
}

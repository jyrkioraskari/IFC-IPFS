package org.lbd.ifc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Statement;


public class CurrentRootEntityTripleSet {
	private final List<Statement> triples = new ArrayList<>();
	private String URI;	

	public CurrentRootEntityTripleSet() {
		super();

	}

	public void addTriples(Set<Statement> current_triples) {
		triples.addAll(current_triples);
	}

	public List<Statement> getTriples() {
		return triples;
	}

	public String getURI() {
		return URI;
	}

	public void setURI(String uRI) {
		URI = uRI;
	}


}

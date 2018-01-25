package org.lbd.ifc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import guidcompressor.GuidCompressor;

/*
* The GNU Affero General Public License
* 
* Copyright (c) 2018 Jyrki Oraskari (Jyrki.Oraskari@gmail.f)
* 
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
* 
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

public class GUIDSet {
	private final List<Statement> triples=new ArrayList<>();
	private final String guid;
	private String URI;
	private Resource Resource;
	
	public GUIDSet(String guid) {
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

package org.lbd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.lbd.ifc.GUIDSet;
import org.lbd.rdf.IFC_NS;

import be.ugent.IfcSpfReader;

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

public class IfcRDFCat {

	private final Model model;

	public IfcRDFCat(String ifc_file) {
		model=addtoIFC2Repository(ifc_file);
		Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
		inference();
	}
	

	public Model addtoIFC2Repository(String ifc_file) {		
		System.out.println("IFC-RDF converter file: "+ifc_file);
		try {
			IfcSpfReader rj = new IfcSpfReader();
			try {

				String uriBase = "http://ipfs/bim/";
				Model m=ModelFactory.createDefaultModel();
				ByteArrayOutputStream stringStream=new ByteArrayOutputStream();
				long start=System.currentTimeMillis();
				rj.convert(ifc_file, stringStream, uriBase);
				IfcIPFS.current_process.setIfc_convert(System.currentTimeMillis()-start);
				InputStream stream = new ByteArrayInputStream(stringStream.toString().getBytes(StandardCharsets.UTF_8.name()));
				m.read(stream,null,"TTL");
				IfcIPFS.current_process.setIfcowl_triples(m.listStatements().toList().size());
				return m;
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}
		System.err.println("IFC-RDF conversion not done");
		return ModelFactory.createDefaultModel();
	}


	private final List<GUIDSet> guid_sets = new ArrayList<>();
	private final Map<String, String> uri_guid = new HashMap<>();

	private final Map<String, Resource> rootmap = new HashMap<>();
	private final List<String> roots = new ArrayList<>();

	private GUIDSet current;
	private final Set<Statement> current_triples = new HashSet<>();

	private void inference() {
		long start=System.currentTimeMillis();
	
		Property guid=model.getProperty("http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#globalId_IfcRoot");
		ResIterator rit = model.listResourcesWithProperty(guid);
		rit.forEachRemaining(x -> {
			roots.add(x.getURI());
			rootmap.put(x.getURI(), x);
		});

		IfcIPFS.current_process.setIfc_root_entities(roots.size());
		roots.stream().forEach(x -> {
			traverse(x);
			current.setURI(x);
			current.setResource(rootmap.get(x));
			current.addTriples(current_triples);
			uri_guid.put(current.getResource().getURI(), current.getGuid());
			guid_sets.add(current);
			IfcIPFS.current_process.addFiltered_triples(current_triples.size());
			current_triples.clear();
		});
		IfcIPFS.current_process.setIfc_split(System.currentTimeMillis()-start);

	}



	private void traverse(String r) {
		Resource rm = model.getResource(r); // The same without inferencing
		rm.listProperties().forEachRemaining(x -> {
			if (x.getPredicate().toString().contains("http://www.w3.org/2002/07/owl"))
				return;
			if (x.getPredicate().toString().contains("http://www.w3.org/2000/01/rdf-schema#subClassOf"))
				return;
			if (x.getPredicate().toString().contains("http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#representation_IfcProduct"))
				return;
			if (x.getPredicate().toString().contains("http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#objectPlacement_IfcProduct"))
				return;
			if (x.getPredicate().toString().contains("http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#ownerHistory_IfcRoot"))
				return;
			if (x.getObject().toString().contains("http://www.w3.org/2002/07/owl#Class"))
				return;

			if (x.getPredicate().toString().contains("http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#globalId_IfcRoot")) {
				String guid = x.getObject().asResource().getProperty(model.getProperty("https://w3id.org/express#hasString")).getObject().asLiteral().getLexicalForm();
				current = new GUIDSet(guid);  // just create a new GUIDSet Note: there should not be many
			}

			if (current_triples.add(x)) {
				if (x.getObject().isResource()) {
					if (!roots.contains(x.getObject().asResource().getURI()))
						traverse(x.getObject().asResource().getURI());
				}

			}
		});

	}

	public List<GUIDSet> getGuid_sets() {
		return guid_sets;
	}

	public Map<String, String> getUri_guid() {
		return uri_guid;
	}

}

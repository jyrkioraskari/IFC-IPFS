package org.lbd;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.vocabulary.RDF;
import org.lbd.ifc.CurrentRootEntityTripleSet;
import org.lbd.ifc.IfcOWLFile;

import be.ugent.IfcSpfReader;

/*
* The GNU Affero General Public License
* 
* Copyright (c) 2018, 2019 Jyrki Oraskari (Jyrki.Oraskari@aalto.fi)
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

public class IfcOWL_Segmenting_Rule10NG {
	final String ifc_file;
	final IPFSPublisherInterface caller;
	long model_size = 0;

	public IfcOWL_Segmenting_Rule10NG(String ifc_file, IPFSPublisherInterface caller) {
		this.ifc_file = ifc_file;
		this.caller = caller;
		IfcOWLFile ifcowl = new IfcOWLFile(ifc_file);
		this.model_size = ifcowl.getTriples_count();
	}

	public void split_and_publish() {

		if (this.model_size > 0) {
			File f = new File(ifc_file + ".ttl");
			handle(ifc_file, f);

		}
	}

	private Set<String> roots;

	private void handle(String ifc_file, File f) {
		Model model = ModelFactory.createDefaultModel();
		System.out.println("Read in TTL: " + f.getAbsolutePath());
		RDFDataMgr.read(model, f.getAbsolutePath());
		System.out.println("TTL done");
		if (model.size() < 10) {
			System.err.println("No triples (less than 10)");
			return;
		}
		System.out.println("org: " + model.size());
		this.roots = findRoots(ifc_file, model);
		try {
			split(roots, model);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	String ifcowl = null;

	private Set<String> findRoots(String ifc_file, Model model) {
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

			x.listProperties().forEachRemaining(y -> {
				if (y.getPredicate().toString().endsWith("#globalId_IfcRoot")) {
					String guid = y.getObject().asResource()
							.getProperty(model.getProperty("https://w3id.org/express#hasString")).getObject()
							.asLiteral().getLexicalForm();
					try {
						guid = URLEncoder.encode(guid, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					this.caller.createRootResource(x.getURI().toString(), guid);
				}
			});

		});

		return roots;
	}

	CurrentRootEntityTripleSet current_root_entity;
	long generated_id = 0;
	long empty_item = 0;
	long guid_sets = 0;

	private void split(final Set<String> roots, Model model) {
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
			
			Resource ifcMaterialDefinitionRepresentation = model.createResource(ifcowl + "IfcMaterialDefinitionRepresentation");
			ResIterator mdr = model.listResourcesWithProperty(RDF.type, ifcMaterialDefinitionRepresentation);
			mdr.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});
			
			Resource ifcProductDefinitionShape = model.createResource(ifcowl + "IfcProductDefinitionShape");
			ResIterator pds = model.listResourcesWithProperty(RDF.type, ifcProductDefinitionShape);
			pds.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});
			

			Resource ifcRepresentation = model.createResource(ifcowl + "IfcRepresentation");
			ResIterator rep = model.listResourcesWithProperty(RDF.type, ifcRepresentation);
			rep.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});

			Resource ifcStyleModel = model.createResource(ifcowl + "IfcStyleModel");
			ResIterator sm = model.listResourcesWithProperty(RDF.type, ifcStyleModel);
			sm.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});
			
			Resource ifcShapeRepresentation = model.createResource(ifcowl + "IfcShapeRepresentation");
			ResIterator sr = model.listResourcesWithProperty(RDF.type, ifcShapeRepresentation);
			sr.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});

			Resource ifcTopologyRepresentation = model.createResource(ifcowl + "IfcTopologyRepresentation");
			ResIterator tr = model.listResourcesWithProperty(RDF.type, ifcTopologyRepresentation);
			tr.forEachRemaining(x -> {
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

			// No inheritance used here
			Resource ifcObjectPlacement = model.createResource(ifcowl + "IfcObjectPlacement");
			ResIterator op = model.listResourcesWithProperty(RDF.type, ifcObjectPlacement);
			op.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});

			Resource ifcGridPlacement = model.createResource(ifcowl + "IfcGridPlacement");
			ResIterator gp = model.listResourcesWithProperty(RDF.type, ifcGridPlacement);
			gp.forEachRemaining(x -> {
				geometry.add(x.getURI());
			});
			
			Resource ifcLocalPlacement = model.createResource(ifcowl + "IfcLocalPlacement");
			ResIterator lp = model.listResourcesWithProperty(RDF.type, ifcLocalPlacement);
			lp.forEachRemaining(x -> {
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
			current_root_entity = new CurrentRootEntityTripleSet();
			pre_traverse(x, model, current_root_entity_triples, processed, roots, shared,geometry);
			current_root_entity_triples.clear();
		});

		Set<Resource> unreferenced = new HashSet<>();
		unreferenced.addAll(getlistOfNonGUIDSubjectsNotReferenced(model, roots));
		//shared.stream().forEach(x -> roots.add(x)); // 10
		//shared.clear();

		unreferenced.stream().forEach(x -> {
			current_root_entity = new CurrentRootEntityTripleSet();
			pre_traverse(x.getURI(), model, current_root_entity_triples, processed, roots, shared,geometry);
			current_root_entity_triples.clear();
		});

		processed.clear();
		//shared.stream().forEach(x -> roots.add(x)); // 10
		//shared.clear();

		unreferenced.stream().forEach(x -> {
			current_root_entity = new CurrentRootEntityTripleSet();
			guid_sets++;
			boolean ret=traverse(x.getURI(), model, current_root_entity_triples, processed, roots, shared, geometry);
			current_root_entity.setURI(x.getURI());
			current_root_entity.addTriples(current_root_entity_triples);
			if(ret==false)
			  this.caller.publishEntityNode2IPFS(current_root_entity);
			current_root_entity_triples.clear();
		});
		unreferenced.clear();
		
		// No roots are filtered
		roots.stream().forEach(x -> {
			current_root_entity = new CurrentRootEntityTripleSet();
			guid_sets++;
			boolean ret=traverse(x, model, current_root_entity_triples, processed, roots, shared, geometry);
			current_root_entity.setURI(x);
			current_root_entity.addTriples(current_root_entity_triples);			
			this.caller.publishEntityNode2IPFS(current_root_entity);
			current_root_entity_triples.clear();
		});
		
		shared.stream().forEach(x -> {
			current_root_entity = new CurrentRootEntityTripleSet();
			guid_sets++;
			boolean ret=traverse(x, model, current_root_entity_triples, processed, roots, shared, geometry);
			current_root_entity.setURI(x);
			current_root_entity.addTriples(current_root_entity_triples);
			if(ret==false)
			  this.caller.publishEntityNode2IPFS(current_root_entity);
			current_root_entity_triples.clear();
		});

		processed.stream().forEach(x -> model.remove(x));
	}

	private void pre_traverse(String r, Model model, final Set<Statement> current_root_entity_triples,
			Set<Statement> processed, Set<String> roots, Set<String> shared, Set<String> geometry) {
		Resource rm = model.getResource(r); // The same without inferencing
		rm.listProperties().forEachRemaining(x -> {

			if (!processed.add(x) && !geometry.contains(x.getSubject().getURI())) {  // the comparison takes time
				  shared.add(x.getSubject().getURI());
				return; // only first!
			}
			
			if (current_root_entity_triples.add(x)) {
				if (x.getObject().isResource()&& !geometry.contains(x.getObject().asResource().getURI())&& !shared.contains(x.getObject().asResource().getURI()))  {
					if (!roots.contains(x.getObject().asResource().getURI()))
						pre_traverse(x.getObject().asResource().getURI(), model, current_root_entity_triples, processed,
								roots, shared,geometry);
				}

			}
		});

	}

	private int total_count = 0;
	private boolean traverse(String r, Model model, final Set<Statement> current_root_entity_triples,
			Set<Statement> processed, Set<String> roots,  Set<String> shared, Set<String> geometry) {
		if(geometry.contains(r))  // Could be shared
				return true;
		
		Resource rm = model.getResource(r); // The same without inferencing
		
		StmtIterator iterator=rm.listProperties();
		while(iterator.hasNext())
		{
			Statement x=iterator.next();
			if (x.getPredicate().toString().endsWith("#ownerHistory_IfcRoot"))
				continue;
			if(x.getObject().isResource()&&geometry.contains(x.getObject().asResource().getURI()))
				return true;
			if(x.getObject().isResource()&&shared.contains(x.getObject().asResource().getURI()))
				return true;
			Resource IfcCartesianPoint = model.createResource(this.ifcowl + "IfcCartesianPoint");
			if(x.getPredicate().equals(RDF.type))
			  if(x.getObject()==IfcCartesianPoint)
				  return true;

			if (current_root_entity_triples.add(x)) {
				this.total_count += 1;
				if (x.getObject().isResource()) {
					if (!roots.contains(x.getObject().asResource().getURI()))
						traverse(x.getObject().asResource().getURI(), model, current_root_entity_triples, processed,
								roots, shared,geometry);
				}

			}
		
		}
        return false;
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

	public long getTotal_count() {
		return this.total_count;
	}

	public long getRoots_size() {
		if(roots==null)
			return -1;
		return roots.size();
	}

	public long getModel_size() {
		return this.model_size;
	}

}

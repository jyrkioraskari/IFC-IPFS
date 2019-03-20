package org.lbd.ifc;

import java.io.File;
import java.util.Optional;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;

public class IfcFileMeasurements {

	static public final Property jena_property_ifcfile = ResourceFactory.createProperty("http://measurements/ifcfile");

	static public final Property jena_property_ifcowl_triplescount = ResourceFactory
			.createProperty("http://measurements/ifcowl_triplescount");
	static public final Property jena_property_ifcowl_triplescountWithOwnerHistory = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_triplescountWithOwnerHistory");

	static public final Property jena_property_ifcowl_root_elements = ResourceFactory
			.createProperty("http://measurements/ifcowl_root_elements");
	static public final Property jena_property_ifcowl_entity_triples_min = ResourceFactory
			.createProperty("http://measurements/ifcowl_entity_triples_min");
	static public final Property jena_property_ifcowl_entity_triples_max = ResourceFactory
			.createProperty("http://measurements/ifcowl_entity_triples_max");

	
	static public final Property jena_property_ifcowl_entity_triples_min_filtered = ResourceFactory
			.createProperty("http://measurements/ifcowl_entity_triples_min_filtered");
	static public final Property jena_property_ifcowl_entity_triples_max_filtered = ResourceFactory
			.createProperty("http://measurements/ifcowl_entity_triples_max_filtered");


	
	static public final Property jena_property_ifcowl_version = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_version");

	
	
	
	static public final Property jena_property_ipfs_entity_nodesize_min_filtered = ResourceFactory
			.createProperty("http://measurements/ipfs_entity_nodesize_min_filtered");
	static public final Property jena_property_ipfs_entity_nodesize_max_filtered = ResourceFactory
			.createProperty("http://measurements/ipfs_entity_nodesize_max_filtered");
	
	static public final Property jena_property_filtered_ipfs_one_block_percentage = ResourceFactory
			.createProperty("http://measurements/jena_property_filtered_ipfs_one_block_percentage");

	static public final Property jena_property_ipfs_one_block_percentage = ResourceFactory
			.createProperty("http://measurements/jena_property_ipfs_one_block_percentage");

	
	
	private Optional ifcfile = Optional.empty();
	private Optional ifcowl_version = Optional.empty();
	private Optional ifcowl_triplescount = Optional.empty();
	
	private Optional root_elements = Optional.empty();
	private Optional entity_triples_min = Optional.empty();
	private Optional entity_triples_max = Optional.empty();

	private Optional entity_triples_min_filtered = Optional.empty();
	private Optional entity_triples_max_filtered = Optional.empty();

	

	private Optional entity_nodesize_min_filtered = Optional.empty();
	private Optional entity_nodesize_max_filtered = Optional.empty();
	
	
	private Optional filtered_ipfs_one_block_percentage = Optional.empty();
	private Optional ipfs_one_block_percentage = Optional.empty();
	
	public IfcFileMeasurements(Resource jena_model_resource) {

		if (jena_model_resource.hasProperty(jena_property_ifcfile)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcfile);

			try {
				String[] f = new File(ret.getObject().asLiteral().getLexicalForm()).getName().split("\\.");
				ifcfile = Optional.of(f[0].replaceAll("_", " "));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (jena_model_resource.hasProperty(jena_property_ifcowl_version)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_version);
			ifcowl_version = Optional.of(ret.getObject().asLiteral().getLexicalForm());
		}
		
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_triplescount)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_triplescount);
			ifcowl_triplescount = Optional.of(ret.getObject().asLiteral().getLong());
		}


		if (jena_model_resource.hasProperty(jena_property_ifcowl_root_elements)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_root_elements);
			root_elements = Optional.of(ret.getObject().asLiteral().getLong());
		}

		if (jena_model_resource.hasProperty(jena_property_ifcowl_entity_triples_min)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_entity_triples_min);
			entity_triples_min = Optional.of(ret.getObject().asLiteral().getLong());
		}

		if (jena_model_resource.hasProperty(jena_property_ifcowl_entity_triples_max)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_entity_triples_max);
			entity_triples_max = Optional.of(ret.getObject().asLiteral().getLong());
		}
		
		
		

		if (jena_model_resource.hasProperty(jena_property_ifcowl_entity_triples_min_filtered)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_entity_triples_min_filtered);
			entity_triples_min_filtered = Optional.of(ret.getObject().asLiteral().getLong());
		}

		if (jena_model_resource.hasProperty(jena_property_ifcowl_entity_triples_max_filtered)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_entity_triples_max_filtered);
			entity_triples_max_filtered = Optional.of(ret.getObject().asLiteral().getLong());
		}
	
		
		if (jena_model_resource.hasProperty(jena_property_ipfs_entity_nodesize_min_filtered)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ipfs_entity_nodesize_min_filtered);
			entity_nodesize_min_filtered = Optional.of(ret.getObject().asLiteral().getLong());
		}

		if (jena_model_resource.hasProperty(jena_property_ipfs_entity_nodesize_max_filtered)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ipfs_entity_nodesize_max_filtered);
			entity_nodesize_max_filtered = Optional.of(ret.getObject().asLiteral().getLong());
		}
		
		

		if (jena_model_resource.hasProperty(jena_property_filtered_ipfs_one_block_percentage)) {
			Statement ret = jena_model_resource.getProperty(jena_property_filtered_ipfs_one_block_percentage);
			filtered_ipfs_one_block_percentage = Optional.of(ret.getObject().asLiteral().getFloat());
		}
		
		if (jena_model_resource.hasProperty(jena_property_ipfs_one_block_percentage)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ipfs_one_block_percentage);
			ipfs_one_block_percentage = Optional.of(ret.getObject().asLiteral().getFloat());
		}


	}

	public String getIfcfile() {
		return ifcfile.orElse("").toString();
	}

	public void setIfcfile(String ifcfile) {
		this.ifcfile = Optional.of(ifcfile);
	}
	
	
	public Long getIfcowl_triplescount() {
		return (long) ifcowl_triplescount.orElse(null);
	}

	public void setIfcowl_triplescount(Optional ifcowl_triplescount) {
		this.ifcowl_triplescount = Optional.of(ifcowl_triplescount);
	}

	public Long getRoot_elements() {
		return (long) root_elements.orElse(null);
	}

	public void setRoot_elements(long root_elements) {
		this.root_elements = Optional.of(root_elements);
	}

	public Long getEntity_triples_min() {
		return (long) entity_triples_min.orElse(null);
	}

	public void setEntity_triples_min(long entity_triples_min) {
		this.entity_triples_min = Optional.of(entity_triples_min);
	}

	public long getEntity_triples_max() {
		return (long) entity_triples_max.orElse(null);
	}

	public void setEntity_triples_max(long entity_triples_max) {
		this.entity_triples_max = Optional.of(entity_triples_max);
	}

	public String toString() {
		return ifcfile.orElse("") + ";" + ifcowl_version.orElse("") + ";" + ifcowl_triplescount.orElse("") +";" + root_elements.orElse("") + ";" + entity_triples_min.orElse("") + ";"
				+ entity_triples_max.orElse("") + ";";
	}

	static public String headers() {
		return "Model;Ifc Version; IfcOWL triples;IfcOWL root elements;Min number of IfcOWL entity triples;Max number of IfcOWL entity triples;";
	}
}
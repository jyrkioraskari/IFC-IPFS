package org.lbd.ifc;

import java.io.File;
import java.util.Optional;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
@SuppressWarnings("rawtypes")


public class IfcFileMeasurements {

	static public final Property jena_property_ifcfile = ResourceFactory.createProperty("http://measurements/ifcfile");
	static public final Property jena_property_ifc_version = ResourceFactory
			.createProperty("http://measurements/ifc_version");
	
	static public final Property jena_property_ifc_filesize = ResourceFactory
			.createProperty("http://measurements/ifc_filesize");
	
	static public final Property jena_property_ifc2rdf_error = ResourceFactory
			.createProperty("http://measurements/ifc2rdf_error");

	static public final Property jena_property_ifcowl_filesize = ResourceFactory
			.createProperty("http://measurements/ifcowl_filesize");

	static public final Property jena_property_ifcowl_triplescount = ResourceFactory
			.createProperty("http://measurements/ifcowl_triplescount");
	
	static public final Property jena_property_ifcowl_inferencemodel_triplescount = ResourceFactory
			.createProperty("http://measurements/ifcowl_inferencemodel_triplescount");
	
	static public final Property jena_property_ifcowl_root_elements = ResourceFactory
			.createProperty("http://measurements/ifcowl_root_elements");
	
	
	static public final Property jena_property_ifcowl_remainder_triplecount = ResourceFactory
			.createProperty("http://measurements/ifcowl_remainer_triplecount");
	
	static public final Property jena_property_ifcowl_remainder_triplecount_notref_rule = ResourceFactory
			.createProperty("http://measurements/ifcowl_remainder_triplecount_notref_rule");
	
	
	static public final Property jena_property_ifcowl_total_entity_triplescount_basic = ResourceFactory
			.createProperty("http://measurements/ifcowl_total_entity_triplescount_basic");
	
	static public final Property jena_property_ifcowl_total_entity_triplescount_rmshared  = ResourceFactory
			.createProperty("http://measurements/ifcowl_total_entity_triplescount_rmshared");
	
	static public final Property jena_property_ifcowl_total_entity_triplescount_allow_measurement_attributes = ResourceFactory
			.createProperty("http://measurements/ifcowl_total_entity_triplescount_allow_measurement_attributes");
	
	static public final Property jena_property_ifcowl_total_entity_triplescount_shared_nogeometry  = ResourceFactory
			.createProperty("http://measurements/ifcowl_total_entity_triplescount_shared_nogeometry");
	
	
	static public final Property jena_property_ifcowl_overlap_triplecount_basic = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_overlap_triplecount_basic");
	
	static public final Property jena_property_ifcowl_overlap_triplecount_rmshared = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_overlap_triplecount_rmshared");
	
		
	static public final Property jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes");
	
	
	static public final Property jena_property_ifcowl_overlap_triplecount__shared_nogeometry = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_overlap_triplecount__shared_nogeometry");
	
	
	static public final Property jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_3 = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_3");
	static public final Property jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_4 = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_4");
	static public final Property jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_6 = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_6");
	static public final Property jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_7 = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_7");
	
	
	static public final Property jena_property_ifcowl_guidsetcount_basic = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_guidsetcountt_basic");
	
	static public final Property jena_property_ifcowl_guidsetcount_rmshared  = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_overlap_guidsetcount_rmshared");
	
	static public final Property jena_property_ifcowl_guidsetcount_allow_measurement_attributes = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_guidsetcount_allow_measurement_attributes");
	
	static public final Property jena_property_ifcowl_guidsetcount_allow_measurement_attributes_3 = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_guidsetcount_allow_measurement_attributes_3");
	static public final Property jena_property_ifcowl_guidsetcount_allow_measurement_attributes_4 = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_guidsetcount_allow_measurement_attributes_4");
	static public final Property jena_property_ifcowl_guidsetcount_allow_measurement_attributes_6 = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_guidsetcount_allow_measurement_attributes_6");
	static public final Property jena_property_ifcowl_guidsetcount_allow_measurement_attributes_7 = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_guidsetcount_allow_measurement_attributes_7");
	
	static public final Property jena_property_ifcowl_guidsetcount_shared_nogeometry  = ResourceFactory
			.createProperty("http://measurements/jena_property_ifcowl_overlap_guidsetcount_shared_nogeometry");
	
	
	static public final Property jena_property_error = ResourceFactory
			.createProperty("http://measurements/error");
	
	static public final Property jena_property_ifc2rdf_message = ResourceFactory
			.createProperty("http://measurements/ifc2rdf_message");
	
	
	static public final Property jena_property_ifc_publishing_time = ResourceFactory
			.createProperty("http://measurements/ifc_publishing_time");
	
	static public final Property jena_property_ifcowl_publishing_time = ResourceFactory
			.createProperty("http://measurements/ifcowl_publishing_time");
	
	private Optional ifcfile = Optional.empty();
	private Optional ifc_version = Optional.empty();
	
	private Optional ifc_filesize = Optional.empty();
	private Optional ifc2rdf_error = Optional.empty();
	private Optional ifcowl_filesize = Optional.empty();
	
	private Optional ifcowl_triplescount = Optional.empty();	
	private Optional ifcowl_inferencemodel_triplescount = Optional.empty();	
	private Optional root_elements = Optional.empty();
	
	
	private Optional ifcowl_remainder_entity_triplescount = Optional.empty();
	private Optional ifcowl_remainder_triplecount_notref_rulet = Optional.empty();
	
	
	private Optional ifcowl_overlap_triplecount_basic = Optional.empty();
	private Optional ifcowl_overlap_triplecount_rmshared  = Optional.empty();
	private Optional ifcowl_overlap_triplecount_allow_measurement_attributes = Optional.empty();
	
	private Optional ifcowl_overlap_triplecount__shared_nogeometry  = Optional.empty();
	
	private Optional ifcowl_overlap_triplecount_allow_measurement_attributes_3 = Optional.empty();
	private Optional ifcowl_overlap_triplecount_allow_measurement_attributes_4 = Optional.empty();
	private Optional ifcowl_overlap_triplecount_allow_measurement_attributes_6 = Optional.empty();
	private Optional ifcowl_overlap_triplecount_allow_measurement_attributes_7 = Optional.empty();
	
	
	private Optional ifcowl_guidsetcount_basic = Optional.empty();
	private Optional ifcowl_guidsetcount_rmshared = Optional.empty();
	private Optional ifcowl_guidsetcount_allow_measurement_attributes = Optional.empty();
	
	private Optional ifcowl_guidsetcount_shared_nogeometry = Optional.empty();
	
	private Optional ifcowl_guidsetcount_allow_measurement_attributes_3 = Optional.empty();
	private Optional ifcowl_guidsetcount_allow_measurement_attributes_4 = Optional.empty();
	private Optional ifcowl_guidsetcount_allow_measurement_attributes_6 = Optional.empty();
	private Optional ifcowl_guidsetcount_allow_measurement_attributes_7 = Optional.empty();
	
	
	private Optional ifcowl_total_entity_triplescount_shared_nogeometry = Optional.empty();
	
	
	private Optional ifc_publishing_time = Optional.empty();
	private Optional ifcowl_publishing_time = Optional.empty();
	
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
		
		if (jena_model_resource.hasProperty(jena_property_ifc_version)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifc_version);
			ifc_version = Optional.of(ret.getObject().asLiteral().getLexicalForm());
		}

		if (jena_model_resource.hasProperty(jena_property_ifc_filesize)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifc_filesize);
			ifc_filesize = Optional.of(ret.getObject().asLiteral().getLong());
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifc2rdf_error)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifc2rdf_error);
			ifc2rdf_error = Optional.of(ret.getObject().asLiteral().getLexicalForm());
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_filesize)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_filesize);
			ifcowl_filesize = Optional.of(ret.getObject().asLiteral().getLong());
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_triplescount)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_triplescount);
			long count=ret.getObject().asLiteral().getLong();
			if(count>0)				
			ifcowl_triplescount = Optional.of(count);
		}

		if (jena_model_resource.hasProperty(jena_property_ifcowl_inferencemodel_triplescount)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_inferencemodel_triplescount);
			long count=ret.getObject().asLiteral().getLong();
			if(count>0)				
			ifcowl_inferencemodel_triplescount = Optional.of(count);
		}

		if (jena_model_resource.hasProperty(jena_property_ifcowl_root_elements)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_root_elements);
			try {
				root_elements = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}

		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_remainder_triplecount)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_remainder_triplecount);
			try {
				ifcowl_remainder_entity_triplescount = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_remainder_triplecount_notref_rule)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_remainder_triplecount_notref_rule);
			try {
				ifcowl_remainder_triplecount_notref_rulet = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_overlap_triplecount_basic)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_overlap_triplecount_basic);
			try {
				ifcowl_overlap_triplecount_basic = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}

		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_overlap_triplecount_rmshared)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_overlap_triplecount_rmshared);
			try {
				ifcowl_overlap_triplecount_rmshared = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes);
			try {
				ifcowl_overlap_triplecount_allow_measurement_attributes = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_overlap_triplecount__shared_nogeometry)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_overlap_triplecount__shared_nogeometry);
			try {
				ifcowl_overlap_triplecount__shared_nogeometry = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_3)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_3);
			try {
				ifcowl_overlap_triplecount_allow_measurement_attributes_3 = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		

		if (jena_model_resource.hasProperty(jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_4)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_4);
			try {
				ifcowl_overlap_triplecount_allow_measurement_attributes_4 = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_6)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_6);
			try {
				ifcowl_overlap_triplecount_allow_measurement_attributes_6 = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_7)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_overlap_triplecount_allow_measurement_attributes_7);
			try {
				ifcowl_overlap_triplecount_allow_measurement_attributes_7 = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_guidsetcount_basic)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_guidsetcount_basic);
			try {
				ifcowl_guidsetcount_basic = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}

		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_guidsetcount_rmshared)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_guidsetcount_rmshared);
			try {
				ifcowl_guidsetcount_rmshared = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_guidsetcount_allow_measurement_attributes)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_guidsetcount_allow_measurement_attributes);
			try {
				ifcowl_guidsetcount_allow_measurement_attributes = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_guidsetcount_shared_nogeometry)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_guidsetcount_shared_nogeometry);
			try {
				ifcowl_guidsetcount_shared_nogeometry = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_guidsetcount_allow_measurement_attributes_3)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_guidsetcount_allow_measurement_attributes_3);
			try {
				ifcowl_guidsetcount_allow_measurement_attributes_3 = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_guidsetcount_allow_measurement_attributes_4)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_guidsetcount_allow_measurement_attributes_4);
			try {
				ifcowl_guidsetcount_allow_measurement_attributes_4 = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_guidsetcount_allow_measurement_attributes_6)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_guidsetcount_allow_measurement_attributes_6);
			try {
				ifcowl_guidsetcount_allow_measurement_attributes_6 = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_guidsetcount_allow_measurement_attributes_7)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_guidsetcount_allow_measurement_attributes_7);
			try {
				ifcowl_guidsetcount_allow_measurement_attributes_7 = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_total_entity_triplescount_shared_nogeometry)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_total_entity_triplescount_shared_nogeometry);
			try {
				ifcowl_total_entity_triplescount_shared_nogeometry = Optional.of(ret.getObject().asLiteral().getLong());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
	
		if (jena_model_resource.hasProperty(jena_property_ifc_publishing_time)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifc_publishing_time);
			try {
				ifc_publishing_time = Optional.of(ret.getObject().asLiteral().getFloat());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		if (jena_model_resource.hasProperty(jena_property_ifcowl_publishing_time)) {
			Statement ret = jena_model_resource.getProperty(jena_property_ifcowl_publishing_time);
			try {
				ifcowl_publishing_time = Optional.of(ret.getObject().asLiteral().getFloat());
			}
			catch (Exception e) {
				//System.err.println(e.getMessage());
			}
		}
		
		
	}

	

	public Optional getIfcowl_triplescount() {
		return ifcowl_triplescount;
	}



	public String toString() {
		StringBuilder ret=new StringBuilder();
		ret.append( ifcfile.orElse("") + "," );
		ret.append( ifc_version.orElse("") + "," );
		ret.append( ifc_filesize.orElse("") + "," );
		ret.append( ifc2rdf_error.orElse("") + "," );
		ret.append( ifcowl_filesize.orElse("") + "," );
		ret.append( ifcowl_triplescount.orElse("") +"," );
		ret.append( ifcowl_inferencemodel_triplescount.orElse("") +"," );
		ret.append( root_elements.orElse("") + "," );
		ret.append( ifcowl_remainder_entity_triplescount.orElse("") + "," );
		ret.append( ifcowl_remainder_triplecount_notref_rulet.orElse("") + "," );
		
		ret.append( ifcowl_overlap_triplecount_basic.orElse("") + "," );
		ret.append( ifcowl_overlap_triplecount_rmshared.orElse("") + "," );
		ret.append( ifcowl_overlap_triplecount_allow_measurement_attributes_3.orElse("") + "," );
		ret.append( ifcowl_overlap_triplecount_allow_measurement_attributes_4.orElse("") + "," );
		ret.append( ifcowl_overlap_triplecount_allow_measurement_attributes.orElse("") + "," );
		ret.append( ifcowl_overlap_triplecount_allow_measurement_attributes_6.orElse("") + "," );
		ret.append( ifcowl_overlap_triplecount_allow_measurement_attributes_7.orElse("") + "," );
		
		ret.append( ifcowl_guidsetcount_basic.orElse("") + "," );
		ret.append( ifcowl_guidsetcount_rmshared.orElse("") + "," );
		ret.append( ifcowl_guidsetcount_shared_nogeometry.orElse("") + "," );
		
		ret.append( ifcowl_guidsetcount_allow_measurement_attributes_3.orElse("") + "," );
		ret.append( ifcowl_guidsetcount_allow_measurement_attributes_4.orElse("") + "," );
		ret.append( ifcowl_guidsetcount_allow_measurement_attributes.orElse("") + "," );
		ret.append( ifcowl_guidsetcount_allow_measurement_attributes_6.orElse("") + "," );
		ret.append( ifcowl_guidsetcount_allow_measurement_attributes_7.orElse("") + "," );
		
		ret.append( ifcowl_total_entity_triplescount_shared_nogeometry.orElse("") + "," );
		
		ret.append( ifc_publishing_time.orElse("") + "," );
		ret.append( ifcowl_publishing_time.orElse("") + "," );
		return  ret.toString();
	}

	static public String headers() {
		StringBuilder ret=new StringBuilder();
		ret.append(  "Model,");
		ret.append(  "Ifc Version,");
		ret.append(  "IFC filesize,");
		ret.append(  "IFCtoRDF errors,");
		ret.append(  "IfcOWL filesize,");
		ret.append(  "IfcOWL triples,");
		ret.append(  "Inference model triples,");
		ret.append(  "IfcOWL root elements,");
		ret.append(  "ifcowl remainder entity triplescount,");
		ret.append(  "ifcowl remainder triplecount notref rulet,");
		
		ret.append(  "ifcowl overlap triplecount basic,");
		ret.append(  "ifcowl overlap triplecount rmshared,");
		ret.append(  "ifcowl overlap triplecount allow measurement attributes <3,");
		ret.append(  "ifcowl overlap triplecount allow measurement attributes <4,");
		ret.append(  "ifcowl overlap triplecount allow measurement attributes <5,");
		ret.append(  "ifcowl overlap triplecount allow measurement attributes <6,");
		ret.append(  "ifcowl overlap triplecount allow measurement attributes <7,");
		
		ret.append(  "ifcowl guidsetcount basic,");
		ret.append(  "ifcowl guidsetcount rmshared,");
		ret.append(  "ifcowl guidsetcount shared nogeometry,");
		
		ret.append(  "ifcowl guidsetcount allow measurement attributes <3,");
		ret.append(  "ifcowl guidsetcount allow measurement attributes <4,");
		ret.append(  "ifcowl guidsetcount allow measurement attributes <5,");
		ret.append(  "ifcowl guidsetcount allow measurement attributes <6,");
		ret.append(  "ifcowl guidsetcount allow measurement attributes <7,");
		
		ret.append(  "ifcowl total entity triplescount shared nogeometry,");
		
		ret.append(  "ifc publishing time,");
		ret.append(  "ifcowl publishing time,");
		
		return  ret.toString();
	}
}
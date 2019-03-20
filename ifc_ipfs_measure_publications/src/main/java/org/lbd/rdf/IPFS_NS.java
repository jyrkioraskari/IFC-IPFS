

package org.lbd.rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class IPFS_NS {
	private static final String URI = "http://ipfs/";

	protected static final Property property(String tag) {
		return ResourceFactory.createProperty(URI, tag);
	}

	public static final Property merkle_node = property("merkle_node");
}

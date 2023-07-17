package org.lbd;

import java.util.Map;

import org.lbd.ifc.CurrentRootEntityTripleSet;

public interface IPFSPublisherInterface {
	public void createRootResource(String uri,String guid);
	public void publishEntityNode2IPFS(CurrentRootEntityTripleSet root_entity);
}

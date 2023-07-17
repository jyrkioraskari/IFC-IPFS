package org.lbd.statistics;
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

public class Statistics implements Comparable {
	final String model_name;
	long ifcowl_triples=0;
	long ifc_root_entities=0;
	long ipfs_merkle_nodes;
	
	long process_start_time;
	long process_stop_time;

	long ifc_convert;
	
	long rewritingtriples;
	long canonization;
	
	long publish_merkle_nodes;
	long publish_directory_node;
	
	long ipfs_node_count;

	
	public Statistics(String model_name) {
		super();
		this.model_name = model_name;
	}


	public long getIfcowl_triples() {
		return ifcowl_triples;
	}


	public void setIfcowl_triples(long ifcowl_triples) {
		this.ifcowl_triples = ifcowl_triples;
	}

	
	
	public long getIfc_root_entities() {
		return ifc_root_entities;
	}


	public void setIfc_root_entities(long ifc_root_entities) {
		this.ifc_root_entities = ifc_root_entities;
	}


	public long getIpfs_merkle_nodes() {
		return ipfs_merkle_nodes;
	}


	public void setIpfs_merkle_nodes(long ipfs_merkle_nodes) {
		this.ipfs_merkle_nodes = ipfs_merkle_nodes;
	}


	public long getProcess_start_time() {
		return process_start_time;
	}


	public void setProcess_start_time(long process_start_time) {
		this.process_start_time = process_start_time;
	}


	public long getProcess_stop_time() {
		return process_stop_time;
	}


	public void setProcess_stop_time(long process_stop_time) {
		this.process_stop_time = process_stop_time;
	}


	public String getModel_name() {
		return model_name;
	}

	
	public long getIfc_convert() {
		return ifc_convert;
	}


	public void setIfc_convert(long ifc_convert) {
		this.ifc_convert = ifc_convert;
	}

	public void addIfc_convert(long ifc_convert) {
		this.ifc_convert += ifc_convert;
	}

	
	public long getRewritingtriples() {
		return rewritingtriples;
	}


	public void setRewritingtriples(long rewritingtriples) {
		this.rewritingtriples = rewritingtriples;
	}


	public long getCanonization() {
		return canonization;
	}


	public void setCanonization(long canonization) {
		this.canonization = canonization;
	}


	public long getPublish_merkle_nodes() {
		return publish_merkle_nodes;
	}


	public void setPublish_merkle_nodes(long publish_merkle_nodes) {
		this.publish_merkle_nodes = publish_merkle_nodes;
	}

	public void addPublish_merkle_nodes(long publish_merkle_nodes) {
		this.publish_merkle_nodes += publish_merkle_nodes;
	}

	public long getPublish_directory_node() {
		return publish_directory_node;
	}


	public void setPublish_directory_node(long publish_directory_node) {
		this.publish_directory_node = publish_directory_node;
	}
	
	public long getIpfs_node_count() {
		return ipfs_node_count;
	}


	public void setIpfs_node_count(long ipfs_node_count) {
		this.ipfs_node_count = ipfs_node_count;
	}


	@Override
	public String toString() {
		StringBuilder ret=new StringBuilder();
		ret.append(this.model_name+",");
		ret.append(this.ifcowl_triples+",");
		ret.append(this.ifc_root_entities+",");
		ret.append(((this.process_stop_time-this.process_start_time)/1000000f) +",");
		
		ret.append(this.ifc_convert+",");
		ret.append(this.rewritingtriples+",");
		ret.append(this.canonization+",");
		ret.append(this.publish_merkle_nodes+",");
		ret.append(this.publish_directory_node+",");
		ret.append(this.ipfs_node_count+",");
		return ret.toString();
	}

	

	@Override
	public int compareTo(Object o) {
		Statistics so=(Statistics)o;
		long diff=(this.ifcowl_triples-so.ifcowl_triples);
		if(diff==0)
			return 0;
		if(diff>0)
			return 1;
		else
			return -1;
	}

}

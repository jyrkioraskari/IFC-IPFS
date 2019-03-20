package org.lbd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lbd.ifc.RootEntity;
import org.lbd.rdf.CanonizedPattern;

import com.hp.hpl.jena.rdf.model.Statement;

public class IFCtoLBDFiles_ElementIntersection {
	private final HashMap<String,List<RootEntity>> combined_statements = new HashMap<>();
	private final Set<String> doubled_statements = new HashSet<>();

	private final String version_name;
	private final String baseURI = "http://ipfs/bim/";
	private String output_directory;

	private final CanonizedPattern canonized_pattern = new CanonizedPattern();

	public IFCtoLBDFiles_ElementIntersection(String output_directory, String version_name) {

		this.version_name = version_name;
		this.output_directory = output_directory;
		if (!output_directory.endsWith("/"))
			this.output_directory += "/";
		System.out.println("output: " + output_directory);

	}

	public void add(String ifc_file) throws InterruptedException, IOException {
		Splitted_IfcOWL ifcrdf = new Splitted_IfcOWL(ifc_file);
		handleEntityNodes(ifcrdf.getEntitys(), ifcrdf.getURI2GUID_map());
		System.out.println("doubled: " + doubled_statements.size());
		System.out.println("total: " + combined_statements.size());

		System.out.println("doubled elements: " + doubled_elements);
		System.out.println("total elements: " + total_elements);

	}

	int count = 0;

	private void handleEntityNodes(List<RootEntity> root_entitys, Map<String, String> uri2guid) {
	

		for (RootEntity g : root_entitys) {
			isdoubled = false;
			System.out.println(".... "+g.getURI());
			for (Statement x : g.getTriples()) {
				if (x.getPredicate().toString().endsWith("#ownerHistory_IfcRoot"))
					return;
				List<RootEntity> entities = combined_statements.get(x.toString());
				
				if (entities!=null) {
					doubled_statements.add(x.toString());
					//System.out.println("d: " + entities+ " <> "+g.getGuid()+" "+x.toString());
					isdoubled = true;
				} 
				else
				{
					entities=new ArrayList<>();
					combined_statements.put(x.toString(), entities);
				}
				entities.add(g);
			}
			if (isdoubled) {				
				doubled_elements++;
				//System.out.println(g.getGuid() + " " + g.getURI());
			}
			/*count++;
			if (count > 50)
				return;*/
			total_elements++;
		}

	}

	int doubled_elements = 0;
	int total_elements = 0;
	boolean isdoubled = false;

	public static void main(String[] args) {
		try {
			if (args.length > 1) {
				IFCtoLBDFiles_ElementIntersection ifc_ipfs = new IFCtoLBDFiles_ElementIntersection("", "V1");
				ifc_ipfs.add("c:\\ifc\\171210CADstudio_brep.ifc"); 
			}
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

package org.lbd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.lbd.ifc.RootEntity;
import org.lbd.rdfpath.RDFStep;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class IFCtoLBDFiles_MostCommonDuplicates {
	private final HashMap<String, RDFClassCounter> class_countMap = new HashMap<>();
	private long total=0;

	private final HashMap<String, List<RootEntity>> combined_statements = new HashMap<>();

	public IFCtoLBDFiles_MostCommonDuplicates() {
	}

	public void add(String ifc_file) throws InterruptedException, IOException {
		Splitted_IfcOWL ifcrdf = new Splitted_IfcOWL(ifc_file);
		handleEntityNodes(ifcrdf.getEntitys(), ifcrdf.getURI2GUID_map());

	}

	public void summary() {

		List<RDFClassCounter> list = new ArrayList<>();
		class_countMap.values().stream().forEach(x -> list.add(x));
		Collections.sort(list);
		System.out.println("List size: " + list.size());
		for (int i = 0; i < 5; i++) {
			RDFClassCounter rc = list.get(i);
			System.out.println(i + 1 + ". " + rc.getURI() + " " + rc.getCount());
		}
		combined_statements.clear();
		System.out.println("total: "+total);
	}	

	private void handleEntityNodes(List<RootEntity> root_entitys, Map<String, String> uri2guid) {

		for (RootEntity g : root_entitys) {
			System.out.println(".... " + g.getURI());
			for (Statement x : g.getTriples()) {
				total++;
				if (x.getPredicate().toString().endsWith("#ownerHistory_IfcRoot"))
					return;
				List<RootEntity> entities = combined_statements.get(x.toString());

				if (entities != null) {
					String class_name = getType(x.getSubject()).get().getLocalName();
					RDFClassCounter rc = class_countMap.get(class_name);
					if (rc == null) {
						rc = new RDFClassCounter(class_name);
						class_countMap.put(class_name, rc);
					}
					rc.inc();
				} else {
					entities = new ArrayList<>();
					combined_statements.put(x.toString(), entities);
				}
				entities.add(g);
			}
		}
	}

	private Optional<Resource> getType(Resource r) {
		RDFStep[] path = { new RDFStep(RDF.type) };
		return pathQuery(r, path).stream().map(rn -> rn.asResource()).findAny();
	}

	private List<RDFNode> pathQuery(Resource r, RDFStep[] path) {
		List<RDFStep> path_list = Arrays.asList(path);
		if (r.getModel() == null)
			return new ArrayList<RDFNode>();
		Optional<RDFStep> step = path_list.stream().findFirst();
		if (step.isPresent()) {
			List<RDFNode> step_result = step.get().next(r);
			if (path.length > 1) {
				final List<RDFNode> result = new ArrayList<RDFNode>();
				step_result.stream().filter(rn1 -> rn1.isResource()).map(rn2 -> rn2.asResource()).forEach(r1 -> {
					List<RDFStep> tail = path_list.stream().skip(1).collect(Collectors.toList());
					result.addAll(pathQuery(r1, tail.toArray(new RDFStep[tail.size()])));
				});
				return result;
			} else
				return step_result;
		}
		return new ArrayList<RDFNode>();
	}

	public static void main(String[] args) {
		try {
			IFCtoLBDFiles_MostCommonDuplicates ifc_ipfs = new IFCtoLBDFiles_MostCommonDuplicates();
			ifc_ipfs.add("c:\\ifc\\301110FZK-Haus-EliteCAD.ifc");
			ifc_ipfs.add("c:\\ifc\\301110Nem-FZK-Haus-2x3.ifc");
			ifc_ipfs.add("c:\\ifc\\171210Bentley1_brep.ifc");
			ifc_ipfs.add("c:\\ifc\\261110Allplan-2008-Institute-Var-2-IFC.ifc");
			ifc_ipfs.add("c:\\ifc\\171210PlayersTheater_param.ifc");
			ifc_ipfs.add("c:\\ifc\\171210CADstudio_brep.ifc");
			ifc_ipfs.add("c:\\ifc\\20160125RST_2010_Trapelo.ifc");
			ifc_ipfs.summary();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}

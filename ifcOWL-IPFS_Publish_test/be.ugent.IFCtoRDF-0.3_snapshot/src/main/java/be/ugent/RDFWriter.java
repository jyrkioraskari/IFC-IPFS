/*
 * Copyright 2016 Pieter Pauwels, Ghent University; Jyrki Oraskari, Aalto University; Lewis John McGibbney, Apache
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License atf
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.ugent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.ifcrdf.EventBusService;
import org.ifcrdf.messages.SystemErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buildingsmart.tech.ifcowl.ExpressReader;
import com.buildingsmart.tech.ifcowl.vo.EntityVO;
import com.buildingsmart.tech.ifcowl.vo.IFCVO;
import com.buildingsmart.tech.ifcowl.vo.TypeVO;
import com.google.common.eventbus.EventBus;

import fi.ni.rdf.Namespace;


public class RDFWriter {
    private final EventBus eventBus = EventBusService.getEventBus();
    // input variables
    private final String baseURI;
    private final String ontNS;
    private static final String EXPRESS_URI = "https://w3id.org/express";
    private static final String EXPRESS_NS = EXPRESS_URI + "#";
    private static final String LIST_URI = "https://w3id.org/list";
    private static final String LIST_NS = LIST_URI + "#";

    // EXPRESS basis
    private final Map<String, EntityVO> ent;
    private final Map<String, TypeVO> typ;

    // conversion variables
    private int idCounter = 0;
    private Map<Long, IFCVO> linemap = new HashMap<>();

    private StreamRDF ttlWriter;
    private InputStream inputStream;
    private final OntModel ontModel;

    @SuppressWarnings("unused")
    private IfcSpfReader myIfcReaderStream;

    // for removing duplicates in line entries
    private Map<String, Resource> listOfUniqueResources = new HashMap<>();
    private Map<Long, Long> listOfDuplicateLineEntries = new HashMap<>();

    // Taking care of avoiding duplicate resources
    private Map<String, Resource> propertyResourceMap = new HashMap<>();
    private Map<String, Resource> resourceMap = new HashMap<>();

    private boolean removeDuplicates = false;

    private static final Logger LOG = LoggerFactory.getLogger(RDFWriter.class);

    public RDFWriter(OntModel ontModel, InputStream inputStream, String baseURI, Map<String, EntityVO> ent, Map<String, TypeVO> typ, String ontURI) {
        this.ontModel = ontModel;
        this.inputStream = inputStream;
        this.baseURI = baseURI;
        this.ent = ent;
        this.typ = typ;
        this.ontNS = ontURI + "#";
    }

    public void setIfcReader(IfcSpfReader r) {
        this.myIfcReaderStream = r;
    }

    public void parseModel2Stream(OutputStream out) throws IOException {
        ttlWriter = StreamRDFWriter.getWriterStream(out, RDFFormat.TURTLE_BLOCKS);
        ttlWriter.base(baseURI);
        ttlWriter.prefix("ifcowl", ontNS);
        ttlWriter.prefix("inst", baseURI);
        ttlWriter.prefix("list", LIST_NS);
        ttlWriter.prefix("express", EXPRESS_NS);
        ttlWriter.prefix("rdf", Namespace.RDF);
        ttlWriter.prefix("xsd", Namespace.XSD);
        ttlWriter.prefix("owl", Namespace.OWL);
        ttlWriter.start();

        ttlWriter.triple(new Triple(NodeFactory.createURI(baseURI), RDF.type.asNode(), OWL.Ontology.asNode()));
        ttlWriter.triple(new Triple(NodeFactory.createURI(baseURI), OWL.imports.asNode(), NodeFactory.createURI(ontNS)));

        // Read the whole file into a linemap Map object
        readModel();

        LOG.info("Model parsed");

        if (removeDuplicates) {
            resolveDuplicates();
        }

        // map entries of the linemap Map object to the ontology Model and make
        // new instances in the model
        boolean parsedSuccessfully = mapEntries();

        if (!parsedSuccessfully)
            return;

        LOG.info("Entries mapped, now creating instances");
        createInstances();

        // Save memory
        linemap.clear();
        linemap = null;

        ttlWriter.finish();
    }

    private void readModel() {
        try {
            DataInputStream in = new DataInputStream(inputStream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            try {
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    if (strLine.length() > 0) {
                        if (strLine.charAt(0) == '#') {
                            StringBuilder sb = new StringBuilder();
                            String stmp = strLine;
                            sb.append(stmp.trim());
                            while (!stmp.contains(";")) {
                                stmp = br.readLine();
                                if (stmp == null)
                                    break;
                                sb.append(stmp.trim());
                            }
                            // the whole IFC gets parsed, and everything ends up
                            // as IFCVO objects in the Map<Long, IFCVO> linemap
                            // variable
                            parseIfcLineStatement(sb.toString().substring(1));
                        }
                    }
                }
            } finally {
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            eventBus.post(new SystemErrorEvent(e.getClass().getName()+" - "+e.getMessage()));
        }
    }

    private void parseIfcLineStatement(String line) {
        IFCVO ifcvo = new IFCVO();
        ifcvo.setFullLineAfterNum(line.substring(line.indexOf('=') + 1));
        int state = 0;
        StringBuilder sb = new StringBuilder();
        int clCount = 0;
        LinkedList<Object> current = (LinkedList<Object>) ifcvo.getObjectList();
        Stack<LinkedList<Object>> listStack = new Stack<>();
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            switch (state) {
                case 0:
                if (ch == '=') {
                    ifcvo.setLineNum(toLong(sb.toString()));
                    sb.setLength(0);
                    state++;
                    continue;
                } else if (Character.isDigit(ch))
                    sb.append(ch);
                    break;
                case 1: // (
                if (ch == '(') {
                    ifcvo.setName(sb.toString());
                    sb.setLength(0);
                    state++;
                    continue;
                } else if (ch == ';') {
                    ifcvo.setName(sb.toString());
                    sb.setLength(0);
                    state = Integer.MAX_VALUE;
                } else if (!Character.isWhitespace(ch))
                    sb.append(ch);
                    break;
                case 2: // (... line started and doing (...
                if (ch == '\'') {
                    state++;
                }
                if (ch == '(') {
                    listStack.push(current);
                    LinkedList<Object> tmp = new LinkedList<>();
                    if (sb.toString().trim().length() > 0)
                        current.add(sb.toString().trim());
                    sb.setLength(0);
                    current.add(tmp);
                    current = tmp;
                    clCount++;
                } else if (ch == ')') {
                    if (clCount == 0) {
                        if (sb.toString().trim().length() > 0)
                            current.add(sb.toString().trim());
                        sb.setLength(0);
                        state = Integer.MAX_VALUE; // line is done
                        continue;
                    } else {
                        if (sb.toString().trim().length() > 0)
                            current.add(sb.toString().trim());
                        sb.setLength(0);
                        clCount--;
                        current = listStack.pop();
                    }
                } else if (ch == ',') {
                    if (sb.toString().trim().length() > 0)
                        current.add(sb.toString().trim());
                    current.add(Character.valueOf(ch));

                    sb.setLength(0);
                } else {
                    sb.append(ch);
                }
                    break;
                case 3: // (...
                if (ch == '\'') {
                    state--;
                } else {
                    sb.append(ch);
                }
                    break;
                default:
                // Do nothing
            }
        }
        linemap.put(ifcvo.getLineNum(), ifcvo);
        idCounter++;
    }

    private void resolveDuplicates() throws IOException {
        Map<String, IFCVO> listOfUniqueResources = new HashMap<>();
        List<Long> entriesToRemove = new ArrayList<>();
        for (Map.Entry<Long, IFCVO> entry : linemap.entrySet()) {
            IFCVO vo = entry.getValue();
            String t = vo.getFullLineAfterNum();
            if (!listOfUniqueResources.containsKey(t))
                listOfUniqueResources.put(t, vo);
            else {
                // found duplicate
                entriesToRemove.add(entry.getKey());
                listOfDuplicateLineEntries.put(vo.getLineNum(), listOfUniqueResources.get(t).getLineNum());
            }
        }
        LOG.info("MESSAGE: found and removed " + listOfDuplicateLineEntries.size() + " duplicates!");
        for (Long x : entriesToRemove) {
            linemap.remove(x);
        }
    }

    private boolean mapEntries() throws IOException {
        for (Map.Entry<Long, IFCVO> entry : linemap.entrySet()) {
            IFCVO vo = entry.getValue();

            // mapping properties to IFCVOs
            for (int i = 0; i < vo.getObjectList().size(); i++) {
                Object o = vo.getObjectList().get(i);
                if (Character.class.isInstance(o)) {
                    if ((Character) o != ',') {
                        LOG.error("*ERROR 15*: We found a character that is not a comma. That should not be possible");
                        eventBus.post(new SystemErrorEvent("*ERROR 15*: A character that is not a comma. "));
                    }
                } else if (String.class.isInstance(o)) {
                    String s = (String) o;
                    if (s.length() < 1)
                        continue;
                    if (s.charAt(0) == '#') {
                        Object or = null;
                        if (listOfDuplicateLineEntries.containsKey(toLong(s.substring(1))))
                            or = linemap.get(listOfDuplicateLineEntries.get(toLong(s.substring(1))));
                        else
                            or = linemap.get(toLong(s.substring(1)));

                        if (or == null) {
                            LOG.error("*ERROR 6*: Reference to non-existing line number in line: #" + vo.getLineNum() + "=" + vo.getFullLineAfterNum());
                            eventBus.post(new SystemErrorEvent("*ERROR 6*: Reference to non-existing line number in line: #" + vo.getLineNum() + "=" + vo.getFullLineAfterNum()));
                            return false;
                        }
                        vo.getObjectList().set(i, or);
                    }
                } else if (LinkedList.class.isInstance(o)) {
                    @SuppressWarnings("unchecked")
                    LinkedList<Object> tmpList = (LinkedList<Object>) o;

                    for (int j = 0; j < tmpList.size(); j++) {
                        Object o1 = tmpList.get(j);
                        if (Character.class.isInstance(o)) {
                            if ((Character) o != ',') {
                                LOG.error("*ERROR 16*: We found a character that is not a comma. " + "That should not be possible!");
                                eventBus.post(new SystemErrorEvent("*ERROR 16*: A character that is not a comma. "));
                            }
                        } else if (String.class.isInstance(o1)) {
                            String s = (String) o1;
                            if (s.length() < 1)
                                continue;
                            if (s.charAt(0) == '#') {
                                Object or = null;
                                if (listOfDuplicateLineEntries.containsKey(toLong(s.substring(1))))
                                    or = linemap.get(listOfDuplicateLineEntries.get(toLong(s.substring(1))));
                                else
                                    or = linemap.get(toLong(s.substring(1)));
                                if (or == null) {
                                    LOG.error("*ERROR 7*: Reference to non-existing line number in line: #" + vo.getLineNum() + " - " + vo.getFullLineAfterNum());
                                    eventBus.post(new SystemErrorEvent("*ERROR 7*: Reference to non-existing line number in line: #" + vo.getLineNum() + " - " + vo.getFullLineAfterNum()));
                                    tmpList.set(j, "-");
                                    return false;
                                } else
                                    tmpList.set(j, or);
                            } else {
                                // list/set of values
                                tmpList.set(j, s);
                            }
                        } else if (LinkedList.class.isInstance(o1)) {
                            @SuppressWarnings("unchecked")
                            LinkedList<Object> tmp2List = (LinkedList<Object>) o1;
                            for (int j2 = 0; j2 < tmp2List.size(); j2++) {
                                Object o2 = tmp2List.get(j2);
                                if (String.class.isInstance(o2)) {
                                    String s = (String) o2;
                                    if (s.length() < 1)
                                        continue;
                                    if (s.charAt(0) == '#') {
                                        Object or = null;
                                        if (listOfDuplicateLineEntries.containsKey(toLong(s.substring(1))))
                                            or = linemap.get(listOfDuplicateLineEntries.get(toLong(s.substring(1))));
                                        else
                                            or = linemap.get(toLong(s.substring(1)));
                                        if (or == null) {
                                            LOG.error("*ERROR 8*: Reference to non-existing line number in line: #" + vo.getLineNum() + " - " + vo.getFullLineAfterNum());
                                            eventBus.post(new SystemErrorEvent("*ERROR 8*: Reference to non-existing line number in line: #" + vo.getLineNum() + " - " + vo.getFullLineAfterNum()));
                                            tmp2List.set(j2, "-");
                                            return false;
                                        } else
                                            tmp2List.set(j2, or);
                                    }
                                }
                            }
                            tmpList.set(j, tmp2List);
                        }
                    }
                }
            }
        }
        return true;
    }

    private void createInstances() throws IOException {
        for (Map.Entry<Long, IFCVO> entry : linemap.entrySet()) {
            IFCVO ifcLineEntry = entry.getValue();
            String typeName = "";
            if (ent.containsKey(ifcLineEntry.getName()))
                typeName = ent.get(ifcLineEntry.getName()).getName();
            else if (typ.containsKey(ifcLineEntry.getName()))
                typeName = typ.get(ifcLineEntry.getName()).getName();

            OntClass cl = ontModel.getOntClass(ontNS + typeName);
            if(cl==null)
                eventBus.post(new SystemErrorEvent(" OntClass is null: "+ontNS + typeName));
            Resource r = getResource(baseURI + typeName + "_" + ifcLineEntry.getLineNum(), cl);
            if (r == null) {
                // *ERROR 2 already hit: we can safely stop
                return;
            }
            listOfUniqueResources.put(ifcLineEntry.getFullLineAfterNum(), r);

            LOG.info("-------------------------------");
            LOG.info(r.getLocalName());
            LOG.info("-------------------------------");

            fillProperties(ifcLineEntry, r);
        }
        // The map is used only to avoid duplicates.
        // So, it can be cleared here
        propertyResourceMap.clear();
    }

    TypeVO typeRemembrance = null;

    private void fillProperties(IFCVO ifcLineEntry, Resource r) throws IOException {

        EntityVO evo = ent.get(ExpressReader.formatClassName(ifcLineEntry.getName()));
        TypeVO tvo = typ.get(ExpressReader.formatClassName(ifcLineEntry.getName()));

        if (tvo == null && evo == null) {
            // This can actually never happen
            // Namely, if this is the case, then ERROR 2 should fire first,
            // after which the program stops
            LOG.error("ERROR 3*: fillProperties 1 - Type nor entity exists: {}", ifcLineEntry.getName());
            eventBus.post(new SystemErrorEvent("ERROR 3*: fillProperties 1 - Type nor entity exists: "+ ifcLineEntry.getName()));
        }

        if (evo == null && tvo != null) {
            // working with a TYPE

            typeRemembrance = null;
            for (Object o : ifcLineEntry.getObjectList()) {

                if (Character.class.isInstance(o)) {
                    if ((Character) o != ',') {
                        LOG.error("*ERROR 17*: We found a character that is not a comma. That should not be possible!");
                        eventBus.post(new SystemErrorEvent("*ERROR 17*: A character that is not a comma. "));
                    }
                } else if (String.class.isInstance(o)) {
                    LOG.warn("*WARNING 1*: fillProperties 2: unhandled type property found.");
                } else if (IFCVO.class.isInstance(o)) {
                    LOG.warn("*WARNING 2*: fillProperties 2: unhandled type property found.");
                } else if (LinkedList.class.isInstance(o)) {
                    LOG.info("fillProperties 3 - fillPropertiesHandleListObject(tvo)");
                    fillPropertiesHandleListObject(r, tvo, o);
                }
            }
        }

        if (tvo == null && evo != null) {
            // working with an ENTITY
            final String subject = evo.getName() + "_" + ifcLineEntry.getLineNum();

            typeRemembrance = null;
            int attributePointer = 0;
            for (Object o : ifcLineEntry.getObjectList()) {

                if (Character.class.isInstance(o)) {
                    if ((Character) o != ',') {
                        LOG.error("*ERROR 18*: We found a character that is not a comma. That should not be possible!");
                        eventBus.post(new SystemErrorEvent("*ERROR 18*: A character that is not a comma."));
                    }
                } else if (String.class.isInstance(o)) {
                    LOG.info("fillProperties 4 - fillPropertiesHandleStringObject(evo)");
                    attributePointer = fillPropertiesHandleStringObject(r, evo, subject, attributePointer, o);
                } else if (IFCVO.class.isInstance(o)) {
                    LOG.info("fillProperties 5 - fillPropertiesHandleIfcObject(evo)");
                    attributePointer = fillPropertiesHandleIfcObject(r, evo, attributePointer, o);
                } else if (LinkedList.class.isInstance(o)) {
                    LOG.info("fillProperties 6 - fillPropertiesHandleListObject(evo)");
                    attributePointer = fillPropertiesHandleListObject(r, evo, attributePointer, o);
                }
            }
        }
    }

    // --------------------------------------
    // 6 MAIN FILLPROPERTIES METHODS
    // --------------------------------------

    private int fillPropertiesHandleStringObject(Resource r, EntityVO evo, String subject, int attributePointer, Object o) throws IOException {
        if (!((String) o).equals("$") && !((String) o).equals("*")) {

            if (typ.get(ExpressReader.formatClassName((String) o)) == null) {
                if ((evo != null) && (evo.getDerivedAttributeList() != null)) {
                    if (evo.getDerivedAttributeList().size() <= attributePointer) {
                        LOG.error("*ERROR 4*: Entity in IFC files has more attributes than it is allowed have: " + subject);
                        eventBus.post(new SystemErrorEvent("*ERROR 4*: Entity in IFC files has more attributes than it is allowed have: " + subject));
                        attributePointer++;
                        return attributePointer;
                    }

                    final String propURI = ontNS + evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
                    final String literalString = filterExtras((String) o);

                    OntProperty p = ontModel.getOntProperty(propURI);
                    OntResource range = p.getRange();
                    if (range.isClass()) {
                        if (range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "ENUMERATION"))) {
                            // Check for ENUM
                            addEnumProperty(r, p, range, literalString);
                        } else if (range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "SELECT"))) {
                            // Check for SELECT
                            LOG.info("*OK 25*: found subClass of SELECT Class, now doing nothing with it: " + p + " - " + range.getLocalName() + " - " + literalString);
                            createLiteralProperty(r, p, range, literalString);
                        } else if (range.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList"))) {
                            // Check for LIST
                            LOG.info("*WARNING 5*: found LIST property (but doing nothing with it): " + subject + " -- " + p + " - " + range.getLocalName() + " - " + literalString);
                        } else {
                            createLiteralProperty(r, p, range, literalString);
                        }
                    } else {
                        LOG.warn("*WARNING 7*: found other kind of property: " + p + " - " + range.getLocalName());
                    }
                } else {
                    LOG.warn("*WARNING 8*: Nothing happened. Not sure if this is good or bad, possible or not.");
                }
                attributePointer++;
            } else {
                typeRemembrance = typ.get(ExpressReader.formatClassName((String) o));
            }
        } else
            attributePointer++;
        return attributePointer;
    }

    private int fillPropertiesHandleIfcObject(Resource r, EntityVO evo, int attributePointer, Object o) throws IOException {
        if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {

            final String propURI = ontNS + evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
            EntityVO evorange = ent.get(ExpressReader.formatClassName(((IFCVO) o).getName()));

            OntProperty p = ontModel.getOntProperty(propURI);
            OntResource rclass = ontModel.getOntResource(ontNS + evorange.getName());
            if(rclass==null)
                eventBus.post(new SystemErrorEvent("rw540 rclass null"+ontNS + evorange.getName()));

            Resource r1 = getResource(baseURI + evorange.getName() + "_" + ((IFCVO) o).getLineNum(), rclass);
            ttlWriter.triple(new Triple(r.asNode(), p.asNode(), r1.asNode()));
            LOG.info("*OK 1*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r1.getLocalName());
        } else {
            LOG.warn("*WARNING 3*: Nothing happened. Not sure if this is good or bad, possible or not.");
        }
        attributePointer++;
        return attributePointer;
    }

    @SuppressWarnings("unchecked")
    private int fillPropertiesHandleListObject(Resource r, EntityVO evo, int attributePointer, Object o) throws IOException {

        final LinkedList<Object> tmpList = (LinkedList<Object>) o;
        LinkedList<String> literals = new LinkedList<>();
        LinkedList<Resource> listRemembranceResources = new LinkedList<>();
        LinkedList<IFCVO> ifcVOs = new LinkedList<>();

        // process list
        for (int j = 0; j < tmpList.size(); j++) {
            Object o1 = tmpList.get(j);
            if (Character.class.isInstance(o1)) {
                Character c = (Character) o1;
                if (c != ',') {
                    LOG.error("*ERROR 12*: We found a character that is not a comma. That is odd. Check!");
                    eventBus.post(new SystemErrorEvent("*ERROR 12*: A character that is not a comma."));
                }
            } else if (String.class.isInstance(o1)) {
                TypeVO t = typ.get(ExpressReader.formatClassName((String) o1));
                if (typeRemembrance == null) {
                    if (t != null) {
                        typeRemembrance = t;
                    } else {
                        literals.add(filterExtras((String) o1));
                    }
                } else {
                    if (t != null) {
                        if (t == typeRemembrance) {
                            // Ignore and continue with life
                        } else {
                            // Panic
                            LOG.warn("*WARNING 37*: Found two different types in one list. This is worth checking.");
                        }
                    } else {
                        literals.add(filterExtras((String) o1));
                    }
                }
            } else if (IFCVO.class.isInstance(o1)) {
                if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {

                    String propURI = evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
                    OntProperty p = ontModel.getOntProperty(ontNS + propURI);
                    OntResource typerange = p.getRange();

                    if (typerange.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList"))) {
                        // EXPRESS LISTs
                        String listvaluepropURI = ontNS + typerange.getLocalName().substring(0, typerange.getLocalName().length() - 5);
                        OntResource listrange = ontModel.getOntResource(listvaluepropURI);

                        if (listrange.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList"))) {
                            LOG.error("*ERROR 22*: Found supposedly unhandled ListOfList, but this should not be possible.");
                            eventBus.post(new SystemErrorEvent("*ERROR 22*: Found supposedly unhandled ListOfList."));
                        } else {
                            fillClassInstanceList(tmpList, typerange, p, r);
                            j = tmpList.size() - 1;
                        }
                    } else {
                        // EXPRESS SETs
                        EntityVO evorange = ent.get(ExpressReader.formatClassName(((IFCVO) o1).getName()));
                        OntResource rclass = ontModel.getOntResource(ontNS + evorange.getName());
                        if(rclass==null)
                            eventBus.post(new SystemErrorEvent("rw613 rclass null: "+ontNS + evorange.getName()));

                        Resource r1 = getResource(baseURI + evorange.getName() + "_" + ((IFCVO) o1).getLineNum(), rclass);
                        ttlWriter.triple(new Triple(r.asNode(), p.asNode(), r1.asNode()));
                        LOG.info("*OK 5*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r1.getLocalName());
                    }
                } else {
                    LOG.warn("*WARNING 13*: Nothing happened. Not sure if this is good or bad, possible or not.");
                }
            } else if (LinkedList.class.isInstance(o1)) {
                if (typeRemembrance != null) {
                    LinkedList<Object> tmpListInList = (LinkedList<Object>) o1;
                    for (int jj = 0; jj < tmpListInList.size(); jj++) {
                        Object o2 = tmpListInList.get(jj);
                        if (Character.class.isInstance(o2)) {
                            if ((Character) o2 != ',') {
                                LOG.error("*ERROR 20*: We found a character that is not a comma. That should not be possible");
                                eventBus.post(new SystemErrorEvent("*ERROR 20*: A character that is not a comma."));
                            }
                        } else if (String.class.isInstance(o2)) {
                            literals.add(filterExtras((String) o2));
                        } else if (IFCVO.class.isInstance(o2)) {
                            // Lists of IFC entities
                            LOG.warn("*WARNING 30: Nothing happened. Not sure if this is good or bad, possible or not.");
                        } else if (LinkedList.class.isInstance(o2)) {
                            // this happens only for types that are equivalent
                            // to lists (e.g. IfcLineIndex in IFC4_ADD1)
                            // in this case, the elements of the list should be
                            // treated as new instances that are equivalent to
                            // the correct lists
                            LinkedList<Object> tmpListInListInList = (LinkedList<Object>) o2;
                            for (int jjj = 0; jjj < tmpListInListInList.size(); jjj++) {
                                Object o3 = tmpListInListInList.get(jjj);
                                if (Character.class.isInstance(o3)) {
                                    if ((Character) o3 != ',') {
                                        LOG.error("*ERROR 24*: We found a character that is not a comma. That should not be possible");
                                        eventBus.post(new SystemErrorEvent("*ERROR 24*: A character that is not a comma."));

                                    }
                                } else if (String.class.isInstance(o3)) {
                                    literals.add(filterExtras((String) o3));
                                } else {
                                    LOG.warn("*WARNING 31: Nothing happened. Not sure if this is good or bad, possible or not.");
                                }
                            }

                            // exception. when a list points to a number of
                            // linked lists, it could be that there are multiple
                            // different entities are referenced
                            // example: #308=
                            // IFCINDEXEDPOLYCURVE(#309,(IFCLINEINDEX((1,2)),IFCARCINDEX((2,3,4)),IFCLINEINDEX((4,5)),IFCARCINDEX((5,6,7))),.F.);
                            // in this case, it is better to immediately print
                            // all relevant entities and properties for each
                            // case (e.g. IFCLINEINDEX((1,2))),
                            // and reset typeremembrance for the next case (e.g.
                            // IFCARCINDEX((4,5))).

                            if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {

                                OntClass cl = ontModel.getOntClass(ontNS + typeRemembrance.getName());
                                Resource r1 = getResource(baseURI + typeRemembrance.getName() + "_" + idCounter, cl);
                                idCounter++;
                                OntResource range = ontModel.getOntResource(ontNS + typeRemembrance.getName());

                                // finding listrange
                                String[] primTypeArr = typeRemembrance.getPrimarytype().split(" ");
                                String primType = ontNS + primTypeArr[primTypeArr.length - 1].replace(";", "");
                                OntResource listrange = ontModel.getOntResource(primType);

                                List<Object> literalObjects = new ArrayList<>();
                                literalObjects.addAll(literals);
                                addDirectRegularListProperty(r1, range, listrange, literalObjects, 0);

                                // put relevant top list items in a list, which
                                // can then be parsed at the end of this method
                                listRemembranceResources.add(r1);
                            }

                            typeRemembrance = null;
                            literals.clear();
                        } else {
                            LOG.warn("*WARNING 35: Nothing happened. Not sure if this is good or bad, possible or not.");
                        }
                    }
                } else {
                    LinkedList<Object> tmpListInList = (LinkedList<Object>) o1;
                    for (int jj = 0; jj < tmpListInList.size(); jj++) {
                        Object o2 = tmpListInList.get(jj);
                        if (Character.class.isInstance(o2)) {
                            if ((Character) o2 != ',') {
                                LOG.error("*ERROR 21*: We found a character that is not a comma. That should not be possible");
                                eventBus.post(new SystemErrorEvent("*ERROR 21*: A character that is not a comma."));

                            }
                        } else if (String.class.isInstance(o2)) {
                            literals.add(filterExtras((String) o2));
                        } else if (IFCVO.class.isInstance(o2)) {
                            ifcVOs.add((IFCVO) o2);
                        } else if (LinkedList.class.isInstance(o2)) {
                            LOG.error("*ERROR 19*: Found List of List of List. Code cannot handle that.");
                            eventBus.post(new SystemErrorEvent("*ERROR 19*: Found List of List of List. Code cannot handle that."));
                        } else {
                            LOG.warn("*WARNING 32*: Nothing happened. Not sure if this is good or bad, possible or not.");
                        }
                    }
                    if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {

                        String propURI = ontNS + evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
                        OntProperty p = ontModel.getOntProperty(propURI);
                        OntClass typerange = p.getRange().asClass();

                        if (typerange.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList"))) {
                            String listvaluepropURI = typerange.getLocalName().substring(0, typerange.getLocalName().length() - 5);
                            OntResource listrange = ontModel.getOntResource(ontNS + listvaluepropURI);
                            Resource r1 = getResource(baseURI + listvaluepropURI + "_" + idCounter, listrange);
                            idCounter++;
                            List<Object> objects = new ArrayList<>();
                            if (!ifcVOs.isEmpty()) {
                                objects.addAll(ifcVOs);
                                OntResource listcontentrange = getListContentType(listrange.asClass());
                                addDirectRegularListProperty(r1, listrange, listcontentrange, objects, 1);
                            } else if (!literals.isEmpty()) {
                                objects.addAll(literals);
                                OntResource listcontentrange = getListContentType(listrange.asClass());
                                addDirectRegularListProperty(r1, listrange, listcontentrange, objects, 0);
                            }
                            listRemembranceResources.add(r1);
                        } else {
                            LOG.error("*ERROR 23*: Impossible: found a list that is actually not a list.");
                            eventBus.post(new SystemErrorEvent("*ERROR 23*: Found a list that is actually not a list."));
                        }
                    }

                    literals.clear();
                    ifcVOs.clear();
                }
            } else {
                LOG.error("*ERROR 11*: We found something that is not an IFC entity, not a list, not a string, and not a character. Check!");
                eventBus.post(new SystemErrorEvent("*ERROR 11*: We found something that is not an IFC entity, not a list, not a string, and not a character. "));
            }
        }

        // interpret parse
        if (!literals.isEmpty()) {
            String propURI = ontNS + evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
            OntProperty p = ontModel.getOntProperty(propURI);
            OntResource typerange = p.getRange();
            if (typeRemembrance != null) {
                if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {
                    if (typerange.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList")))
                        addRegularListProperty(r, p, literals, typeRemembrance);
                    else {
                        addSinglePropertyFromTypeRemembrance(r, p, literals.getFirst(), typeRemembrance);
                        if (literals.size() > 1) {
                            LOG.warn("*WARNING 37*: We are ignoring a number of literal values here.");
                        }
                    }
                } else {
                    LOG.warn("*WARNING 15*: Nothing happened. Not sure if this is good or bad, possible or not.");
                }
                typeRemembrance = null;
            } else if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {
                if (typerange.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList")))
                    addRegularListProperty(r, p, literals, null);
                else
                    for (int i = 0; i < literals.size(); i++)
                        createLiteralProperty(r, p, typerange, literals.get(i));
            } else {
                LOG.warn("*WARNING 14*: Nothing happened. Not sure if this is good or bad, possible or not.");
            }
        }
        if (!listRemembranceResources.isEmpty()) {
            if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {
                String propURI = ontNS + evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
                OntProperty p = ontModel.getOntProperty(propURI);
                addListPropertyToGivenEntities(r, p, listRemembranceResources);
            }
        }

        attributePointer++;
        return attributePointer;
    }

    @SuppressWarnings({ "unchecked" })
    private void fillPropertiesHandleListObject(Resource r, TypeVO tvo, Object o) throws IOException {

        final LinkedList<Object> tmpList = (LinkedList<Object>) o;
        LinkedList<String> literals = new LinkedList<>();

        // process list
        for (int j = 0; j < tmpList.size(); j++) {
            Object o1 = tmpList.get(j);
            if (Character.class.isInstance(o1)) {
                Character c = (Character) o1;
                if (c != ',') {
                    LOG.error("*ERROR 13*: We found a character that is not a comma. That is odd. Check!");
                    eventBus.post(new SystemErrorEvent("*ERROR 13*: A character that is not a comma."));
                }
            } else if (String.class.isInstance(o1)) {
                if (typ.get(ExpressReader.formatClassName((String) o1)) != null && typeRemembrance == null) {
                    typeRemembrance = typ.get(ExpressReader.formatClassName((String) o1));
                } else
                    literals.add(filterExtras((String) o1));
            } else if (IFCVO.class.isInstance(o1)) {
                if ((tvo != null)) {
                    LOG.warn("*WARNING 16*: found TYPE that is equivalent to a list if IFC entities - below is the code used when this happens for ENTITIES with a list of ENTITIES");
                } else {
                    LOG.warn("*WARNING 19*: Nothing happened. Not sure if this is good or bad, possible or not.");
                }
            } else if (LinkedList.class.isInstance(o1) && typeRemembrance != null) {
                LinkedList<Object> tmpListInlist = (LinkedList<Object>) o1;
                for (int jj = 0; jj < tmpListInlist.size(); jj++) {
                    Object o2 = tmpListInlist.get(jj);
                    if (String.class.isInstance(o2)) {
                        literals.add(filterExtras((String) o2));
                    } else {
                        LOG.warn("*WARNING 18*: Nothing happened. Not sure if this is good or bad, possible or not.");
                    }
                }
            } else {
                LOG.error("*ERROR 10*: We found something that is not an IFC entity, not a list, not a string, and not a character. Check!");
                eventBus.post(new SystemErrorEvent("*ERROR 10*: We found something that is not an IFC entity, not a list, not a string, and not a character."));
            }
        }

        // interpret parse
        if (literals.isEmpty()) {
            if (typeRemembrance != null) {
                if ((tvo != null)) {
                    LOG.warn("*WARNING 20*: this part of the code has not been checked - it can't be correct");

                    String[] primtypeArr = tvo.getPrimarytype().split(" ");
                    String primType = primtypeArr[primtypeArr.length - 1].replace(";", "") + "_" + primtypeArr[0].substring(0, 1).toUpperCase() + primtypeArr[0].substring(1).toLowerCase();
                    String typeURI = ontNS + primType;
                    OntResource range = ontModel.getOntResource(typeURI);
                    OntResource listrange = getListContentType(range.asClass());
                    List<Object> literalObjects = new ArrayList<>();
                    literalObjects.addAll(literals);
                    addDirectRegularListProperty(r, range, listrange, literalObjects, 0);
                } else {
                    LOG.warn("*WARNING 21*: Nothing happened. Not sure if this is good or bad, possible or not.");
                }
                typeRemembrance = null;
            } else if ((tvo != null)) {
                String[] primTypeArr = tvo.getPrimarytype().split(" ");
                String primType = primTypeArr[primTypeArr.length - 1].replace(";", "") + "_" + primTypeArr[0].substring(0, 1).toUpperCase() + primTypeArr[0].substring(1).toLowerCase();
                String typeURI = ontNS + primType;
                OntResource range = ontModel.getOntResource(typeURI);
                List<Object> literalObjects = new ArrayList<>();
                literalObjects.addAll(literals);
                OntResource listrange = getListContentType(range.asClass());
                addDirectRegularListProperty(r, range, listrange, literalObjects, 0);
            }
        }
    }

    // --------------------------------------
    // EVERYTHING TO DO WITH LISTS
    // --------------------------------------

    private void addSinglePropertyFromTypeRemembrance(Resource r, OntProperty p, String literalString, TypeVO typeremembrance) throws IOException {
        OntResource range = ontModel.getOntResource(ontNS + typeremembrance.getName());

        if (range.isClass()) {
            if (range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "ENUMERATION"))) {
                // Check for ENUM
                addEnumProperty(r, p, range, literalString);
            } else if (range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "SELECT"))) {
                // Check for SELECT
                LOG.info("*OK 24*: found subClass of SELECT Class, now doing nothing with it: " + p + " - " + range.getLocalName() + " - " + literalString);
                createLiteralProperty(r, p, range, literalString);
            } else if (range.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList"))) {
                // Check for LIST
                LOG.warn("*WARNING 24*: found LIST property (but doing nothing with it): " + p + " - " + range.getLocalName() + " - " + literalString);
            } else {
                createLiteralProperty(r, p, range, literalString);
            }
        } else {
            LOG.warn("*WARNING 26*: found other kind of property: " + p + " - " + range.getLocalName());
        }
    }

    private void addEnumProperty(Resource r, Property p, OntResource range, String literalString) throws IOException {
        for (ExtendedIterator<? extends OntResource> instances = range.asClass().listInstances(); instances.hasNext();) {
            OntResource rangeInstance = instances.next();
            if (rangeInstance.getProperty(RDFS.label).getString().equalsIgnoreCase(filterPoints(literalString))) {
                ttlWriter.triple(new Triple(r.asNode(), p.asNode(), rangeInstance.asNode()));
                LOG.info("*OK 2*: added ENUM statement " + r.getLocalName() + " - " + p.getLocalName() + " - " + rangeInstance.getLocalName());
                return;
            }
        }
        LOG.error("*ERROR 9*: did not find ENUM individual for " + literalString + "\r\nQuitting the application without output!");
        eventBus.post(new SystemErrorEvent("*ERROR 9*: did not find ENUM individual for " + literalString + "\r\nQuitting the application without output!"));

    }

    private void addLiteralToResource(Resource r1, OntProperty valueProp, String xsdType, String literalString) throws IOException {
        if ("integer".equalsIgnoreCase(xsdType))
            addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral(literalString, XSDDatatype.XSDinteger));
        else if ("double".equalsIgnoreCase(xsdType))
            addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral(literalString, XSDDatatype.XSDdouble));
        else if ("hexBinary".equalsIgnoreCase(xsdType))
            addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral(literalString, XSDDatatype.XSDhexBinary));
        else if ("boolean".equalsIgnoreCase(xsdType)) {
            if (".F.".equalsIgnoreCase(literalString))
                addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral("false", XSDDatatype.XSDboolean));
            else if (".T.".equalsIgnoreCase(literalString))
                addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral("true", XSDDatatype.XSDboolean));
            else
                LOG.warn("*WARNING 10*: found odd boolean value: " + literalString);
        } else if ("logical".equalsIgnoreCase(xsdType)) {
            if (".F.".equalsIgnoreCase(literalString))
                addProperty(r1, valueProp, ontModel.getResource(EXPRESS_NS + "FALSE"));
            else if (".T.".equalsIgnoreCase(literalString))
                addProperty(r1, valueProp, ontModel.getResource(EXPRESS_NS + "TRUE"));
            else if (".U.".equalsIgnoreCase(literalString))
                addProperty(r1, valueProp, ontModel.getResource(EXPRESS_NS + "UNKNOWN"));
            else
                LOG.warn("*WARNING 9*: found odd logical value: " + literalString);
        } else if ("string".equalsIgnoreCase(xsdType))
            addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral(literalString, XSDDatatype.XSDstring));
        else
            addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral(literalString));

        LOG.info("*OK 4*: added literal: " + r1.getLocalName() + " - " + valueProp + " - " + literalString);
    }

    // LIST HANDLING
    private void addDirectRegularListProperty(Resource r, OntResource range, OntResource listrange, List<Object> el, int mySwitch) throws IOException {

        if (range.isClass()) {
            if (listrange.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList"))) {
                LOG.warn("*WARNING 27*: Found unhandled ListOfList");
            } else {
                List<Resource> reslist = new ArrayList<>();
                // createrequirednumberofresources
                for (int i = 0; i < el.size(); i++) {
                    if (i == 0)
                        reslist.add(r);
                    else {
                        Resource r1 = getResource(baseURI + range.getLocalName() + "_" + idCounter, range);
                        reslist.add(r1);
                        idCounter++;
                    }
                }

                if (mySwitch == 0) {
                    // bind the properties with literal values only if we are
                    // actually dealing with literals
                    List<String> literals = new ArrayList<>();
                    for (int i = 0; i < el.size(); i++) {
                        literals.add((String) el.get(i));
                    }
                    addListInstanceProperties(reslist, literals, listrange);
                } else {
                    for (int i = 0; i < reslist.size(); i++) {
                        Resource r1 = reslist.get(i);
                        IFCVO vo = (IFCVO) el.get(i);
                        EntityVO evorange = ent.get(ExpressReader.formatClassName((vo).getName()));
                        OntResource rclass = ontModel.getOntResource(ontNS + evorange.getName());
                        if(rclass==null)
                            eventBus.post(new SystemErrorEvent("rw970 rclass null: "+ontNS + evorange.getName()));
                        Resource r2 = getResource(baseURI + evorange.getName() + "_" + (vo).getLineNum(), rclass);
                        LOG.info("*OK 21*: created resource: " + r2.getLocalName());
                        idCounter++;
                        ttlWriter.triple(new Triple(r1.asNode(), ontModel.getOntProperty(LIST_NS + "hasContents").asNode(), r2.asNode()));
                        LOG.info("*OK 22*: added property: " + r1.getLocalName() + " - " + "-hasContents-" + " - " + r2.getLocalName());

                        if (i < el.size() - 1) {
                            ttlWriter.triple(new Triple(r1.asNode(), ontModel.getOntProperty(LIST_NS + "hasNext").asNode(), reslist.get(i + 1).asNode()));
                            LOG.info("*OK 23*: added property: " + r1.getLocalName() + " - " + "-hasNext-" + " - " + reslist.get(i + 1).getLocalName());
                        }
                    }
                }
            }
        }
    }

    private void addRegularListProperty(Resource r, OntProperty p, List<String> el, TypeVO typeRemembranceOverride) throws IOException {
        OntResource range = p.getRange();
        if (range.isClass()) {
            OntResource listrange = getListContentType(range.asClass());
            if (typeRemembranceOverride != null) {
                OntClass cla = ontModel.getOntClass(ontNS + typeRemembranceOverride.getName());
                listrange = cla;
            }

            if (listrange == null) {
                LOG.error("*ERROR 14*: We could not find what kind of content is expected in the LIST.");
                eventBus.post(new SystemErrorEvent("*ERROR 14*: We could not find what kind of content is expected in the LIST."));

            } else {
                if (listrange.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList"))) {
                    LOG.warn("*WARNING 28*: Found unhandled ListOfList");
                } else {
                    List<Resource> reslist = new ArrayList<>();
                    // createrequirednumberofresources
                    for (int ii = 0; ii < el.size(); ii++) {
                        Resource r1 = getResource(baseURI + range.getLocalName() + "_" + idCounter, range);
                        reslist.add(r1);
                        idCounter++;
                        if (ii == 0) {
                            ttlWriter.triple(new Triple(r.asNode(), p.asNode(), r1.asNode()));
                            LOG.info("*OK 7*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r1.getLocalName());
                        }
                    }
                    // bindtheproperties
                    addListInstanceProperties(reslist, el, listrange);
                }
            }
        }
    }

    private void createLiteralProperty(Resource r, OntResource p, OntResource range, String literalString) throws IOException {
        String xsdType = getXSDTypeFromRange(range);
        if (xsdType == null) {
            xsdType = getXSDTypeFromRangeExpensiveMethod(range);
        }
        if (xsdType != null) {
            String xsdTypeCAP = Character.toUpperCase(xsdType.charAt(0)) + xsdType.substring(1);
            OntProperty valueProp = ontModel.getOntProperty(EXPRESS_NS + "has" + xsdTypeCAP);
            String key = valueProp.toString() + ":" + xsdType + ":" + literalString;

            Resource r1 = propertyResourceMap.get(key);
            if (r1 == null) {
                r1 = ResourceFactory.createResource(baseURI + range.getLocalName() + "_" + idCounter);
                ttlWriter.triple(new Triple(r1.asNode(), RDF.type.asNode(), range.asNode()));
                LOG.info("*OK 17*: created resource: " + r1.getLocalName());
                idCounter++;
                propertyResourceMap.put(key, r1);
                addLiteralToResource(r1, valueProp, xsdType, literalString);
            }
            ttlWriter.triple(new Triple(r.asNode(), p.asNode(), r1.asNode()));
            LOG.info("*OK 3*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r1.getLocalName());
        } else {
            LOG.error("*ERROR 1*: XSD type not found for: " + p + " - " + range.getURI() + " - " + literalString);
            eventBus.post(new SystemErrorEvent("*ERROR 1*: XSD type not found for: " + p + " - " + range.getURI() + " - " + literalString));

        }
    }

    private void addListPropertyToGivenEntities(Resource r, OntProperty p, List<Resource> el) throws IOException {
        OntResource range = p.getRange();
        if (range.isClass()) {
            OntResource listrange = getListContentType(range.asClass());

            if (listrange != null) {
                if (listrange.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList"))) {
                    LOG.info("*OK 20*: Handling list of list");
                    listrange = range;
                }
                for (int i = 0; i < el.size(); i++) {
                    Resource r1 = el.get(i);
                    Resource r2 = ResourceFactory.createResource(baseURI + range.getLocalName() + "_" + idCounter); // was
                    // listrange
                    ttlWriter.triple(new Triple(r2.asNode(), RDF.type.asNode(), range.asNode()));
                    LOG.info("*OK 14*: added property: " + r2.getLocalName() + " - rdf:type - " + range.getLocalName());
                    idCounter++;
                    Resource r3 = ResourceFactory.createResource(baseURI + range.getLocalName() + "_" + idCounter);

                    if (i == 0) {
                        ttlWriter.triple(new Triple(r.asNode(), p.asNode(), r2.asNode()));
                        LOG.info("*OK 15*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r2.getLocalName());
                    }
                    ttlWriter.triple(new Triple(r2.asNode(), ontModel.getOntProperty(LIST_NS + "hasContents").asNode(), r1.asNode()));
                    LOG.info("*OK 16*: added property: " + r2.getLocalName() + " - " + "-hasContents-" + " - " + r1.getLocalName());

                    if (i < el.size() - 1) {
                        ttlWriter.triple(new Triple(r2.asNode(), ontModel.getOntProperty(LIST_NS + "hasNext").asNode(), r3.asNode()));
                        LOG.info("*OK 17*: added property: " + r2.getLocalName() + " - " + "-hasNext-" + " - " + r3.getLocalName());
                    }
                }
            }
        }
    }

    private void fillClassInstanceList(LinkedList<Object> tmpList, OntResource typerange, OntProperty p, Resource r) throws IOException {
        List<Resource> reslist = new ArrayList<>();
        List<IFCVO> entlist = new ArrayList<>();

        // createrequirednumberofresources
        for (int i = 0; i < tmpList.size(); i++) {
            if (IFCVO.class.isInstance(tmpList.get(i))) {
                Resource r1 = getResource(baseURI + typerange.getLocalName() + "_" + idCounter, typerange);
                reslist.add(r1);
                idCounter++;
                entlist.add((IFCVO) tmpList.get(i));
                if (i == 0) {
                    ttlWriter.triple(new Triple(r.asNode(), p.asNode(), r1.asNode()));
                    LOG.info("*OK 13*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r1.getLocalName());
                }
            }
        }

        addClassInstanceListProperties(reslist, entlist);
    }

    private void addClassInstanceListProperties(List<Resource> reslist, List<IFCVO> entlist) throws IOException {
        OntProperty listp = ontModel.getOntProperty(LIST_NS + "hasContents");
        OntProperty isfollowed = ontModel.getOntProperty(LIST_NS + "hasNext");

        for (int i = 0; i < reslist.size(); i++) {
            Resource r = reslist.get(i);

            OntResource rclass = null;
            EntityVO evorange = ent.get(ExpressReader.formatClassName(entlist.get(i).getName()));
            if (evorange == null) {
                TypeVO typerange = typ.get(ExpressReader.formatClassName(entlist.get(i).getName()));
                rclass = ontModel.getOntResource(ontNS + typerange.getName());
                if(rclass==null)
                    eventBus.post(new SystemErrorEvent("rw126 rclass null: "+ontNS + typerange.getName()));
                Resource r1 = getResource(baseURI + typerange.getName() + "_" + entlist.get(i).getLineNum(), rclass);
                ttlWriter.triple(new Triple(r.asNode(), listp.asNode(), r1.asNode()));
                LOG.info("*OK 8*: created property: " + r.getLocalName() + " - " + listp.getLocalName() + " - " + r1.getLocalName());
            } else {
                rclass = ontModel.getOntResource(ontNS + evorange.getName());
                if(rclass==null)
                    eventBus.post(new SystemErrorEvent("rw126 rclass null: "+ontNS + evorange.getName()));
                Resource r1 = getResource(baseURI + evorange.getName() + "_" + entlist.get(i).getLineNum(), rclass);
                ttlWriter.triple(new Triple(r.asNode(), listp.asNode(), r1.asNode()));
                LOG.info("*OK 9*: created property: " + r.getLocalName() + " - " + listp.getLocalName() + " - " + r1.getLocalName());
            }

            if (i < reslist.size() - 1) {
                ttlWriter.triple(new Triple(r.asNode(), isfollowed.asNode(), reslist.get(i + 1).asNode()));
                LOG.info("*OK 10*: created property: " + r.getLocalName() + " - " + isfollowed.getLocalName() + " - " + reslist.get(i + 1).getLocalName());
            }
        }
    }

    private void addListInstanceProperties(List<Resource> reslist, List<String> listelements, OntResource listrange) throws IOException {
        // GetListType
        String xsdType = getXSDTypeFromRange(listrange);
        if (xsdType == null)
            xsdType = getXSDTypeFromRangeExpensiveMethod(listrange);
        if (xsdType != null) {
            String xsdTypeCAP = Character.toUpperCase(xsdType.charAt(0)) + xsdType.substring(1);
            OntProperty valueProp = ontModel.getOntProperty(EXPRESS_NS + "has" + xsdTypeCAP);

            // Adding Content only if found
            for (int i = 0; i < reslist.size(); i++) {
                Resource r = reslist.get(i);
                String literalString = listelements.get(i);
                String key = valueProp.toString() + ":" + xsdType + ":" + literalString;
                Resource r2 = propertyResourceMap.get(key);
                if (r2 == null) {
                    r2 = ResourceFactory.createResource(baseURI + listrange.getLocalName() + "_" + idCounter);
                    ttlWriter.triple(new Triple(r2.asNode(), RDF.type.asNode(), listrange.asNode()));
                    LOG.info("*OK 19*: created resource: " + r2.getLocalName());
                    idCounter++;
                    propertyResourceMap.put(key, r2);
                    addLiteralToResource(r2, valueProp, xsdType, literalString);
                }
                ttlWriter.triple(new Triple(r.asNode(), ontModel.getOntProperty(LIST_NS + "hasContents").asNode(), r2.asNode()));
                LOG.info("*OK 11*: added property: " + r.getLocalName() + " - " + "-hasContents-" + " - " + r2.getLocalName());

                if (i < listelements.size() - 1) {
                    ttlWriter.triple(new Triple(r.asNode(), ontModel.getOntProperty(LIST_NS + "hasNext").asNode(), reslist.get(i + 1).asNode()));
                    LOG.info("*OK 12*: added property: " + r.getLocalName() + " - " + "-hasNext-" + " - " + reslist.get(i + 1).getLocalName());
                }
            }
        } else {
            LOG.error("*ERROR 5*: XSD type not found for: " + listrange.getLocalName());
            eventBus.post(new SystemErrorEvent("*ERROR 5*: XSD type not found for: " + listrange.getLocalName()));
        }
    }

    // HELPER METHODS
    private String filterExtras(String txt) {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < txt.length(); n++) {
            char ch = txt.charAt(n);
            switch (ch) {
                case '\'':
                    break;
                case '=':
                    break;
                default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private String filterPoints(String txt) {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < txt.length(); n++) {
            char ch = txt.charAt(n);
            switch (ch) {
                case '.':
                    break;
                default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private Long toLong(String txt) {
        try {
            return Long.valueOf(txt);
        } catch (NumberFormatException e) {
            eventBus.post(new SystemErrorEvent(e.getMessage()));
            return Long.MIN_VALUE;
        }
    }

    private void addLiteral(Resource r, OntProperty valueProp, Literal l) {
        ttlWriter.triple(new Triple(r.asNode(), valueProp.asNode(), l.asNode()));
    }

    private void addProperty(Resource r, OntProperty valueProp, Resource r1) {
        ttlWriter.triple(new Triple(r.asNode(), valueProp.asNode(), r1.asNode()));
    }

    private OntResource getListContentType(OntClass range) throws IOException {
        String resourceURI = range.asClass().getURI();
        OntResource ret;
        if ((EXPRESS_NS + "STRING_List").equalsIgnoreCase(resourceURI) || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "STRING_List")))
           ret= ontModel.getOntResource(EXPRESS_NS + "STRING");
        else if ((EXPRESS_NS + "REAL_List").equalsIgnoreCase(resourceURI) || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "REAL_List")))
           ret= ontModel.getOntResource(EXPRESS_NS + "REAL");
        else if ((EXPRESS_NS + "INTEGER_List").equalsIgnoreCase(resourceURI) || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "INTEGER_List")))
           ret= ontModel.getOntResource(EXPRESS_NS + "INTEGER");
        else if ((EXPRESS_NS + "BINARY_List").equalsIgnoreCase(resourceURI) || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "BINARY_List")))
            ret= ontModel.getOntResource(EXPRESS_NS + "BINARY");
        else if ((EXPRESS_NS + "BOOLEAN_List").equalsIgnoreCase(resourceURI) || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "BOOLEAN_List")))
            ret=  ontModel.getOntResource(EXPRESS_NS + "BOOLEAN");
        else if ((EXPRESS_NS + "LOGICAL_List").equalsIgnoreCase(resourceURI) || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "LOGICAL_List")))
            ret=  ontModel.getOntResource(EXPRESS_NS + "LOGICAL");
        else if ((EXPRESS_NS + "NUMBER_List").equalsIgnoreCase(resourceURI) || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "NUMBER_List")))
            ret=  ontModel.getOntResource(EXPRESS_NS + "NUMBER");
        else if (range.asClass().hasSuperClass(ontModel.getOntClass(LIST_NS + "OWLList"))) {
            String listvaluepropURI = ontNS + range.getLocalName().substring(0, range.getLocalName().length() - 5);
            ret=  ontModel.getOntResource(listvaluepropURI);
        } else {
            LOG.warn("*WARNING 29*: did not find listcontenttype for : {}", range.getLocalName());
            eventBus.post(new SystemErrorEvent("*WARNING 29*: did not find listcontenttype for : "+ range.getLocalName()));
            return null;
        }
        if(ret==null)
            eventBus.post(new SystemErrorEvent("*WARNING 29*: did not find listcontenttype for : "+ range.getLocalName()));
        return ret;
    }

    private String getXSDTypeFromRange(OntResource range) {
        if (range.asClass().getURI().equalsIgnoreCase(EXPRESS_NS + "STRING") || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "STRING")))
            return "string";
        else if (range.asClass().getURI().equalsIgnoreCase(EXPRESS_NS + "REAL") || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "REAL")))
            return "double";
        else if (range.asClass().getURI().equalsIgnoreCase(EXPRESS_NS + "INTEGER") || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "INTEGER")))
            return "integer";
        else if (range.asClass().getURI().equalsIgnoreCase(EXPRESS_NS + "BINARY") || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "BINARY")))
            return "hexBinary";
        else if (range.asClass().getURI().equalsIgnoreCase(EXPRESS_NS + "BOOLEAN") || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "BOOLEAN")))
            return "boolean";
        else if (range.asClass().getURI().equalsIgnoreCase(EXPRESS_NS + "LOGICAL") || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "LOGICAL")))
            return "logical";
        else if (range.asClass().getURI().equalsIgnoreCase(EXPRESS_NS + "NUMBER") || range.asClass().hasSuperClass(ontModel.getOntClass(EXPRESS_NS + "NUMBER")))
            return "double";
        else
            return null;
    }

    private String getXSDTypeFromRangeExpensiveMethod(OntResource range) {
        ExtendedIterator<OntClass> iter = range.asClass().listSuperClasses();
        while (iter.hasNext()) {
            OntClass superc = iter.next();
            if (!superc.isAnon()) {
                String type = getXSDTypeFromRange(superc);
                if (type != null)
                    return type;
            }
        }
        return null;
    }

    private Resource getResource(String uri, OntResource rclass) {
        if(rclass==null)
            eventBus.post(new SystemErrorEvent("getResource failed for " + uri+" rclass is null"));
        Resource r = resourceMap.get(uri);
        if (r == null) {
            r = ResourceFactory.createResource(uri);
            if(r==null)
                eventBus.post(new SystemErrorEvent("getResource failed for " + uri+" r is null"));
            resourceMap.put(uri, r);
            try {
                ttlWriter.triple(new Triple(r.asNode(), RDF.type.asNode(), rclass.asNode()));
            } catch (Exception e) { 
                e.printStackTrace();
                eventBus.post(new SystemErrorEvent(e.getClass().getName()+" - "+e.getMessage()));
                LOG.error("*ERROR 2*: getResource failed for " + uri); 
                eventBus.post(new SystemErrorEvent("*ERROR 2*: getResource failed for " + uri));
                return null;
            }
        }
        return r;
    }

    public boolean isRemoveDuplicates() {
        return removeDuplicates;
    }

    public void setRemoveDuplicates(boolean removeDuplicates) {
        this.removeDuplicates = removeDuplicates;
    }

}

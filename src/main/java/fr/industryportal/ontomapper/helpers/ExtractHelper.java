package fr.industryportal.ontomapper.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.industryportal.ontomapper.config.Config;
import fr.industryportal.ontomapper.controller.CronController;
import fr.industryportal.ontomapper.model.entities.Contribution;
import fr.industryportal.ontomapper.model.entities.Contributor;
import fr.industryportal.ontomapper.model.entities.Mapping;
import fr.industryportal.ontomapper.model.entities.enums.MappingSetType;
import fr.industryportal.ontomapper.model.entities.enums.MappingType;
import fr.industryportal.ontomapper.model.repos.LinkedDataMappingRepository;
import fr.industryportal.ontomapper.model.repos.MappingRepository;
import fr.industryportal.ontomapper.model.repos.MappingSetRepository;
import fr.industryportal.ontomapper.model.requests.LinkedDataMappingRequest;
import fr.industryportal.ontomapper.model.requests.MappingRequest;
import fr.industryportal.ontomapper.model.requests.SetRequest;
import fr.industryportal.ontomapper.services.ApiService;
import jakarta.json.Json;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author Nasreddine Bouchemel
 */
public class ExtractHelper {


    private static final Pattern RELATION_PATTERN = Pattern.compile("([!\\^\\+]?)(.+)");

    public static Logger logger =  Logger.getLogger(CronController.class.getName());


    public static OWLOntology getOntologyFromFile(File file) {
        OntologyConfigurator configurator = new OntologyConfigurator();
        configurator.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        manager.setOntologyConfigurator(configurator);
        OntoMapperMissingImportsListener importsListener = new OntoMapperMissingImportsListener();
        manager.addMissingImportListener(importsListener);
        OWLOntology ontology;
        try {
            ontology = manager.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            ExtractHelper.logger.log(Level.SEVERE,"error reading ontology file");
            e.printStackTrace();
            return null;
        } catch (UnloadableImportException e) {
            ExtractHelper.logger.log(Level.SEVERE,"import could not be loaded correctly");
            e.printStackTrace();
            return null;
        }

        return ontology;

    }

    public static String replaceClassURIsWithLabels(String ontologyString, OWLOntology ontology, OWLAxiom axiom) {
        String replacedString = ontologyString;

        // Get all class axioms from the ontology
        for (OWLEntity cls : axiom.getSignature()) {
            // Get the class URI and label
            String classURI = cls.getIRI().toString();
            String label = getClassLabel(classURI, ontology);

            //check if classUri is in replacedString first
            if (!replacedString.contains(classURI)) continue;

            // Replace class URI with label(uri) in the string
            replacedString = replacedString.replace(classURI, "!!!" + label + "(" + classURI + ")");
        }

        return replacedString.replace("<", "").replace(">", "");
    }

    public static String getClassLabel(String classUri, OWLOntology ontology) {
//        OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
//        OWLClass cls = dataFactory.getOWLClass(IRI.create(classUri));
//
//        ontology.getAnnotations().iterator().next().annotationValue();
//
//        for (OWLAnnotationAssertionAxiom axiom : ontology.getAnnotationAssertionAxioms(cls.getIRI())) {
//
//            if (  axiom.getProperty().isLabel()) {
//                OWLLiteral literal = (OWLLiteral) axiom.getValue();
//                String label = literal.getLiteral();
//                return label;
//
//            }
//        }
//
//        // Return the class URI as the label if no label annotation is found
//        return classUri;
        // Get the class for the given URI
        // Get the class for the given URI
        // Get the class for the given URI
 /*       OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLClass cls = dataFactory.getOWLClass(IRI.create(classUri));

        // Retrieve the annotations on the class for rdfs:label
        OWLAnnotationProperty labelProperty = dataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
        for (OWLAnnotationAssertionAxiom annotationAxiom : ontology.getAnnotationAssertionAxioms(cls.getIRI())) {
            if (annotationAxiom.getProperty().equals(labelProperty)) {
                OWLAnnotationValue value = annotationAxiom.getValue();
                if (value instanceof OWLLiteral) {
                    return ((OWLLiteral) value).getLiteral();
                }
            }
        }*/
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        OWLClass targetClass = dataFactory.getOWLClass(IRI.create(classUri));

        for (OWLAnnotationAssertionAxiom axiom : ontology.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
            OWLAnnotationSubject subject = axiom.getSubject();
            if (subject instanceof IRI && subject.equals(targetClass.getIRI())) {
                OWLAnnotationValue value = axiom.getValue();
                if (value instanceof OWLLiteral) {
                    return ((OWLLiteral) value).getLiteral();
                }
            }
        }

        return targetClass.getIRI().getFragment(); // Use the class fragment (the last part of the iri) as the label if no rdfs:label found

    }

    public String convertToManchesterSyntax(OWLAxiom axiom, String classUri, String axiomType) {
        //OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
        //loaderConfig = loaderConfig.setLoadAnnotationAxioms(false); // Skip loading annotation axioms
        ManchesterOWLSyntaxOntologyFormat format = new ManchesterOWLSyntaxOntologyFormat();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;

        try {

            ontology = manager.createOntology();
            manager.addAxiom(ontology, axiom);
            StringDocumentTarget documentTarget = new StringDocumentTarget();
            manager.saveOntology(ontology, format, documentTarget);
            return extractAxiomString(documentTarget.toString(), classUri, axiomType);
        } catch (OWLOntologyCreationException | OWLOntologyStorageException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String extractAxiomString(String serializedOntology, String classUri, String axiomType) {
        serializedOntology = serializedOntology.replaceAll("\\n", "").replaceAll("\\s+", " ").trim();
//        int axiomSimplicity = isSimpleAxiom(serializedOntology, axiomType);
//        if ( axiomSimplicity == 0 ) {
//            String mappingPattern = "Class:\\s*<([^>]+)>";
//
//            Pattern pattern = Pattern.compile(mappingPattern);
//            Matcher matcher = pattern.matcher(serializedOntology);
//
//            while (matcher.find()) {
//                if  (matcher.group(1).equals(classUri)) continue;
//                return matcher.group(1);
//            }
//        } else if (axiomSimplicity == 1) {
//            StringBuilder result = new StringBuilder();
//            int openingParenthesesCount = 0;
//            int closingParenthesesCount = 0;
//            boolean foundOpeningParenthesis = false;
//            boolean capture = false;
//
//            for (int i = serializedOntology.length() - 1; i >= 0; i--) {
//                char c = serializedOntology.charAt(i);
//
//                if (c == ')') {
//                    closingParenthesesCount++;
//                    capture = true;
//                } else if (c == '(') {
//                    openingParenthesesCount++;
//                    if (openingParenthesesCount == closingParenthesesCount) {
//                        foundOpeningParenthesis = true;
//                    }
//                } else if (foundOpeningParenthesis && c == ':' && serializedOntology.charAt(i + 1) == ' ') {
//                    capture = false;
//                    break;
//                }
//
//                if (capture) {
//                    result.insert(0, c);
//                }
//            }
//            return result.toString();
//        } else
        if (axiomType.equals("DisjointUnion")) {
            String searchToken = "DisjointUnionOf:";
            int startIndex = serializedOntology.indexOf(searchToken);
            if (startIndex != -1) {
                startIndex += searchToken.length();
                int endIndex = serializedOntology.indexOf("Class:", startIndex);
                if (endIndex == -1) {
                    endIndex = serializedOntology.length();
                }
                String disjointUnion = serializedOntology.substring(startIndex, endIndex).trim();
                return disjointUnion;
            }
        } else if (axiomType.equals("DisjointClasses")) {
            String searchToken = "DisjointWith:";
            int startIndex = serializedOntology.indexOf(searchToken);
            if (startIndex != -1) {
                startIndex += searchToken.length();
                int endIndex = serializedOntology.indexOf("Class:", startIndex);
                if (endIndex == -1) {
                    endIndex = serializedOntology.length();
                }
                String disjointUnion = serializedOntology.substring(startIndex, endIndex).trim();
                return disjointUnion;
            }
        } else if (axiomType.equals("SubClassOf")) {
            String searchToken = "SubClassOf:";
            int startIndex = serializedOntology.indexOf(searchToken);
            if (startIndex != -1) {
                startIndex += searchToken.length();
                int endIndex = serializedOntology.indexOf("Class:", startIndex);
                if (endIndex == -1) {
                    endIndex = serializedOntology.length();
                }
                String disjointUnion = serializedOntology.substring(startIndex, endIndex).trim();
                return disjointUnion;
            }
        } else if (axiomType.equals("EquivalentClasses")) {
            String searchToken = "EquivalentTo:";
            int startIndex = serializedOntology.indexOf(searchToken);
            if (startIndex != -1) {
                startIndex += searchToken.length();
                int endIndex = serializedOntology.indexOf("Class:", startIndex);
                if (endIndex == -1) {
                    endIndex = serializedOntology.length();
                }
                String disjointUnion = serializedOntology.substring(startIndex, endIndex).trim();
                return disjointUnion;
            }
        } else {
            return serializedOntology;
        }
        return serializedOntology;
    }

    public String downloadOntologyFile(String apikey, String acronym) {
        try {
            URL url = new URL(Config.API_URL + "/ontologies/" + acronym + "/download?apikey=" + apikey);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "*/*");

            if (con.getResponseCode() != 200) {
                return null;
            }

            //getting file name from header "content-disposition"
            String fileName = con.getHeaderField("Content-Disposition").split("=\"")[1];
            String lastModified = con.getHeaderField("Last-Modified");
            fileName = lastModified + ' ' + fileName.substring(0, fileName.length() - 1);

            File file = new File(Config.ONTOLOGY_FOLDER, fileName);

            // Check if the file exists
            if (file.exists()) {
                ExtractHelper.logger.log(Level.INFO,"The file already exists.");
                return Config.ONTOLOGY_FOLDER + fileName;
            }

            //BufferedInputStream in = new BufferedInputStream(con.getErrorStream());
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            FileOutputStream fileOutputStream = new FileOutputStream(new File(Config.ONTOLOGY_FOLDER + fileName));
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            ExtractHelper.logger.log(Level.INFO,"file downloading done");
            return Config.ONTOLOGY_FOLDER + fileName;
        } catch (IOException e) {
            // handle exception
            e.printStackTrace();
            return null;
        }
    }

    public String getOntologySource(File file) {
        try {
            OWLOntology ontology = ExtractHelper.getOntologyFromFile(file);

            return ontology.getOntologyID().getOntologyIRI().get().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public JSONObject getIntraMappingSet(String name, Model m, StmtIterator siter, String ontologyVersion) {
        JSONArray creators = new JSONArray();
        JSONObject creator = new JSONObject();
        creator.put("id", "nass");
        creator.put("label", "nass");
        creator.put("type", "AUTHOR");
        creators.put(creator);

        JSONObject mappingSetJSON = new JSONObject();
        mappingSetJSON.put("comment", getComment());
        mappingSetJSON.put("creators", creators);
        mappingSetJSON.put("description", "");
        mappingSetJSON.put("license", "");
        mappingSetJSON.put("mapping_date", getDate());
        mappingSetJSON.put("mapping_provider", "");
        mappingSetJSON.put("mapping_set_id", name + "#intra");
        mappingSetJSON.put("mapping_tool", getMappingTool(getComment()));
        mappingSetJSON.put("source", new JSONArray());
        mappingSetJSON.put("version", ontologyVersion);
        mappingSetJSON.put("mapping_set_source", m.getNsPrefixURI(""));
        mappingSetJSON.put("mapping_set_title", "");
        mappingSetJSON.put("subject_source", m.getNsPrefixURI(""));
        mappingSetJSON.put("subject_type", getType(siter));
        mappingSetJSON.put("subject_source_version", "");
        mappingSetJSON.put("subject_preprocessing", "");
        mappingSetJSON.put("subject_match_field", getMatchField(getComment()));
        mappingSetJSON.put("object_source", m.getNsPrefixURI(""));
        mappingSetJSON.put("object_type", getType(siter));
        mappingSetJSON.put("object_source_version", "");
        mappingSetJSON.put("object_preprocessing", "");
        mappingSetJSON.put("object_match_field", getMatchField(getComment()));
        mappingSetJSON.put("other", "");
        mappingSetJSON.put("see_also", "");

        return mappingSetJSON;
    }

    public JSONObject geInterMappingSet(String name, Model m, StmtIterator siter, String ontologyVersion) {
        JSONArray creators = new JSONArray();
        JSONObject creator = new JSONObject();
        creator.put("id", "nass");
        creator.put("label", "nass");
        creator.put("type", "AUTHOR");
        creators.put(creator);

        JSONObject mappingSetJSON = new JSONObject();
        mappingSetJSON.put("comment", getComment());
        mappingSetJSON.put("creators", creators);
        mappingSetJSON.put("description", "");
        mappingSetJSON.put("license", "");
        mappingSetJSON.put("mapping_date", getDate());
        mappingSetJSON.put("mapping_provider", "");
        mappingSetJSON.put("mapping_set_id", name + "#inter");
        mappingSetJSON.put("mapping_tool", getMappingTool(getComment()));
        mappingSetJSON.put("source", new JSONArray());
        mappingSetJSON.put("version", ontologyVersion);
        mappingSetJSON.put("mapping_set_source", m.getNsPrefixURI(""));
        mappingSetJSON.put("mapping_set_title", "");
        mappingSetJSON.put("subject_source", m.getNsPrefixURI(""));
        mappingSetJSON.put("subject_type", getType(siter));
        mappingSetJSON.put("subject_source_version", "");
        mappingSetJSON.put("subject_preprocessing", "");
        mappingSetJSON.put("subject_match_field", getMatchField(getComment()));
        mappingSetJSON.put("object_source", m.getNsPrefixURI(""));
        mappingSetJSON.put("object_type", getType(siter));
        mappingSetJSON.put("object_source_version", "");
        mappingSetJSON.put("object_preprocessing", "");
        mappingSetJSON.put("object_match_field", getMatchField(getComment()));
        mappingSetJSON.put("other", "");
        mappingSetJSON.put("see_also", "");

        return mappingSetJSON;
    }

    public JSONObject getCrossMappingSet(String name, Model m, StmtIterator siter, String ontologyVersion) {
        JSONArray creators = new JSONArray();
        JSONObject creator = new JSONObject();
        creator.put("id", "nass");
        creator.put("label", "nass");
        creator.put("type", "AUTHOR");
        creators.put(creator);

        JSONObject mappingSetJSON = new JSONObject();
        mappingSetJSON.put("comment", getComment());
        mappingSetJSON.put("creators", creators);
        mappingSetJSON.put("description", "");
        mappingSetJSON.put("license", "");
        mappingSetJSON.put("mapping_date", getDate());
        mappingSetJSON.put("mapping_provider", "");
        mappingSetJSON.put("mapping_set_id", name + "#inter");
        mappingSetJSON.put("mapping_tool", getMappingTool(getComment()));
        mappingSetJSON.put("source", new JSONArray());
        mappingSetJSON.put("version", ontologyVersion);
        mappingSetJSON.put("mapping_set_source", m.getNsPrefixURI(""));
        mappingSetJSON.put("mapping_set_title", "");
        mappingSetJSON.put("subject_source", m.getNsPrefixURI(""));
        mappingSetJSON.put("subject_type", getType(siter));
        mappingSetJSON.put("subject_source_version", "");
        mappingSetJSON.put("subject_preprocessing", "");
        mappingSetJSON.put("subject_match_field", getMatchField(getComment()));
        mappingSetJSON.put("object_source", m.getNsPrefixURI(""));
        mappingSetJSON.put("object_type", getType(siter));
        mappingSetJSON.put("object_source_version", "");
        mappingSetJSON.put("object_preprocessing", "");
        mappingSetJSON.put("object_match_field", getMatchField(getComment()));
        mappingSetJSON.put("other", "");
        mappingSetJSON.put("see_also", "");

        return mappingSetJSON;
    }

    public JSONObject getCrossMappingSet(OWLOntology ontology, String acronym) {
        JSONArray creators = new JSONArray();
        JSONObject creator = new JSONObject();
        creator.put("id", "ontomapper");
        creator.put("label", "ontomapper service");
        creator.put("type", "AUTHOR");
        creators.put(creator);

        JSONObject mappingSetJSON = new JSONObject();
        mappingSetJSON.put("comment", getComment());
        mappingSetJSON.put("creators", creators);
        mappingSetJSON.put("description", "");
        mappingSetJSON.put("license", "");
        mappingSetJSON.put("mapping_date", getDate());
        mappingSetJSON.put("mapping_provider", "");
        mappingSetJSON.put("mapping_set_id", acronym + "#cross");
        mappingSetJSON.put("mapping_tool", "Ontomapper");
        mappingSetJSON.put("source", new JSONArray());
        mappingSetJSON.put("version", getOntologyVersion(ontology));
        mappingSetJSON.put("mapping_set_source", getOntologyUri(ontology, acronym));
        mappingSetJSON.put("mapping_set_title", acronym + "cross mappings");
        mappingSetJSON.put("subject_source", getOntologyUri(ontology, acronym));
        mappingSetJSON.put("subject_type", "OWL_CLASS");
        mappingSetJSON.put("subject_source_version", "");
        mappingSetJSON.put("subject_preprocessing", "");
        mappingSetJSON.put("subject_match_field", getMatchField(getComment()));
        mappingSetJSON.put("object_source", "");
        mappingSetJSON.put("object_type", "OWL_CLASS");
        mappingSetJSON.put("object_source_version", "");
        mappingSetJSON.put("object_preprocessing", "");
        mappingSetJSON.put("object_match_field", "");
        mappingSetJSON.put("other", "");
        mappingSetJSON.put("see_also", "");

        return mappingSetJSON;
    }

    public JSONObject getInterMappingSet(OWLOntology ontology, String acronym) {
        JSONArray creators = new JSONArray();
        JSONObject creator = new JSONObject();
        creator.put("id", "ontomapper");
        creator.put("label", "ontomapper service");
        creator.put("type", "AUTHOR");
        creators.put(creator);

        JSONObject mappingSetJSON = new JSONObject();
        mappingSetJSON.put("comment", getComment());
        mappingSetJSON.put("creators", creators);
        mappingSetJSON.put("description", "");
        mappingSetJSON.put("license", "");
        mappingSetJSON.put("mapping_date", getDate());
        mappingSetJSON.put("mapping_provider", "");
        mappingSetJSON.put("mapping_set_id", acronym + "#inter");
        mappingSetJSON.put("mapping_tool", "Ontomapper");
        mappingSetJSON.put("source", new JSONArray());
        mappingSetJSON.put("version", getOntologyVersion(ontology));
        mappingSetJSON.put("mapping_set_source", getOntologyUri(ontology, acronym));
        mappingSetJSON.put("mapping_set_title", acronym + "Inter mappings");
        mappingSetJSON.put("subject_source", getOntologyUri(ontology, acronym));
        mappingSetJSON.put("subject_type", "OWL_CLASS");
        mappingSetJSON.put("subject_source_version", "");
        mappingSetJSON.put("subject_preprocessing", "");
        mappingSetJSON.put("subject_match_field", getMatchField(getComment()));

        mappingSetJSON.put("object_source", "");
        mappingSetJSON.put("object_type", "OWL_CLASS");
        mappingSetJSON.put("object_source_version", "");
        mappingSetJSON.put("object_preprocessing", "");
        mappingSetJSON.put("object_match_field", "");
        mappingSetJSON.put("other", "");
        mappingSetJSON.put("see_also", "");

        return mappingSetJSON;
    }

    public String getMappingProvider(JSONObject intra, JSONObject inter, JSONObject cross, String mappingType) {
        switch (mappingType) {
            case "http://industryportal.enit.fr/sssom#intra":
                return intra.getString("mapping_provider");
            case "http://industryportal.enit.fr/sssom#inter":
                return inter.getString("mapping_provider");
            case "http://industryportal.enit.fr/sssom#cross":
                return cross.getString("mapping_provider");
            default:
                return "";
        }
    }

    public String getSemanticSimilarityScore(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "";
        return "";
    }

    public String getMatchString(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "";
        return "";
    }

    public boolean isLogicalAxiom(Statement s) {

        if (s.getObject().isLiteral()) return false;

        Property predicate = s.getPredicate();
        RDFNode object = s.getObject();

        if (predicate.equals(OWL.onProperty) && object.isURIResource()) {
            Resource property = object.asResource();
            if (property.hasProperty(RDF.type, OWL.DatatypeProperty)) {
                return false;
            }
        }

        if (predicate.equals(RDF.type)) return false;

        return true;


    }

    public String getOntologyUri(Model m, String acronym) {
        if (!Objects.equals(m.getNsPrefixURI(""), "")) return m.getNsPrefixURI("");
        if (m.getNsPrefixMap().containsKey(acronym.toLowerCase())) return m.getNsPrefixMap().get(acronym.toLowerCase());
        return acronym;
    }

    private String getOntologyUri(OWLOntology ontology, String acronym) {
        if (ontology.getOntologyID().getOntologyIRI().isPresent()) {
            return ontology.getOntologyID().getOntologyIRI().get().toString();
        }
        return "industryportal.enit.fr/ontologies/" + acronym;
    }

    public static String getPredicateIdentifier(String relationId) {
        Matcher matcher = RELATION_PATTERN.matcher(relationId);
        if (matcher.matches()) {
            return matcher.group(2);
        } else {
            return relationId;
        }
    }

    public static String getPredicateModifier(String relationId) {
        Matcher matcher = RELATION_PATTERN.matcher(relationId);
        if (matcher.matches()) {
            String modifier = matcher.group(1);
            if (modifier.equals("!")) {
                return "NOT";
            } else if (modifier.equals("^")) {
                return "INVERSE";
            } else if (modifier.equals("+")) {
                return "INDIRECT";
            } else {
                return "DIRECT";
            }
        } else {
            return "";
        }
    }

    public String getComment() {
        return "http://industryportal.enit.fr/sssom#manual";
    }

    public String getCategory(ArrayList<Triple> triples, String id) {
        for (Triple triple : triples
        ) {
            if (
                    triple.getSubject().toString().equals(id)
            )
                if (
                        triple.getSubject().toString().equals(id) && triple.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
                ) return triple.getObject().toString().split("/")[triple.getObject().toString().split("/").length - 1];

            if (
                    triple.getSubject().toString().equals(id) && (triple.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest") || triple.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#first"))
            ) return "rdf:List";

        }
        return "";
    }

    public String getCategory(Resource resource) {
        if (resource.hasProperty(RDF.type, OWL.Restriction)) return "OWL_RESTRICTION";
        if (resource.hasProperty(RDF.type, XSD.nonNegativeInteger)) return "XSD_NON_NEGATIVE_INTEGER";
        if (resource.hasProperty(RDF.first)) return "RDF_LIST";
        return "";
    }

    public String getType(Resource resource) {
        if (resource.hasProperty(RDF.type, OWL.Class)) return "OWL_CLASS";
        if (resource.hasProperty(RDF.type, OWL.ObjectProperty)) return "OWL_OBJECT_PROPERTY";
        if (resource.hasProperty(RDF.type, OWL.DatatypeProperty)) return "DATA_TYPE_PROPERTY";
        if (resource.hasProperty(RDF.type, OWL.Restriction)) return "OWL_CLASS";
        if (resource.hasProperty(RDF.first)) return "OWL_CLASS";

        return "OWL_CLASS";
    }

    public String getType(StmtIterator stmts) {
        while (stmts.hasNext()) {
            Statement s = stmts.next();
            try {
                Resource r = s.getResource();
                return getType(r);

            } catch (ResourceRequiredException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public String getLabel(Resource resource) {
        if (resource.hasProperty(RDFS.label))
            return resource.getProperty(RDFS.label).getObject().asLiteral().getString();
        if (resource.hasProperty(SKOS.prefLabel))
            return resource.getProperty(RDFS.label).getObject().asLiteral().getString();
        return "";
    }

    public String getMappingsJustification(Resource s, RDFNode o, Property p, String comment) {
        if (o.isResource()) {
            if (comment.equals("http://industryportal.enit.fr/sssom#manual")) {
                // get the axiom for which the mapping is derived
                if (s.hasProperty(p)) {
                    String axiom = s.getProperty(p).getObject().toString();
                    return axiom;
                }
            } else if (comment.equals("http://industryportal.enit.fr/sssom#automatic")) {
                // provide a written justification
                String justification = "TBD"; // TODO: add actual justification here
                return justification;
            }
        } else if (o.isLiteral()) {
            if (comment.equals("http://industryportal.enit.fr/sssom#manual")) {
                // get the axiom for which the mapping is derived
                String axiom = s.getProperty(p).getObject().toString();
                return axiom;
            } else if (comment.equals("http://industryportal.enit.fr/sssom#automatic")) {
                // provide a written justification
                String justification = "TBD"; // TODO: add actual justification here
                return justification;
            }
        }
        return "";
    }

    public String getOntologyVersion(String ontologyPath) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        try {
            ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
        } catch (Exception e) {
            System.err.println("Error loading ontology: " + e.getMessage());
            return "";
        }
        OWLAnnotationProperty versionProperty =
                manager.getOWLDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.OWL_VERSION_INFO.getIRI());

        String version = "";
        for (OWLAnnotation annotation : ontology.getAnnotations()) {
            if (annotation.getProperty().equals(versionProperty)) {
                version = annotation.getValue().asLiteral().get().getLiteral();
                return version;
            }
        }

        return "";
    }

    private String getOntologyVersion(OWLOntology ontology) {
        OWLOntologyID ontologyID = ontology.getOntologyID();

        // Check if there is an ontology ID and a version IRI
        if (ontologyID != null && ontologyID.getVersionIRI().isPresent()) {
            return ontologyID.getVersionIRI().get().toString();
        } else {
            // Handle the case where no version IRI is present
            return "";
        }    }

    public String getOntologySource(String uri) {
        String[] parts = uri.split("#");
        if (parts.length == 2) {
            return parts[0];
        }
        return "";
    }

    public String getConfidence(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "1";
        if (comment.equals("http://industryportal.enit.fr/sssom#automatic")) return "";
        return "";
    }

    public String getMatchField(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "1";
        return "";
    }

    public String getPreProcessing() {
        return "";
    }

    public String getSemanticSimilarity(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "";
        return "";
    }

    public String getMappingType(String subject, String object, Set<OWLOntology> imports) {
        if (getOntologySource(subject).equals(getOntologySource(object)))
            return "http://industryportal.enit.fr/sssom#intra";
        if (imports.contains(getOntologySource(subject)) || imports.contains(getOntologySource(object)))
            return "http://industryportal.enit.fr/sssom#inter";
        return "http://industryportal.enit.fr/sssom#cross";
    }

    public String getDate() {
        return LocalDateTime.now().toString();
    }

    public String getMappingTool(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "";
        return "";
    }

    public Set<OWLImportsDeclaration> getImports(String filePath) throws OWLOntologyCreationException {
        // Load the ontology from a file
        File file = new File(filePath);
        try {
            OWLOntology ontology = ExtractHelper.getOntologyFromFile(file);
// Get the imports of the ontology
            return ontology.getImportsDeclarations();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptySet();
        }

    }

    public int isSimpleAxiom(String axiom, String axiomType) {
        // if ( countOccurrences(axiom, "Class:") == 2) return true ;
        //connectors available in the manchester format ,i fthey are present in the axiom string then is it a complex axiom
        String[] complexPatterns = {"and", "or", "some", "only", "not", "value", "exactly", "min", "max", "inverse", "self", "that", "where", "orMore", "orLess", "intersectionOf", "unionOf", "complementOf", "oneOf", "disjointWith", "subClassOf", "equivalentTo"};
        String patternString = "\\b(" + String.join("|", complexPatterns) + ")\\b";
        if (Pattern.compile(patternString).matcher(axiom).find() || countOccurrences(axiom, "Class:") > 2 || axiomType.equals("DisjointUnion")) {
            //if it contains parenthesis, we'll return one, to handle the axiom depending on opening and closing parenthesis
            if (axiom.contains("(")) return 1;
            //if it doesn't contain parenthesis, we'll return two, to handle the axiom without depending on them in parsing
            return 2;
        }
        //if it doesn't find any matches it returns zero, meaning it is a simple axiom
        return 0;

    }

    public int countOccurrences(String text, String searchString) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(searchString, index)) != -1) {
            count++;
            index += searchString.length();
        }
        return count;
    }

    private MappingSetType isMappingToOtherOntology(OWLClass sourceClass, OWLClass targetClass, OWLOntology ontology) {

        // Get the IRI of the class expression
        IRI sourceClassIRI = sourceClass.getIRI();
        IRI targetClassIRI = targetClass.getIRI();

        // Get the IRI of the current ontology
        String ontologyIRI = ontology.getOntologyID().getOntologyIRI().get().getNamespace();

        // Check if the class expression refers to another ontology without importing it
        if (!sourceClassIRI.toString().contains(ontologyIRI)) {
            // Get the imported ontologies
            Set<OWLOntology> importedOntologies = ontology.getImports();

            // Check if the other ontology is imported
            for (OWLOntology importedOntology : importedOntologies) {
                if (importedOntology.getOntologyID().getOntologyIRI().equals(sourceClassIRI)) {
                    // Class expression refers to an imported ontology
                    return MappingSetType.INTER;
                }
            }

            // Class expression does not refer to an imported ontology
            return MappingSetType.CROSS;
        } else if (!targetClassIRI.toString().contains(ontologyIRI.toString())) {
            // Get the imported ontologies
            Set<OWLOntology> importedOntologies = ontology.getImports();

            // Check if the other ontology is imported
            for (OWLOntology importedOntology : importedOntologies) {
                if (importedOntology.getOntologyID().getOntologyIRI().equals(targetClassIRI)) {
                    // Class expression refers to an imported ontology
                    return MappingSetType.INTER;
                }
            }

            // Class expression does not refer to an imported ontology
            return MappingSetType.CROSS;
        }

        // Class expression refers to the same ontology
        return MappingSetType.INTRA;
    }

    private Long postAndGetMappingSetId(JSONObject mappingSet, String apikey, String username) {
        return ApiService.postMappingSet(apikey, username, mappingSet);
    }

    public static boolean isImportedClass(OWLOntology ontology, OWLClass cls) {
        for (OWLOntology importedOntology : ontology.getImports()) {
            if (importedOntology.containsClassInSignature(cls.getIRI())) {
                return true;
            }
        }
        return false;
    }

    public JSONObject extractLinkedDataMappings(OWLOntology sourceOntology, String acronym, String apikey, String username) {
        ExtractHelper.logger.log(Level.INFO, "=====starting parsing ontology " + acronym + " to get sssom/linked data mappings");

        JSONArray results = new JSONArray();

        JSONArray sssomResults = new JSONArray();

        Long interMappingSetId = ApiService.postMappingSet(apikey, username, getInterMappingSet(sourceOntology, acronym));

        Long crossMappingSetId = ApiService.postMappingSet(apikey, username, getCrossMappingSet(sourceOntology, acronym));

        for (OWLClass cls : sourceOntology.getClassesInSignature()) {
            for (OWLSubClassOfAxiom ax : sourceOntology.getSubClassAxiomsForSubClass(cls)) {
                if (ax.getSuperClass().isNamed() && ax.getSubClass().asOWLClass().getIRI().equals(cls.getIRI())) {

                    OWLClass mappedClass;
                    try {
                        mappedClass = ax.getSuperClass().asOWLClass();
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (sourceOntology.containsClassInSignature(mappedClass.getIRI()) && mappedClass.getIRI().toString().contains(getOntologyUri(sourceOntology, acronym))) {ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is sub class  to: " + mappedClass.getIRI().toString() + " (intra Mapping)");
                    } else if (isImportedClass(sourceOntology, mappedClass)) {
                        // Mapped class belongs to an imported ontology
                        ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is sub class to: " + mappedClass.getIRI().toString() + " (Inter Mapping)");
                        results.put(createMappingElement(sourceOntology, acronym, cls.getIRI().toString(),
                                mappedClass.getIRI().toString(), MappingType.SUBCLASS_OF.toString(),
                                acronym + "#inter"));
                        sssomResults.put(createSSSOMMappingElement(sourceOntology, acronym, acronym + "#inter",
                                cls.getIRI().toString(), mappedClass.getIRI().toString(), MappingType.SUBCLASS_OF.toString(), sourceOntology.getImports(), interMappingSetId.toString()));
                    } else {
                        // Mapped class is from an external ontology
                        ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is sub class to: " + mappedClass.getIRI().toString() + " (Cross Mapping)");
                        results.put(createMappingElement(sourceOntology, acronym, cls.getIRI().toString(),
                                mappedClass.getIRI().toString(), MappingType.SUBCLASS_OF.toString(),
                                acronym + "#cross"));
                        sssomResults.put(createSSSOMMappingElement(sourceOntology, acronym, acronym + "#cross",
                                cls.getIRI().toString(), mappedClass.getIRI().toString(), MappingType.SUBCLASS_OF.toString(), sourceOntology.getImports(), crossMappingSetId.toString()));


                    }
                }
            }

            for (OWLEquivalentClassesAxiom ax : sourceOntology.getEquivalentClassesAxioms(cls)) {
                for (OWLClassExpression equiv : ax.getClassExpressionsMinus(cls)) {
                    // owl:equivalentClass
                    if (equiv.isClassExpressionLiteral()) {
                        OWLClass mappedClass;

                        try {
                            mappedClass = equiv.asOWLClass();
                        } catch (ClassCastException e) {
                            e.printStackTrace();
                            continue;
                        }
                        if (sourceOntology.containsClassInSignature(mappedClass.getIRI()) && mappedClass.getIRI().toString().contains(getOntologyUri(sourceOntology, acronym))) {
                            // Mapped class is a class in the ontology itself
                            ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is equivalent to: " + mappedClass.getIRI().toString() + " (Intra Mapping)");
                        } else if (isImportedClass(sourceOntology, mappedClass)) {
                            // Mapped class belongs to an imported ontology
                            ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is equivalent to: " + mappedClass.getIRI().toString() + " (Inter Mapping)");
                            results.put(createMappingElement(sourceOntology, acronym, cls.getIRI().toString(),
                                    mappedClass.getIRI().toString(), MappingType.EQUIVALENT_CLASSES.toString(),
                                    acronym + "#inter"));
                            sssomResults.put(createSSSOMMappingElement(sourceOntology, acronym, acronym + "#inter",
                                    cls.getIRI().toString(), mappedClass.getIRI().toString(), MappingType.EQUIVALENT_CLASSES.toString(), sourceOntology.getImports(), interMappingSetId.toString()));

                        } else {
                            // Mapped class is from an external ontology
                            ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is equivalent to: " + mappedClass.getIRI().toString() + " (Cross Mapping)");
                            results.put(createMappingElement(sourceOntology, acronym, cls.getIRI().toString(),
                                    mappedClass.getIRI().toString(), MappingType.EQUIVALENT_CLASSES.toString(),
                                    acronym + "#cross"));
                            sssomResults.put(createSSSOMMappingElement(sourceOntology, acronym, acronym + "#cross",
                                    cls.getIRI().toString(), mappedClass.getIRI().toString(), MappingType.EQUIVALENT_CLASSES.toString(), sourceOntology.getImports(), interMappingSetId.toString()));

                        }
                    }
                }
            }

//            for (OWLClassAssertionAxiom ax : sourceOntology.getClassAssertionAxioms(cls)) {
//                if (ax.getClassExpression().isObjectOneOf()) {
//                    // owl:oneOf
//                    OWLObjectOneOf oneOf = ax.getClassExpression().asObjectOneOf();
//                    for (OWLIndividual ind : oneOf.getIndividuals()) {
//                        ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is mapped to individual: " + ind.toString());
//                    }
//                }
//            }

            for (OWLDisjointClassesAxiom ax : sourceOntology.getDisjointClassesAxioms(cls)) {
                for (OWLClassExpression disjoint : ax.getClassExpressionsMinus(cls)) {
                    // owl:disjointWith
                    if (disjoint.isClassExpressionLiteral()) {
                        OWLClass mappedClass;
                        try {
                            mappedClass = disjoint.asOWLClass();
                        } catch (ClassCastException e) {
                            e.printStackTrace();
                            continue;
                        }
                        if (sourceOntology.containsClassInSignature(mappedClass.getIRI()) && mappedClass.getIRI().toString().contains(getOntologyUri(sourceOntology, acronym))) {
                            // Mapped class is a class in the ontology itself
                            ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is disjoint with: " + mappedClass.getIRI().toString() + " (Intra Mapping)");
                        } else if (isImportedClass(sourceOntology, mappedClass)) {
                            // Mapped class belongs to an imported ontology
                            ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is disjoint with: " + mappedClass.getIRI().toString() + " (Inter Mapping)");
                            results.put(createMappingElement(sourceOntology, acronym, cls.getIRI().toString(),
                                    mappedClass.getIRI().toString(), MappingType.DISJOINT_CLASSES.toString(),
                                    acronym + "#inter"));
                            sssomResults.put(createSSSOMMappingElement(sourceOntology, acronym, acronym + "#inter",
                                    cls.getIRI().toString(), mappedClass.getIRI().toString(), MappingType.DISJOINT_CLASSES.toString(), sourceOntology.getImports(), interMappingSetId.toString()));


                        } else {
                            // Mapped class is from an external ontology
                            ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is disjoint with: " + mappedClass.getIRI().toString() + " (Cross Mapping)");
                            results.put(createMappingElement(sourceOntology, acronym, cls.getIRI().toString(),
                                    mappedClass.getIRI().toString(), MappingType.DISJOINT_CLASSES.toString(),
                                    acronym + "#cross"));
                            sssomResults.put(createSSSOMMappingElement(sourceOntology, acronym, acronym + "#cross",
                                    cls.getIRI().toString(), mappedClass.getIRI().toString(), MappingType.DISJOINT_CLASSES.toString(), sourceOntology.getImports(), interMappingSetId.toString()));

                        }
                    }
                }
            }
            for (OWLDisjointUnionAxiom ax : sourceOntology.getDisjointUnionAxioms(cls)) {
                OWLClass mappedClass;
                try {
                    mappedClass = ax.getOWLClass();
                } catch (ClassCastException e) {
                    e.printStackTrace();
                    continue;
                }
                if (sourceOntology.containsClassInSignature(mappedClass.getIRI()) && mappedClass.getIRI().toString().contains(getOntologyUri(sourceOntology, acronym))) {
                    // Mapped class is a class in the ontology itself
                    //  ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is disjoint union with: " + mappedClass.getIRI().toString() + " (Intra Mapping)");

                } else if (isImportedClass(sourceOntology, mappedClass)) {
                    // Mapped class belongs to an imported ontology
                    ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is disjoint union with: " + mappedClass.getIRI().toString() + " (Inter Mapping)");
                    results.put(createMappingElement(sourceOntology, acronym, cls.getIRI().toString(),
                            mappedClass.getIRI().toString(), MappingType.DISJOINT_UNION.toString(),
                            acronym + "#inter"));
                    sssomResults.put(createSSSOMMappingElement(sourceOntology, acronym, acronym + "#inter",
                            cls.getIRI().toString(), mappedClass.getIRI().toString(), MappingType.DISJOINT_UNION.toString(), sourceOntology.getImports(), interMappingSetId.toString()));

                } else {
                    // Mapped class is from an external ontology
                    ExtractHelper.logger.log(Level.INFO,"Class: " + cls.getIRI().toString() + " is disjoint union with: " + mappedClass.getIRI().toString() + " (Cross Mapping)");
                    results.put(createMappingElement(sourceOntology, acronym, cls.getIRI().toString(),
                            mappedClass.getIRI().toString(), MappingType.DISJOINT_UNION.toString(),
                            acronym + "#cross"));
                    sssomResults.put(createSSSOMMappingElement(sourceOntology, acronym, acronym + "#cross",
                            cls.getIRI().toString(), mappedClass.getIRI().toString(), MappingType.DISJOINT_UNION.toString(), sourceOntology.getImports(), interMappingSetId.toString()));

                }
            }
        }


        JSONObject finalRes = new JSONObject();

        finalRes.put("linked_data", results);
        finalRes.put("sssom", sssomResults);

        return finalRes;


//
//        for (OWLAxiom axiom : sourceOntology.getAxioms()) {
//
//            for (OWLClass cls:
//                 axiom.getClassesInSignature()) {
//                if (cls.getIRI().toString().equals("http://purl.obolibrary.org/obo/DOID_8986")) {
//                    int b = 5;
//                }
//
//            }
//
//            if (!MappingType.isMappingType(axiom.getAxiomType())) continue;
//
//
//            //get first and second class from axiom
//            OWLClass sourceCLass = null;
//            OWLClass targetCLass = null;
//
//            Iterator<OWLClass> classIterator = axiom.getClassesInSignature().iterator();
//            if (classIterator.hasNext()) {
//                sourceCLass = classIterator.next();
//            }
//            if (classIterator.hasNext()) {
//                targetCLass = classIterator.next();
//            }
//
//                if (isMappingToOtherOntology(sourceCLass, targetCLass,  sourceOntology).equals(MappingSetType.CROSS)) {
//                    JSONObject tripleJSON = new JSONObject();
//
//                    String subjectUri = sourceCLass.getIRI().toString();
//                    String objectUri = targetCLass.getIRI().toString();
//                    String predicateUri = String.valueOf(axiom.getAxiomType());
//
//                    tripleJSON.put("other", "cross");
//                    tripleJSON.put("mapping_id", subjectUri+ "_" + predicateUri+ "_" +objectUri );
//                    //tripleJSON.put("mapping_provider", extractHelper.getMappingProvider(intraMapping, interMapping, crossMapping, tripleJSON.getString("other")) );
//                    //tripleJSON.put("mapping_tool", extractHelper.getMappingTool(comment) );
//                    //tripleJSON.put("mapping_tool_version", "");
//                    //tripleJSON.put("match_string", extractHelper.getMatchString(comment) );
//                    //tripleJSON.put("source", "" );
//                    tripleJSON.put("subject_id",  subjectUri);
//                    //tripleJSON.put("subject_label",  );
//                    //tripleJSON.put("subject_type", extractHelper.getType(subject));
//                    //tripleJSON.put("subject_category", extractHelper.getCategory(subject));
//                    tripleJSON.put("subject_source", sourceOntology.getOntologyID().getOntologyIRI().get()  );
//                    tripleJSON.put("subject_source_version", sourceOntology.getOntologyID().getVersionIRI() );
//                    //tripleJSON.put( "subject_match_field", extractHelper.getMatchField(comment) );
//                    //tripleJSON.put("subject_preprocessing", extractHelper.getPreProcessing() );
////                    tripleJSON.put("predicate_id", extractHelper.getPredicateIdentifier(predicateUri));
////                    tripleJSON.put("predicate_label", extractHelper.getLabel(predicate) );
////                    tripleJSON.put("predicate_Modifier", extractHelper.getPredicateModifier(predicateUri) );
////                    tripleJSON.put("object_id",  objectUri);
////                    tripleJSON.put("object_label", objectLabel);
////                    tripleJSON.put("object_type", objectType);
////                    tripleJSON.put("object_category", objectCategory);
////                    tripleJSON.put("object_source", extractHelper.getOntologySource(objectUri));
////                    tripleJSON.put("object_source_version", ontologyVersion );
////                    tripleJSON.put("object_match_field", extractHelper.getMatchField(comment) );
////                    tripleJSON.put("object_preprocessing", extractHelper.getPreProcessing() );
////                    tripleJSON.put("justification", extractHelper.getMappingsJustification(subject, s.getObject(), predicate , comment) );
////                    tripleJSON.put("semantic_similarity_measure", extractHelper.getSemanticSimilarity(comment));
////                    tripleJSON.put("semantic_similarity_score", extractHelper.getSemanticSimilarityScore(comment));
////                    tripleJSON.put("confidence", extractHelper.getConfidence(comment) );
////                    tripleJSON.put("cardinality", "MANY_TO_MANY");
////                    tripleJSON.put("contriutors", new JSONArray());
////                    tripleJSON.put("license", "");
////                    tripleJSON.put("mapping_date", extractHelper.getDate());
////                    tripleJSON.put("comment", comment );
//
//                    int b = 5;
//
//            }
//        }
//
//        return crossMappings;
    }

    private JSONObject createMappingElement(OWLOntology sourceOntology, String acronym, String subjectUri, String objectUri, String predicateUri, String mappingSetId) {
        JSONObject tripleJSON = new JSONObject();
        tripleJSON.put("name", "mapping between " + subjectUri + " and " + objectUri);
        tripleJSON.put("creator", "ontomapper");
        tripleJSON.put("comment", "");
        tripleJSON.put("source_contact_info", "ontomapper@nodomain.org");
        tripleJSON.put("source_name", mappingSetId);
        tripleJSON.put("relation", predicateUri);
        tripleJSON.put("source_id", subjectUri);
        tripleJSON.put("target_id", objectUri);
        tripleJSON.put("external_mapping", true);
        tripleJSON.put("source_ontology_id", getOntologyUri(sourceOntology, acronym));
        tripleJSON.put("source_ontology_acronym", acronym);
        tripleJSON.put("target_ontology_id", getTargetOntologyId(sourceOntology, objectUri));
        tripleJSON.put("target_ontology_acronym", getTargetOntologyAcronym(sourceOntology, objectUri));

        return tripleJSON;

    }

    private JSONObject createSSSOMMappingElement(OWLOntology ontology, String acronym, String mappingType, String subjectUri, String objectUri, String predicateUri, Set<OWLOntology> imports, String mappingSetId) {
        JSONObject tripleJSON = new JSONObject();
        tripleJSON.put("other", getMappingType(subjectUri, objectUri, imports));
        tripleJSON.put("mapping_id", subjectUri + "_" + predicateUri + "_" + objectUri);
        tripleJSON.put("mapping_provider", "");
        tripleJSON.put("mapping_tool", "Ontomapper");
        tripleJSON.put("mapping_tool_version", "");
        tripleJSON.put("match_string", "");
        tripleJSON.put("subject_id", subjectUri);
        tripleJSON.put("subject_label", getClassLabel(subjectUri, ontology));
        tripleJSON.put("subject_type", "OWL_CLASS");
        tripleJSON.put("subject_category", "");
        tripleJSON.put("subject_source", getOntologyUri(ontology, acronym));
        tripleJSON.put("subject_source_version", getOntologyVersion(ontology));
        tripleJSON.put("subject_match_field", "");
        tripleJSON.put("subject_preprocessing", "");
        tripleJSON.put("predicate_id", predicateUri);
        tripleJSON.put("predicate_label", "");
        tripleJSON.put("predicate_modifier", "EMPTY");
        tripleJSON.put("object_id", objectUri);
        tripleJSON.put("object_label", getClassLabel(objectUri, ontology));
        tripleJSON.put("object_type", "OWL_CLASS");
        tripleJSON.put("object_category", "");
        tripleJSON.put("object_source", getTargetOntologyAcronym(ontology, acronym));
        tripleJSON.put("object_source_version", getTargetOntologyVersion(ontology, objectUri));
        tripleJSON.put("object_match_field", "");
        tripleJSON.put("object_preprocessing", "");
        tripleJSON.put("justification", "");
        tripleJSON.put("semantic_similarity_measure", "");
        tripleJSON.put("semantic_similarity_score", "");
        tripleJSON.put("confidence", "1");
        tripleJSON.put("cardinality", "MANY_TO_MANY");
        tripleJSON.put("contributors", new JSONArray());
        tripleJSON.put("license", "");
        tripleJSON.put("mapping_date", getDate());
        tripleJSON.put("comment", "http://industryportal.enit.fr/sssom#automatic");
        tripleJSON.put("set_id", mappingSetId);

        return tripleJSON;
    }

    public JSONArray convertSSSOMToLinkedDataMappings(List<SetRequest> sssomMappings) {
        JSONArray linkedDataMappings = new JSONArray();
        for (int i = 0; i <= sssomMappings.size() - 1; i++) {
            SetRequest obj = sssomMappings.get(i);
            List<MappingRequest> mappings = obj.getMappings();
            for (int j = 0; i <= mappings.size() - 1; j++ ) {

                MappingRequest mapping = mappings.get(j);

                JSONObject jsonMapping = new JSONObject();
                jsonMapping.put("name", "mapping between " + mapping.getSubject_id() + " and " + mapping.getObject_id());
                jsonMapping.put("creator", "");
                jsonMapping.put("comment", mapping.getComment());
                jsonMapping.put("source_contact_info", "ontomapper@nodomain.org");
                jsonMapping.put("source_name", mapping.getMapping_id());
                jsonMapping.put("relation", mapping.getPredicate_id());
                jsonMapping.put("source_id", mapping.getSubject_id());
                jsonMapping.put("target_id", mapping.getObject_id());
                jsonMapping.put("external_mapping", true);
                jsonMapping.put("source_ontology_id", mapping.getSubject_source());
                jsonMapping.put("source_ontology_acronym", mapping.getSubject_source().substring(
                        mapping.getSubject_source().lastIndexOf("/") + 1));
                jsonMapping.put("target_ontology_id", mapping.getObject_source());
                jsonMapping.put("target_ontology_acronym", mapping.getObject_source().substring(
                        mapping.getObject_source().lastIndexOf("/") + 1));

                linkedDataMappings.put(jsonMapping);

            }
        }

        return linkedDataMappings;

    }

    private String getTargetOntologyAcronym(OWLOntology ontology, String clsUri) {
        return "";
    }

    private String getTargetOntologyVersion(OWLOntology sourceOntology, String objectUri) {
        for (OWLOntology importedOntology : sourceOntology.getImports()) {
            IRI clsIRI = IRI.create(objectUri);

            if (importedOntology.containsClassInSignature(clsIRI)) {
                OWLOntologyID ontologyID = importedOntology.getOntologyID();

                // Check if there is an ontology ID and a version IRI
                if (ontologyID != null && ontologyID.getVersionIRI().isPresent()) {
                    return ontologyID.getVersionIRI().get().toString();
                } else {
                    // Handle the case where no version IRI is present
                    return "No version IRI found";
                }
            }
        }

        return "";

    }

    private String getTargetOntologyId(OWLOntology ontology, String clsUri) {
        //looks in the imported ontology ,if it finds the cls belongs to one of them if returns that imported
        // ontology id, if it doesn't find, it returns sub-uri of the class
        for (OWLOntology importedOntology : ontology.getImports()) {
            IRI clsIRI = IRI.create(clsUri);

            if (importedOntology.containsClassInSignature(clsIRI)) {
                return importedOntology.getOntologyID().getOntologyIRI().get().toString();
            }
        }

        // Return the class URI without its name


        int indexSquare = clsUri.lastIndexOf("#");
        int indexSlash = clsUri.lastIndexOf("/");
        if (indexSquare != -1) {
            return clsUri.substring(0, indexSquare);
        } else if (indexSlash != -1) {
            return clsUri.substring(0, indexSlash + 1);
        } else {
            return "";
        }
    }

    private String getClassType(String subjectUri, OWLOntology sourceOntology) {
        return "OWL_CLASS";
    }


    private boolean checkIfInSignature(OWLClass cls, OWLOntology ont) {

        return ont.getClassesInSignature().contains(cls);
    }

    public int storeLinkedDataMappings(LinkedDataMappingRepository repo, JSONArray mappings) {
        for (int i = 0; i <= mappings.length() - 1; i++) {
            JSONObject obj = mappings.getJSONObject(i);

            LinkedDataMappingRequest mapping = new LinkedDataMappingRequest();

            mapping.setName(obj.getString("name"));
            mapping.setCreator(obj.getString("creator"));
            mapping.setComment(obj.getString("comment"));
            mapping.setSourceContactInfo(obj.getString("source_contact_info"));
            mapping.setSourceName(obj.getString("source_name"));
            mapping.setRelation(Collections.singletonList(obj.getString("relation")));
            mapping.setSourceId(obj.getString("source_id"));
            mapping.setTargetId(obj.getString("target_id"));
            mapping.setExternalMapping(obj.getBoolean("external_mapping"));
            mapping.setSourceOntologyId(obj.getString("source_ontology_id"));
            mapping.setSourceOntologyAcronym(obj.getString("source_ontology_acronym"));
            mapping.setTargetOntologyId(obj.getString("target_ontology_id"));
            mapping.setTargetOntologyAcronym(obj.getString("target_ontology_acronym"));


            repo.save(mapping.toDbModel());

        }
        return 1;
    }

    public void storeSSSOMMappings(MappingRepository mappingRepository, MappingSetRepository mappingSetRepository, JSONArray sssomRes) {
        sssomRes.forEach(mapping -> {
            try {
                MappingRequest request = MappingRequest.fromJSON(mapping.toString());
                Mapping m = request.toDBModel(mappingSetRepository, "ontomapper");
                // inserting or updating the mapping
                mappingRepository.save(m);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}

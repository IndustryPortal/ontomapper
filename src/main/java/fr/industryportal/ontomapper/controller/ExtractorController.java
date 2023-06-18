package fr.industryportal.ontomapper.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.industryportal.ontomapper.config.Config;
import fr.industryportal.ontomapper.model.entities.ManchesterMapping;
import fr.industryportal.ontomapper.model.repos.*;
import fr.industryportal.ontomapper.model.requests.ManchesterMappingRequest;
import fr.industryportal.ontomapper.model.requests.User;
import fr.industryportal.ontomapper.services.ApiService;
import org.apache.http.HttpException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import uk.ac.manchester.cs.owl.owlapi.*;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/extractor")
public class ExtractorController {

    @Autowired
    private MappingSetRepository mappingSetRepository;

    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private ContributorRepository contributorRepository;

    @Autowired
    private ContributionRepository contributionRepository;

    @Autowired
    private ManchesterMappingRepository manchesterMappingRepository;

    private static final Pattern RELATION_PATTERN = Pattern.compile("([!\\^\\+]?)(.+)");

    @GetMapping("")
    public String extractTriplesWithJena(HttpServletRequest request, @RequestParam String acronym) throws OWLOntologyCreationException, IOException {
        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();
        String filePath = downloadOntologyFile(apikey, acronym);
        //String filePath = Config.ONTOLOGY_FOLDER + "Core.rdf";
        if (filePath == null ) {
            JSONObject o = new JSONObject();
            o.put("message", "error in getting ontology source file from portal");
            return o.toString();
        }
        //String filePath = Config.ONTOLOGY_FOLDER + "ROMAIN.owl";
        String ontologyUri = getOntologySource(new File(filePath));
        ArrayList<Triple> result = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        Model m = ModelFactory.createDefaultModel();
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        try {
            m.read(new FileInputStream(new File(filePath)), null, "RDF/XML");
                ontModel.read(new File(filePath).toURI().toString(), "RDF/XML");
            // m.write(System.out, "TTL");
            System.out.println("=========================================");
            StmtIterator siter = m.listStatements();
//            FileOutputStream fo = new FileOutputStream(new File(acronym + ".txt"));
            String ontologyVersion = getOntologyVersion(filePath);

            System.out.println("ontology version: " + ontologyVersion );
            Set<OWLImportsDeclaration> imports = getImports(filePath);

            JSONObject intraMapping = getIntraMappingSet(acronym, m, m.listStatements(), ontologyVersion);
            JSONArray intraMappings = new JSONArray();

            JSONObject interMapping = geInterMappingSet(acronym, m, m.listStatements(), ontologyVersion);
            JSONArray interMappings = new JSONArray();

            JSONObject crossMapping = getCrossMappingSet(acronym, m, m.listStatements(), ontologyVersion);
            JSONArray crossMappings = new JSONArray();

            Long intraMappingSetId = ApiService.postMappingSet(apikey, username, intraMapping);

            Long interMappingSetId = ApiService.postMappingSet(apikey, username, interMapping);

            Long crossMappingSetId = ApiService.postMappingSet(apikey, username, crossMapping);



            while (siter.hasNext()) {
                Statement s = siter.next();


                if (!isLogicalAxiom(s)) continue;

                String comment = getComment(s);

                Resource subject = s.getSubject();
                Property predicate = s.getPredicate();
//                Resource object = s.getObject().asResource();
                String objectCategory = "";
                String objectType = "";
                String objectLabel= "";
                if (s.getObject().isLiteral()) {
                    Literal object = s.getObject().asLiteral();
                    objectType = object.getDatatypeURI();
                    if (objectType.equals("http://www.w3.org/2001/XMLSchema#nonNegativeInteger")) {
                        objectType = "xsd:nonNegativeInteger";
                        objectCategory = "rdfs:Datatype";
                    }
                    objectLabel = object.getLexicalForm();
                    // do something with the literal object
                } else {
                    Resource object = s.getObject().asResource();
                    objectCategory = getCategory(object);
                    objectType = getType(object);
                    objectLabel = getLabel(object);
                    // do something with the resource object
                }

                String subjectUri = s.getSubject().toString();
                String predicateUri = s.getPredicate().toString();
                String objectUri = s.getObject().toString();

                JSONObject tripleJSON = new JSONObject();
                tripleJSON.put("other", getMappingType(subjectUri, objectUri, imports));
                tripleJSON.put("mapping_id", subjectUri+ "_" + predicateUri+ "_" +objectUri );
                tripleJSON.put("mapping_provider", getMappingProvider(intraMapping, interMapping, crossMapping, tripleJSON.getString("other")) );
                tripleJSON.put("mapping_tool", getMappingTool(comment) );
                tripleJSON.put("mapping_tool_version", "");
                tripleJSON.put("match_string", getMatchString(comment) );
                tripleJSON.put("source", "" );
                tripleJSON.put("subject_id",  subjectUri);
                tripleJSON.put("subject_label", getLabel(subject) );
                tripleJSON.put("subject_type", getType(subject));
                tripleJSON.put("subject_category", getCategory(subject));
                tripleJSON.put("subject_source", getOntologySource(subjectUri) );
                tripleJSON.put("subject_source_version", ontologyVersion );
                tripleJSON.put( "subject_match_field", getMatchField(comment) );
                tripleJSON.put("subject_preprocessing", getPreProcessing() );
                tripleJSON.put("predicate_id", getPredicateIdentifier(predicateUri));
                tripleJSON.put("predicate_label", getLabel(predicate) );
                tripleJSON.put("predicate_Modifier", getPredicateModifier(predicateUri) );
                tripleJSON.put("object_id",  objectUri);
                tripleJSON.put("object_label", objectLabel);
                tripleJSON.put("object_type", objectType);
                tripleJSON.put("object_category", objectCategory);
                tripleJSON.put("object_source", getOntologySource(objectUri));
                tripleJSON.put("object_source_version", ontologyVersion );
                tripleJSON.put("object_match_field", getMatchField(comment) );
                tripleJSON.put("object_preprocessing", getPreProcessing() );
                tripleJSON.put("justification", getMappingsJustification(subject, s.getObject(), predicate , comment) );
                tripleJSON.put("semantic_similarity_measure", getSemanticSimilarity(comment));
                tripleJSON.put("semantic_similarity_score", getSemanticSimilarityScore(comment));
                tripleJSON.put("confidence", getConfidence(comment) );
                tripleJSON.put("cardinality", "MANY_TO_MANY");
                tripleJSON.put("contriutors", new JSONArray());
                tripleJSON.put("license", "");
                tripleJSON.put("mapping_date", getDate());
                tripleJSON.put("comment", comment );

                jsonArray.put(tripleJSON);

                switch (tripleJSON.getString("other")) {
                    case "http://industryportal.enit.fr/sssom#intra" :
                        tripleJSON.put("set_id", intraMappingSetId);
                        intraMappings.put(tripleJSON);
                        break;

                    case "http://industryportal.enit.fr/sssom#inter" :
                        tripleJSON.put("set_id", interMappingSetId);
                        interMappings.put(tripleJSON);
                     break;
                    case "http://industryportal.enit.fr/sssom#cross" :
                        tripleJSON.put("set_id", crossMappingSetId);
                        crossMappings.put(tripleJSON);
                    break;
                }

                // System.out.println(s.getSubject().toString() + "\t" +
                //                                 s.getPredicate().toString() + "\t" +
                //                                 s.getObject().toString());
//                fo.write(new String(subjectUri + " " +
//                        predicateUri + " " +
//                        objectUri + "\n").getBytes());

                Node subjectNode = NodeFactory.createURI(subjectUri);
                Node predicateNode = NodeFactory.createURI(predicateUri);
                Node objectNode = NodeFactory.createURI(objectUri);

                result.add(new Triple(subjectNode, predicateNode, objectNode));


            }


//            fo.flush();
//            fo.close();
//            System.out.println("result is written to file:" + acronym + ".txt");


            System.out.println("posting extracted mappings to api");
            ApiService.postMappings(apikey, username, intraMappings);
            ApiService.postMappings(apikey, username, interMappings);
            ApiService.postMappings(apikey, username, crossMappings);

            intraMapping.put("mappings", intraMappings );
            interMapping.put("mappings", interMappings);
            crossMapping.put("mappings", crossMappings);

            JSONObject finalJson = new JSONObject();

            finalJson.put("intra", intraMapping);
            finalJson.put("inter", interMapping);
            finalJson.put("cross", crossMapping);

//            FileWriter fileWriter = new FileWriter("jsonOutput.json");
//
//            // Write JSON array to file
//            fileWriter.write(finalJson.toString(4));
//            fileWriter.flush();
//            fileWriter.close();

            JSONObject o = new JSONObject();
            o.put("message", "mappings extracted successfully, try now getting /set, or /mapping to set the result");
            o.put("intra_set_id", intraMappingSetId );
            o.put("inter_set_id", interMappingSetId );
            o.put("cross_set_id", crossMappingSetId );
            return o.toString();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            JSONObject o = new JSONObject();
            o.put("message", "error occured, try again");
            o.put("error", e.getMessage());
            return o.toString();
        }

        // System.out.println(cleanJsonArray(jsonArray).toString());
        // System.out.println(jsonArray.toString());

    }

    @GetMapping("/{acronym}/mappingsByClass")
    public String getMappingsByClassUri(HttpServletRequest request, @PathVariable String acronym, @RequestParam String classUri) {
        classUri = classUri.trim();
        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();
        String filePath = downloadOntologyFile(apikey, acronym);
        //String filePath = Config.ONTOLOGY_FOLDER + "go.owl";
        if (filePath == null ) {
            JSONObject o = new JSONObject();
            o.put("message", "error in getting ontology source file from portal");
            return o.toString();
        }
        OntologyConfigurator configurator = new OntologyConfigurator();
        configurator.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        manager.setOntologyConfigurator(configurator);
        OWLOntology ontology = null;

        JSONArray result = new JSONArray();
        try {
            ontology = manager.loadOntologyFromOntologyDocument(new File(filePath));

            for (OWLAxiom axiom : ontology.getLogicalAxioms()) {

//                logical axioms:
//                SubClassOf.
//                EquivalentClasses.
//                DisjointClasses.
//                DisjointUnion
//                SubObjectPropertyOf
//                EquivalentObjectProperties
//                InverseObjectProperties
//                FunctionalObjectProperty
//                InverseFunctionalObjectProperty
//                ObjectPropertyDomain
//                ObjectPropertyRange
//                SubDataPropertyOf
//                EquivalentDataProperties
//                DataPropertyDomain
//                DataPropertyRange

                if (axiom instanceof OWLSubClassOfAxiomImpl ) {

                    OWLClassExpression ce = ((OWLSubClassOfAxiomImpl) axiom).getSubClass();

                    for (OWLClass c : ce.getClassesInSignature()) {
                        if (c.getIRI().toString().equals(classUri)) {
                            JSONObject obj = new JSONObject();
                            String axiomType = axiom.getAxiomType().getName();
                            String axiomManchesterSyntax = convertToManchesterSyntax(axiom, classUri, axiomType);
                            if (axiomManchesterSyntax.equals("")) continue;
                            obj.put("classUri", classUri );
                            obj.put("ontologyAcronym", acronym);
                            obj.put("type", axiomType);
                            String formattedBody = replaceClassURIsWithLabels(axiomManchesterSyntax, ontology, axiom);
                            obj.put("body", formattedBody);
                            result.put(obj );
                            break;
                        }
                    }
                }
                else if (axiom instanceof OWLEquivalentClassesAxiomImpl) {
                    Set<OWLClassExpression> ce = ((OWLEquivalentClassesAxiomImpl) axiom).getClassExpressions();
                    for (OWLClass c : ce.iterator().next().getClassesInSignature() ) {
                        if (c.getIRI().toString().equals(classUri)) {
                            JSONObject obj = new JSONObject();
                            String axiomType = axiom.getAxiomType().getName();
                            String axiomManchesterSyntax = convertToManchesterSyntax(axiom, classUri, axiomType);
                            if (axiomManchesterSyntax.equals("")) continue;
                            obj.put("classUri", classUri );
                            obj.put("ontologyAcronym", acronym);
                            obj.put("type", axiomType);
                            String formattedBody = replaceClassURIsWithLabels(axiomManchesterSyntax, ontology, axiom);
                            obj.put("body", formattedBody);
                            result.put(obj );
                            break;
                        }
                    }
                }
                else if (axiom instanceof OWLDisjointClassesAxiomImpl) {
                    Set<OWLClassExpression> ce = ((OWLDisjointClassesAxiomImpl) axiom).getClassExpressions();
                    for (OWLClass c : ce.iterator().next().getClassesInSignature() ) {
                        if (c.getIRI().toString().equals(classUri)) {
                            JSONObject obj = new JSONObject();
                            String axiomType = axiom.getAxiomType().getName();
                            String axiomManchesterSyntax = convertToManchesterSyntax(axiom, classUri, axiomType);
                            if (axiomManchesterSyntax.equals("")) continue;
                            obj.put("classUri", classUri );
                            obj.put("ontologyAcronym", acronym);
                            obj.put("type", axiomType);
                            String formattedBody = replaceClassURIsWithLabels(axiomManchesterSyntax, ontology, axiom);
                            obj.put("body", formattedBody);
                            result.put(obj );
                            break;
                        }
                    }
                }
                else if (axiom instanceof OWLDisjointUnionAxiomImpl) {
                    if (((OWLDisjointUnionAxiomImpl) axiom).getOWLClass().getIRI().toString().equals(classUri)) {
                        JSONObject obj = new JSONObject();
                        String axiomType = axiom.getAxiomType().getName();
                        String axiomManchesterSyntax = convertToManchesterSyntax(axiom, classUri, axiomType);
                        if (axiomManchesterSyntax.equals("")) continue;
                        obj.put("classUri", classUri );
                        obj.put("ontologyAcronym", acronym);
                        obj.put("type", axiomType);
                        String formattedBody = replaceClassURIsWithLabels(axiomManchesterSyntax, ontology, axiom);
                        obj.put("body", formattedBody);
                        result.put(obj );
                    }
                }

//                Set<OWLClass> classes = axiom.getClassesInSignature();
//                for (OWLClass c : classes) {
//                    if (c.getIRI().toString().equals(classUri)) {
//                        JSONObject obj = new JSONObject();
//                        String axiomManchesterSyntax = convertToManchesterSyntax(axiom, classUri);
//                        if (axiomManchesterSyntax.equals("")) continue;
//                        obj.put("type", axiom.getAxiomType().getName());
//                        obj.put("body", axiomManchesterSyntax);
//                        result.put(obj );
//                    }
//                }
            }

        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        //saving mapping before returning them
        for (int i = 0; i <= result.length()-1; i++ ) {
            JSONObject obj = result.getJSONObject(i);

            ManchesterMappingRequest mapping = new ManchesterMappingRequest();

            mapping.setClassUri(obj.getString("classUri"));
            mapping.setOntologyAcronym(obj.getString("ontologyAcronym"));
            mapping.setType(obj.getString("type"));
            mapping.setBody(obj.getString("body"));



            manchesterMappingRepository.save(mapping.toDbModel());

        }

        return  result.toString();
    }

//    private String formatResponseUrls( OWLOntology ontology, String response) {
//
//        JsonArray result = new JsonArray();
//
//        JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
//
//        for (int i = 0; i < jsonArray.size(); i++) {
//            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
//
//            // Extract type and body from JSON object
//            String type = jsonObject.get("type").getAsString();
//            String body = jsonObject.get("body").getAsString();
//
//            // Replace class URIs with labels
//            String replacedBody = replaceClassURIsWithLabels(body, ontology);
//
//            jsonObject.addProperty("body", replacedBody);
//
//            result.add(jsonObject);
//
//        }
//
//            return result.toString();
//
//        }

    private static String replaceClassURIsWithLabels(String ontologyString, OWLOntology ontology,  OWLAxiom axiom) {
        String replacedString = ontologyString;

        // Get all class axioms from the ontology
        for (OWLEntity cls : axiom.getSignature()) {
            // Get the class URI and label
            String classURI = cls.getIRI().toString();
            String label = getClassLabel(classURI, ontology);

            //check if classUri is in replacedString first
            if (!replacedString.contains(classURI)) continue;

            // Replace class URI with label(uri) in the string
            replacedString = replacedString.replace( classURI, "!!!" + label + "(" + classURI + ")");
        }

        return replacedString.replace("<", "").replace(">", "");
    }

    private static String getClassLabel(String classUri, OWLOntology ontology) {
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
        OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
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
        }

        return cls.getIRI().getFragment(); // Use the class fragment (the last part of the iri) as the label if no rdfs:label found

    }

    private  String convertToManchesterSyntax(OWLAxiom axiom, String classUri, String axiomType) {
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

    private String extractAxiomString(String serializedOntology, String classUri, String axiomType) {
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
        }else if (axiomType.equals("DisjointClasses")) {
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
        }else if (axiomType.equals("SubClassOf")) {
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
        }else if (axiomType.equals("EquivalentClasses")) {
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
        }else {
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
            fileName = lastModified + ' ' + fileName.substring(0, fileName.length() - 1)  ;

            File file = new File(Config.ONTOLOGY_FOLDER, fileName);

            // Check if the file exists
            if (file.exists()) {
                System.out.println("======The file already exists.");
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
            System.out.println("======file downloading done");
            return Config.ONTOLOGY_FOLDER + fileName;
        } catch (IOException e) {
            // handle exception
            e.printStackTrace();
            return null;
        }
    }

    private String getOntologySource(File file) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        try {
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);

            return ontology.getOntologyID().getOntologyIRI().get().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private JSONObject getIntraMappingSet(String name, Model m, StmtIterator siter, String ontologyVersion) {
        JSONArray creators = new JSONArray();
        JSONObject creator = new JSONObject();
        creator.put("id", "nass");
        creator.put("label", "nass");
        creator.put("type", "AUTHOR");
        creators.put(creator);

        JSONObject mappingSetJSON = new JSONObject();
        mappingSetJSON.put("comment", getComment(null));
        mappingSetJSON.put("creators", creators);
        mappingSetJSON.put("description", "");
        mappingSetJSON.put("license", "");
        mappingSetJSON.put("mapping_date", getDate());
        mappingSetJSON.put("mapping_provider", "" );
        mappingSetJSON.put("mapping_set_id",  name + "#intra" );
        mappingSetJSON.put("mapping_tool", getMappingTool(getComment(null)) );
        mappingSetJSON.put("source", new JSONArray());
        mappingSetJSON.put("version", ontologyVersion);
        mappingSetJSON.put("mapping_set_source", m.getNsPrefixURI("") );
        mappingSetJSON.put("mapping_set_title", "");
        mappingSetJSON.put("subject_source", m.getNsPrefixURI("") );
        mappingSetJSON.put("subject_type", getType(siter));
        mappingSetJSON.put("subject_source_version", "");
        mappingSetJSON.put("subject_preprocessing", "");
        mappingSetJSON.put( "subject_match_field", getMatchField(getComment(null)) );
        mappingSetJSON.put("object_source", m.getNsPrefixURI("") );
        mappingSetJSON.put("object_type", getType(siter));
        mappingSetJSON.put("object_source_version", "");
        mappingSetJSON.put("object_preprocessing", "");
        mappingSetJSON.put( "object_match_field", getMatchField(getComment(null)) );
        mappingSetJSON.put("other", "");
        mappingSetJSON.put("see_also", "");

        return mappingSetJSON;
    }

    private JSONObject geInterMappingSet(String name, Model m, StmtIterator siter, String ontologyVersion) {
        JSONArray creators = new JSONArray();
        JSONObject creator = new JSONObject();
        creator.put("id", "nass");
        creator.put("label", "nass");
        creator.put("type", "AUTHOR");
        creators.put(creator);

        JSONObject mappingSetJSON = new JSONObject();
        mappingSetJSON.put("comment", getComment(null));
        mappingSetJSON.put("creators", creators);
        mappingSetJSON.put("description", "");
        mappingSetJSON.put("license", "");
        mappingSetJSON.put("mapping_date", getDate());
        mappingSetJSON.put("mapping_provider", "" );
        mappingSetJSON.put("mapping_set_id", name + "#inter" );
        mappingSetJSON.put("mapping_tool", getMappingTool(getComment(null)) );
        mappingSetJSON.put("source", new JSONArray());
        mappingSetJSON.put("version", ontologyVersion);
        mappingSetJSON.put("mapping_set_source", m.getNsPrefixURI("") );
        mappingSetJSON.put("mapping_set_title", "");
        mappingSetJSON.put("subject_source", m.getNsPrefixURI("") );
        mappingSetJSON.put("subject_type", getType(siter));
        mappingSetJSON.put("subject_source_version", "");
        mappingSetJSON.put("subject_preprocessing", "");
        mappingSetJSON.put( "subject_match_field", getMatchField(getComment(null)) );
        mappingSetJSON.put("object_source", m.getNsPrefixURI("") );
        mappingSetJSON.put("object_type", getType(siter));
        mappingSetJSON.put("object_source_version", "");
        mappingSetJSON.put("object_preprocessing", "");
        mappingSetJSON.put( "object_match_field", getMatchField(getComment(null)) );
        mappingSetJSON.put("other", "");
        mappingSetJSON.put("see_also", "");

        return mappingSetJSON;
    }

    private JSONObject getCrossMappingSet(String name, Model m, StmtIterator siter, String ontologyVersion) {
        JSONArray creators = new JSONArray();
        JSONObject creator = new JSONObject();
        creator.put("id", "nass");
        creator.put("label", "nass");
        creator.put("type", "AUTHOR");
        creators.put(creator);

        JSONObject mappingSetJSON = new JSONObject();
        mappingSetJSON.put("comment", getComment(null));
        mappingSetJSON.put("creators", creators);
        mappingSetJSON.put("description", "");
        mappingSetJSON.put("license", "");
        mappingSetJSON.put("mapping_date", getDate());
        mappingSetJSON.put("mapping_provider", "" );
        mappingSetJSON.put("mapping_set_id",  name + "#cross" );
        mappingSetJSON.put("mapping_tool", getMappingTool(getComment(null)) );
        mappingSetJSON.put("source", new JSONArray());
        mappingSetJSON.put("version", ontologyVersion);
        mappingSetJSON.put("mapping_set_source", m.getNsPrefixURI("") );
        mappingSetJSON.put("mapping_set_title", "");
        mappingSetJSON.put("subject_source", m.getNsPrefixURI("") );
        mappingSetJSON.put("subject_type", getType(siter));
        mappingSetJSON.put("subject_source_version", "");
        mappingSetJSON.put("subject_preprocessing", "");
        mappingSetJSON.put( "subject_match_field", getMatchField(getComment(null)) );
        mappingSetJSON.put("object_source", m.getNsPrefixURI("") );
        mappingSetJSON.put("object_type", getType(siter));
        mappingSetJSON.put("object_source_version", "");
        mappingSetJSON.put("object_preprocessing", "");
        mappingSetJSON.put( "object_match_field", getMatchField(getComment(null)) );
        mappingSetJSON.put("other", "");
        mappingSetJSON.put("see_also", "");

        return mappingSetJSON;
    }

    private String getMappingProvider(JSONObject intra, JSONObject inter, JSONObject cross, String mappingType) {
        switch (mappingType) {
            case "http://industryportal.enit.fr/sssom#intra" :
                return intra.getString("mapping_provider");
            case "http://industryportal.enit.fr/sssom#inter" :
                return inter.getString("mapping_provider");
            case "http://industryportal.enit.fr/sssom#cross" :
                return cross.getString("mapping_provider");
            default :
                return "";
        }
    }

    private String getSemanticSimilarityScore(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "";
        return "";
    }

    private String getMatchString(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "";
        return "";
    }

    private boolean isLogicalAxiom(Statement s) {

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

    private String getOntologyUri(Model m, String acronym) {
        if (!Objects.equals(m.getNsPrefixURI(""), "")) return m.getNsPrefixURI("");
        if (m.getNsPrefixMap().containsKey(acronym.toLowerCase()) ) return m.getNsPrefixMap().get(acronym.toLowerCase());
        return acronym;
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

    private String getComment(Statement s) {
        return "http://industryportal.enit.fr/sssom#manual";
    }

    private String getCategory(ArrayList<Triple> triples, String id) {
        for (Triple triple : triples
        ) {
            if (
                    triple.getSubject().toString().equals(id)
            )
                if (
                        triple.getSubject().toString().equals(id) && triple.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
                ) return triple.getObject().toString().split("/")[triple.getObject().toString().split("/").length - 1];

            if (
                    triple.getSubject().toString().equals(id) && (triple.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest") || triple.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#first") )
            ) return "rdf:List";

        }
        return "";
    }

    private String getCategory(Resource resource) {
        if (resource.hasProperty(RDF.type, OWL.Restriction)) return "OWL_RESTRICTION";
        if (resource.hasProperty(RDF.type, XSD.nonNegativeInteger)) return "XSD_NON_NEGATIVE_INTEGER";
        if (resource.hasProperty(RDF.first )) return "RDF_LIST";
        return "";
    }

    private String getType(Resource resource) {
        if (resource.hasProperty(RDF.type, OWL.Class)) return "OWL_CLASS";
        if (resource.hasProperty(RDF.type, OWL.ObjectProperty)) return "OWL_OBJECT_PROPERTY";
        if (resource.hasProperty(RDF.type, OWL.DatatypeProperty)) return "DATA_TYPE_PROPERTY";
        if (resource.hasProperty(RDF.type, OWL.Restriction)) return "OWL_CLASS";
        if (resource.hasProperty(RDF.first )) return "OWL_CLASS";

        return "OWL_CLASS";
    }

    private String getType(StmtIterator stmts) {
        while (stmts.hasNext() ) {
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

    private String getLabel(Resource resource) {
        if (resource.hasProperty(RDFS.label)) return resource.getProperty(RDFS.label).getObject().asLiteral().getString();
        if (resource.hasProperty(SKOS.prefLabel)) return resource.getProperty(RDFS.label).getObject().asLiteral().getString();
        return "";
    }

    private String getMappingsJustification(Resource s, RDFNode o, Property p, String comment) {
        if ( o.isResource()) {
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
        }else if (o.isLiteral()) {
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

    private String getOntologyVersion(String ontologyPath) {
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

    private String getOntologySource(String uri) {
        String[] parts = uri.split("#");
        if (parts.length == 2) {
            return parts[0];
        }
        return "";
    }

    private String getConfidence(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "1";
        if (comment.equals("http://industryportal.enit.fr/sssom#automatic")) return "";
        return "";
    }

    private String getMatchField(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "1";
        return "";
    }

    private String getPreProcessing() {
        return "";
    }

    private String getSemanticSimilarity(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "";
        return "";
    }

    private String getMappingType(String subject, String object, Set<OWLImportsDeclaration> imports) {
        if (getOntologySource(subject).equals(getOntologySource(object))) return "http://industryportal.enit.fr/sssom#intra";
        if (imports.contains(getOntologySource(subject)) || imports.contains(getOntologySource(object))) return "http://industryportal.enit.fr/sssom#inter";
        return "http://industryportal.enit.fr/sssom#cross";
    }

    private String getDate() {
        return LocalDateTime.now().toString();
    }

    private String getMappingTool(String comment) {
        if (comment.equals("http://industryportal.enit.fr/sssom#manual")) return "";
        return "";
    }

    private Set<OWLImportsDeclaration> getImports(String filePath) throws OWLOntologyCreationException {
        // Load the ontology from a file
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File file = new File(filePath);
        try {
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
// Get the imports of the ontology
            return ontology.getImportsDeclarations();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptySet();
        }

    }

    private int isSimpleAxiom(String axiom, String axiomType) {
       // if ( countOccurrences(axiom, "Class:") == 2) return true ;
        //connectors available in the manchester format ,i fthey are present in the axiom string then is it a complex axiom
        String[] complexPatterns = { "and", "or", "some", "only", "not", "value", "exactly", "min", "max", "inverse", "self", "that", "where", "orMore", "orLess", "intersectionOf", "unionOf", "complementOf", "oneOf", "disjointWith", "subClassOf", "equivalentTo" };
        String patternString = "\\b(" + String.join("|", complexPatterns) + ")\\b";
        if ( Pattern.compile(patternString).matcher(axiom).find() || countOccurrences(axiom, "Class:") > 2  || axiomType.equals("DisjointUnion") ) {
            //if it contains parenthesis, we'll return one, to handle the axiom depending on opening and closing parenthesis
            if ( axiom.contains("(") )return 1;
            //if it doesn't contain parenthesis, we'll return two, to handle the axiom without depending on them in parsing
            return 2;
        }
        //if it doesn't find any matches it returns zero, meaning it is a simple axiom
        return 0;

    }

    private int countOccurrences(String text, String searchString) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(searchString, index)) != -1) {
            count++;
            index += searchString.length();
        }
        return count;
    }

}

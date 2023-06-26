package fr.industryportal.ontomapper.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.industryportal.ontomapper.config.Config;
import fr.industryportal.ontomapper.helpers.ExtractHelper;
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


/**
 * @author Nasreddine Bouchemel
 */
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


    @GetMapping("")
    public String extractTriplesWithJena(HttpServletRequest request, @RequestParam String acronym) throws OWLOntologyCreationException, IOException {
        User user = ((User) request.getAttribute("user"));
        ExtractHelper extractHelper = new ExtractHelper();
        String apikey = user.getApikey();
        String username = user.getUsername();
        String filePath = extractHelper.downloadOntologyFile(apikey, acronym);
        //String filePath = Config.ONTOLOGY_FOLDER + "Core.rdf";
        if (filePath == null ) {
            JSONObject o = new JSONObject();
            o.put("message", "error in getting ontology source file from portal");
            return o.toString();
        }
        //String filePath = Config.ONTOLOGY_FOLDER + "ROMAIN.owl";
        //String ontologyUri = extractHelper.getOntologySource(new File(filePath));
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
            String ontologyVersion = extractHelper.getOntologyVersion(filePath);

            System.out.println("ontology version: " + ontologyVersion );
            Set<OWLImportsDeclaration> imports = extractHelper.getImports(filePath);

            JSONObject intraMapping = extractHelper.getIntraMappingSet(acronym, m, m.listStatements(), ontologyVersion);
            JSONArray intraMappings = new JSONArray();

            JSONObject interMapping = extractHelper.geInterMappingSet(acronym, m, m.listStatements(), ontologyVersion);
            JSONArray interMappings = new JSONArray();

            JSONObject crossMapping = extractHelper.getCrossMappingSet(acronym, m, m.listStatements(), ontologyVersion);
            JSONArray crossMappings = new JSONArray();

            Long intraMappingSetId = ApiService.postMappingSet(apikey, username, intraMapping);

            Long interMappingSetId = ApiService.postMappingSet(apikey, username, interMapping);

            Long crossMappingSetId = ApiService.postMappingSet(apikey, username, crossMapping);



            while (siter.hasNext()) {
                Statement s = siter.next();


                if (!extractHelper.isLogicalAxiom(s)) continue;

                String comment = extractHelper.getComment(s);

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
                    objectCategory = extractHelper.getCategory(object);
                    objectType = extractHelper.getType(object);
                    objectLabel = extractHelper.getLabel(object);
                    // do something with the resource object
                }

                String subjectUri = s.getSubject().toString();
                String predicateUri = s.getPredicate().toString();
                String objectUri = s.getObject().toString();

                JSONObject tripleJSON = new JSONObject();
                tripleJSON.put("other", extractHelper.getMappingType(subjectUri, objectUri, imports));
                tripleJSON.put("mapping_id", subjectUri+ "_" + predicateUri+ "_" +objectUri );
                tripleJSON.put("mapping_provider", extractHelper.getMappingProvider(intraMapping, interMapping, crossMapping, tripleJSON.getString("other")) );
                tripleJSON.put("mapping_tool", extractHelper.getMappingTool(comment) );
                tripleJSON.put("mapping_tool_version", "");
                tripleJSON.put("match_string", extractHelper.getMatchString(comment) );
                tripleJSON.put("source", "" );
                tripleJSON.put("subject_id",  subjectUri);
                tripleJSON.put("subject_label", extractHelper.getLabel(subject) );
                tripleJSON.put("subject_type", extractHelper.getType(subject));
                tripleJSON.put("subject_category", extractHelper.getCategory(subject));
                tripleJSON.put("subject_source", extractHelper.getOntologySource(subjectUri) );
                tripleJSON.put("subject_source_version", ontologyVersion );
                tripleJSON.put( "subject_match_field", extractHelper.getMatchField(comment) );
                tripleJSON.put("subject_preprocessing", extractHelper.getPreProcessing() );
                tripleJSON.put("predicate_id", extractHelper.getPredicateIdentifier(predicateUri));
                tripleJSON.put("predicate_label", extractHelper.getLabel(predicate) );
                tripleJSON.put("predicate_Modifier", extractHelper.getPredicateModifier(predicateUri) );
                tripleJSON.put("object_id",  objectUri);
                tripleJSON.put("object_label", objectLabel);
                tripleJSON.put("object_type", objectType);
                tripleJSON.put("object_category", objectCategory);
                tripleJSON.put("object_source", extractHelper.getOntologySource(objectUri));
                tripleJSON.put("object_source_version", ontologyVersion );
                tripleJSON.put("object_match_field", extractHelper.getMatchField(comment) );
                tripleJSON.put("object_preprocessing", extractHelper.getPreProcessing() );
                tripleJSON.put("justification", extractHelper.getMappingsJustification(subject, s.getObject(), predicate , comment) );
                tripleJSON.put("semantic_similarity_measure", extractHelper.getSemanticSimilarity(comment));
                tripleJSON.put("semantic_similarity_score", extractHelper.getSemanticSimilarityScore(comment));
                tripleJSON.put("confidence", extractHelper.getConfidence(comment) );
                tripleJSON.put("cardinality", "MANY_TO_MANY");
                tripleJSON.put("contriutors", new JSONArray());
                tripleJSON.put("license", "");
                tripleJSON.put("mapping_date", extractHelper.getDate());
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

}

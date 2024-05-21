package fr.industryportal.ontomapper.controller;

import fr.industryportal.ontomapper.helpers.ExtractHelper;
import fr.industryportal.ontomapper.model.repos.*;
import fr.industryportal.ontomapper.model.requests.User;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;


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

    @Autowired
    private LinkedDataMappingRepository linkedDataMappingRepository;

    @GetMapping("")
    public String extractTriplesWithJena(HttpServletRequest request, @RequestParam String acronym) throws OWLOntologyCreationException, IOException {
        User user = ((User) request.getAttribute("user"));
        ExtractHelper extractHelper = new ExtractHelper();
        String apikey = user.getApikey();
        String username = user.getUsername();
        String filePath = extractHelper.downloadOntologyFile(apikey, acronym);
        //String filePath = Config.ONTOLOGY_FOLDER + "testten.owl";
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
            FileOutputStream fo = new FileOutputStream(new File(acronym + ".txt"));

            while (siter.hasNext()) {
                Statement s = siter.next();

                String subjectUri = s.getSubject().toString();
                String predicateUri = s.getPredicate().toString();
                String objectUri = s.getObject().toString();


                // System.out.println(s.getSubject().toString() + "\t" +
                //                                 s.getPredicate().toString() + "\t" +
                //                                 s.getObject().toString());
                fo.write(new String(subjectUri + " " +
                        predicateUri + " " +
                        objectUri + "\n").getBytes());


            }


            fo.flush();
          fo.close();
          System.out.println("result is written to file:" + acronym + ".txt");


            return "done";

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

    @GetMapping("/{acronym}/cross")
    public String extractCrossMappings(HttpServletRequest request, @PathVariable String acronym) {
        User user = ((User) request.getAttribute("user"));
        ExtractHelper extractHelper = new ExtractHelper();
        String apikey = user.getApikey();
        String username = user.getUsername();

        String filepath = extractHelper.downloadOntologyFile(apikey, acronym);
        if (filepath == null) {
            return null;
        }
        OWLOntology sourceOntology = ExtractHelper.getOntologyFromFile(new File(filepath));
        if (sourceOntology == null) {
            return null;
        }
        JSONArray result =  extractHelper.extractLinkedDataMappings(sourceOntology, acronym, apikey, username).getJSONArray("linked_data");
        JSONArray sssomRes =   extractHelper.extractLinkedDataMappings(sourceOntology, acronym, apikey, username).getJSONArray("sssom");
        extractHelper.storeLinkedDataMappings(linkedDataMappingRepository, result);
        extractHelper.storeSSSOMMappings(mappingRepository, mappingSetRepository, sssomRes);


        return "ok";
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

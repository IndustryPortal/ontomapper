package fr.industryportal.ontomapper.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.owlcs.ontapi.Ontology;
import fr.industryportal.ontomapper.helpers.ExtractHelper;
import fr.industryportal.ontomapper.helpers.ManchesterMappingHelper;
import fr.industryportal.ontomapper.helpers.OntologyHelper;
import fr.industryportal.ontomapper.model.entities.LinkedDataMapping;
import fr.industryportal.ontomapper.model.entities.ManchesterMapping;
import fr.industryportal.ontomapper.model.repos.LinkedDataMappingRepository;
import fr.industryportal.ontomapper.model.repos.ManchesterMappingRepository;
import fr.industryportal.ontomapper.model.requests.ManchesterMappingRequest;
import fr.industryportal.ontomapper.model.requests.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


/**
 * @author Nasreddine Bouchemel
 */
@RestController
@RequestMapping("/manchester")
public class ManchesterOwlController {


    @Autowired
    private ManchesterMappingRepository manchesterMappingRepository;

    @Autowired
    LinkedDataMappingRepository linkedDataMappingRepository;

    @Autowired
    ManchesterMappingHelper manchesterMappingHelper;

    @Autowired
    OntologyHelper ontologyHelper;

    @GetMapping("/{acronym}/extract")
    public String extractClassRelationsByClassId(HttpServletRequest request, @PathVariable String acronym, @RequestParam String classUri) throws IOException {
        System.out.println("======looking for mapping of class:" + classUri + " from " + acronym + " ontology");
        ExtractHelper.logger.info("======looking for mapping of class:" + classUri + " from " + acronym + " ontology");
        ExtractHelper extractHelper = new ExtractHelper();
        classUri = classUri.trim();
        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();

        String filePath = extractHelper.downloadOntologyFile(apikey, acronym);
        //String filePath = Config.ONTOLOGY_FOLDER + "Core.rdf";
        if (filePath == null ) {
            JSONObject o = new JSONObject();
            o.put("message", "error in getting ontology source file from portal");
            return o.toString();
        }

//            ontology = ExtractHelper.getOntologyFromFile(new File(filePath));
//        if (ontology == null) {
//            JSONObject o = new JSONObject();
//            o.put("message", "error in reading" + acronym +" ontology file");
//            return o.toString();
//        }

        Ontology ontology;
        try {
            ontology = ontologyHelper.getOntologyWithModelFromFile(new File(filePath));
        } catch (OWLOntologyCreationException e) {
            ExtractHelper.logger.log(Level.WARNING, e.getMessage());
            JSONObject o = new JSONObject();
            o.put("message", "error in reading" + acronym +" ontology file");
            return o.toString();
        }


        JSONArray result = manchesterMappingHelper.extractManchesterMappingsByClassId(ontology, acronym, classUri, filePath, manchesterMappingRepository);


        return  result.toString();

    }

    @GetMapping("/{acronym}/getClassRelations")
    public String getRelationsByClassId(HttpServletRequest request, @PathVariable String acronym, @RequestParam String classUri) {

        List<ManchesterMapping> mappings = new ArrayList<>();

        mappings.addAll(manchesterMappingRepository.findByStringClassUri(classUri));

        if (mappings.isEmpty()) {
            return "[]";
        } else {
            //return the mappings table in a json format
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String jsonMappins = objectMapper.writeValueAsString(mappings);
                return  jsonMappins;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "error";
            }
        }

    }

    @GetMapping("/linked_data")
    public String getLinkedDataMappings(HttpServletRequest request, @RequestParam String ontology_uri ) throws JsonProcessingException {
        List<LinkedDataMapping> list = linkedDataMappingRepository.findBySourceOntologyId(ontology_uri);
        // return the list
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(list);
        return json;
    }

}

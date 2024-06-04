package fr.industryportal.ontomapper.controller;

import com.github.owlcs.ontapi.Ontology;
import fr.industryportal.ontomapper.OntomapperApplication;
import fr.industryportal.ontomapper.helpers.CronHelper;
import fr.industryportal.ontomapper.helpers.ExtractHelper;
import fr.industryportal.ontomapper.helpers.OntologyHelper;
import fr.industryportal.ontomapper.helpers.TripleStoreHelper;
import fr.industryportal.ontomapper.model.repos.LinkedDataMappingRepository;
import fr.industryportal.ontomapper.model.repos.ManchesterMappingRepository;
import fr.industryportal.ontomapper.model.repos.MappingRepository;
import fr.industryportal.ontomapper.model.repos.MappingSetRepository;
import fr.industryportal.ontomapper.model.requests.User;
import fr.industryportal.ontomapper.services.ApiService;
import org.json.JSONArray;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Nasreddine Bouchemel
 */
@RestController
@RequestMapping("/cron")
public class CronController {

    @Autowired
    ManchesterMappingRepository manchesterMappingRepository;

    @Autowired
    LinkedDataMappingRepository linkedDataMappingRepository;

    @Autowired
    MappingRepository mappingRepository;

    @Autowired
    MappingSetRepository mappingSetRepository;

    @Autowired
    CronHelper cronHelper;

    @Autowired
    ExtractHelper extractHelper;

    @Autowired
    ApiService apiService;

    @Autowired
    TripleStoreHelper tripleStoreHelper;

    @Autowired
    OntologyHelper ontologyHelper;


    @GetMapping("/manchester")
    public String parseAllOntologiesForManchester(HttpServletRequest request) {
        System.out.println("=====starting parsing all ontologies to get manchester mappings");
        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();
        //CronHelper.extractAcronyms(apikey);
        cronHelper.parseAllPortalClasses(apikey, username, manchesterMappingRepository);
        return "ok";
    }

    @GetMapping("/linked_data")
    public String parseAllOntologiesForLinkedData(HttpServletRequest request) {
        System.out.println("=====starting parsing all ontologies to get linked data mappings");
        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();
        cronHelper.parseOntologiesForLinkedDataMappings(apikey, username, linkedDataMappingRepository);
        return "ok";

    }

    @GetMapping("/{acronym}/manchester")
    public String parseOntologyForManchster(HttpServletRequest request, @PathVariable String acronym) {
        System.out.println("=====starting parsing ontology " + acronym + " to get manchester mappings");
        ExtractHelper.logger.info("=====starting parsing ontology " + acronym + " to get manchester mappings");

        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();

        ExtractHelper extractHelper = new ExtractHelper();

        String filepath = extractHelper.downloadOntologyFile(apikey, acronym);
        if (filepath == null) {
            return null;
        }
//        OWLOntology sourceOntology = ExtractHelper.getOntologyFromFile(new File(filepath));
//        if (sourceOntology == null) {
//            return null;
//        }

        Ontology sourceOntology;
        try {
            sourceOntology = ontologyHelper.getOntologyWithModelFromFile(new File(filepath));
        } catch (OWLOntologyCreationException e) {
            ExtractHelper.logger.log(Level.WARNING, e.getMessage());
            return null;
        }

        String result = cronHelper.parseOntologyForManchester(sourceOntology, filepath, apikey, username, acronym, manchesterMappingRepository);
        if (result == "done") {
            return "done parsing mappings from " + acronym;
        } else {
            return result;
        }
    }

    @GetMapping("/upload_to_portal")
    public int uploadMappingsToPortal(HttpServletRequest request) {
        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();
        return apiService.uploadMappingToPortal(apikey, linkedDataMappingRepository);

    }


    @GetMapping("{acronym}/upload_to_portal")
    public int uploadMappingsToPortal(HttpServletRequest request, @PathVariable String acronym) {
        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();
        return ApiService.uploadOntologyMappingToPortal(acronym, apikey, linkedDataMappingRepository);

    }

    @GetMapping("{acronym}")
    public String parseOntologyForMappings(HttpServletRequest request, @PathVariable String acronym) throws OWLOntologyCreationException, IOException {
        ExtractHelper.logger.log(Level.INFO, "=====starting parsing ontology " + acronym);

        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();

//        ExtractorController extCOn = new ExtractorController();
//
//        extCOn.extractTriplesWithJena(request, acronym);


        String filepath = extractHelper.downloadOntologyFile(apikey, acronym);
        if (filepath == null) {
            return null;
        }
//        OWLOntology sourceOntology = ExtractHelper.getOntologyFromFile(new File(filepath));
//        if (sourceOntology == null) {
//            return null;
//        }

        Ontology sourceOntology;
        try {
            sourceOntology = ontologyHelper.getOntologyWithModelFromFile(new File(filepath));
        } catch (OWLOntologyCreationException e) {
            ExtractHelper.logger.log(Level.WARNING, e.getMessage());
            return null;
        }

        tripleStoreHelper.getOntologyFromTripleStore(acronym);

        //intra mapping
        //cronHelper.parseOntologyForManchester(sourceOntology, filepath, apikey, username, acronym, manchesterMappingRepository);

        //cross && inter mappings
        JSONArray result = extractHelper.extractLinkedDataMappings(sourceOntology, acronym, apikey, username).getJSONArray("linked_data");
        extractHelper.storeLinkedDataMappings(linkedDataMappingRepository, result);
        JSONArray sssomRes = extractHelper.extractLinkedDataMappings(sourceOntology, acronym, apikey, username).getJSONArray("sssom");
        extractHelper.storeSSSOMMappings(mappingRepository, mappingSetRepository, sssomRes);

        ApiService.uploadOntologyMappingToPortal(acronym, apikey, linkedDataMappingRepository);

        return "operation ended with success";

    }

    @PostMapping("/parse_file/{acronym}")
    public String parseOntologyForMappings(
            HttpServletRequest request,
            @PathVariable String acronym,
            @RequestPart("file") MultipartFile ontologyFile  // This parameter is for the file
    ) throws IOException {
        ExtractHelper.logger.log(Level.INFO, "=====starting parsing file ");

        if (ontologyFile == null || ontologyFile.isEmpty()) {
            return "Error: Ontology file is not provided.";
        }

        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();

        ExtractHelper extractHelper = new ExtractHelper();

//        OWLOntology sourceOntology = ExtractHelper.getOntologyFromFile(convertMultipartFileToFile(ontologyFile));
//        if (sourceOntology == null) {
//            return "Error: could not read ontology from file.";
//        }
        Ontology sourceOntology;
        try {
            sourceOntology = ontologyHelper.getOntologyWithModelFromFile(convertMultipartFileToFile(ontologyFile));
        } catch (OWLOntologyCreationException e) {
            ExtractHelper.logger.log(Level.WARNING, e.getMessage());
            return null;
        }

        // Save the MultipartFile to a temporary file
        Path tempFilePath = Files.createTempFile(ontologyFile.getName(), ".owl");
        File tempFile = tempFilePath.toFile();
        ontologyFile.transferTo(tempFile);




        cronHelper.parseOntologyForManchester(sourceOntology, tempFile.getPath(), apikey, username, acronym, manchesterMappingRepository);

        //cross && inter mappings
        JSONArray result = extractHelper.extractLinkedDataMappings(sourceOntology, acronym, apikey, username).getJSONArray("linked_data");
        extractHelper.storeLinkedDataMappings(linkedDataMappingRepository, result);
        JSONArray sssomRes = extractHelper.extractLinkedDataMappings(sourceOntology, acronym, apikey, username).getJSONArray("sssom");
        extractHelper.storeSSSOMMappings(mappingRepository, mappingSetRepository, sssomRes);

        ApiService.uploadOntologyMappingToPortal(acronym, apikey, linkedDataMappingRepository);

        tempFile.delete();

        return "operation ended with success";

    }

    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = Files.createTempFile("uploaded-ontology-", ".owl").toFile();
        multipartFile.transferTo(file);
        return file;
    }


}

package fr.industryportal.ontomapper.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.industryportal.ontomapper.helpers.ExtractHelper;
import fr.industryportal.ontomapper.model.entities.Mapping;
import fr.industryportal.ontomapper.model.entities.MappingSet;
import fr.industryportal.ontomapper.model.repos.LinkedDataMappingRepository;
import fr.industryportal.ontomapper.model.repos.MappingRepository;
import fr.industryportal.ontomapper.model.repos.MappingSetRepository;
import fr.industryportal.ontomapper.model.requests.MappingRequest;
import fr.industryportal.ontomapper.model.requests.SetRequest;
import fr.industryportal.ontomapper.model.requests.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.Multipart;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.channels.MulticastChannel;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/bulk")
public class BulkLoadController {


    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private MappingSetRepository mappingSetRepository;

    @Autowired
    private LinkedDataMappingRepository linkedDataMappingRepository;


    @PostMapping("")
    public ResponseEntity<String> bulkUploadMappings(HttpServletRequest request, @RequestBody List<SetRequest> mappingsFile) {

        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();

        ExtractHelper extractHelper = new ExtractHelper();

        if (mappingsFile == null || mappingsFile.isEmpty()) {
            return ResponseEntity.badRequest().body("Uploaded file is empty");
        }

        String messages = "";

        try {

            // Read the content of the uploaded file as an InputStream
//            byte[] fileContent = mappingsFile.getBytes();

            // Convert the InputStream to a String (assuming UTF-8 encoding)
//            String fileContentString = new String(fileContent, "UTF-8");

            // Parse the String as JSON using Jackson ObjectMapper
//            ObjectMapper objectMapper = new ObjectMapper();
//            List<SetRequest> requestBody = objectMapper.readValue(fileContentString, new TypeReference<List<SetRequest>>() {});

            List<MappingSet> result = new ArrayList<>();

//            List<Mapping> mappingList = new ArrayList<>();

            mappingsFile.forEach(
                    r -> {
                        if (mappingSetRepository.findByStringId(r.getMapping_set_id()) == null) {

                            MappingSet set = r.toDBModel(mappingSetRepository, username);
                            result.add(set);
                            mappingSetRepository.save(set);

                            List<MappingRequest> mappingsRes = r.getMappings();

                            for (MappingRequest req : mappingsRes
                            ) {

                                req.setSet_id(set.getId());

                                Mapping mapping = req.toDBModel(mappingSetRepository, username);

                                mappingRepository.save(mapping);

                            }
                        }

                    }
            );

            extractHelper.storeLinkedDataMappings(linkedDataMappingRepository, extractHelper.convertSSSOMToLinkedDataMappings(mappingsFile));

            return ResponseEntity.ok("{\"message\": \"mappings saved succefully\", \"sets\": " + result + "}");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process the uploaded file");
        }

    }

}

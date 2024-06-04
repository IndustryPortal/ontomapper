package fr.industryportal.ontomapper.helpers;

import com.github.owlcs.ontapi.Ontology;
import fr.industryportal.ontomapper.config.AppConfig;
import fr.industryportal.ontomapper.model.repos.LinkedDataMappingRepository;
import fr.industryportal.ontomapper.model.repos.ManchesterMappingRepository;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

@Component
public class CronHelper {


    @Autowired
    private ManchesterMappingHelper manchesterMappingHelper;

    @Autowired
    private OntologyHelper ontologyHelper;

    @Autowired
    private ExtractHelper extractHelper;
    public int parseAllPortalClasses(String apikey,  String username, ManchesterMappingRepository repo) {
        extractAcronyms(apikey).forEach(
                acro -> {
                    String filepath = extractHelper.downloadOntologyFile(apikey, acro);
                    if (filepath != null) {
                        //OWLOntology sourceOntology = ExtractHelper.getOntologyFromFile(new File(filepath));

                        Ontology sourceOntology;
                        try {
                            sourceOntology = ontologyHelper.getOntologyWithModelFromFile(new File(filepath));
                            parseOntologyForManchester(sourceOntology, filepath,  apikey, username, acro, repo);
                        } catch (OWLOntologyCreationException e) {
                            ExtractHelper.logger.log(Level.WARNING, e.getMessage());
                        }
                    }
                }
        );

        return 1;

    }

    public String parseOntologyForManchester(Ontology ontology, String ontolgoyFilePath, String apikey, String username, String acro, ManchesterMappingRepository repo)  {
        if  (ontology == null) {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("message", "error in reading" + acro +" ontology file");
            return o.toString();
        }
        for (String cls : getClassesByAcronym(acro, apikey)) {
            manchesterMappingHelper.extractManchesterMappingsByClassId(ontology, acro, cls, ontolgoyFilePath, repo);
        }
        return "done";
    }

    public  int parseOntologiesForLinkedDataMappings(String apikey, String username, LinkedDataMappingRepository repo) {


        extractAcronyms(apikey).forEach(
                acronym -> {
                    String filepath = extractHelper.downloadOntologyFile(apikey, acronym);
                    if (filepath != null) {
                        //OWLOntology sourceOntology = ExtractHelper.getOntologyFromFile(new File(filepath));

                        Ontology sourceOntology;
                        try {
                            sourceOntology = ontologyHelper.getOntologyWithModelFromFile(new File(filepath));
                            org.json.JSONArray result = extractHelper.extractLinkedDataMappings(sourceOntology, acronym, apikey, username).getJSONArray("linked_data");
                            extractHelper.storeLinkedDataMappings(repo, result);
                        } catch (OWLOntologyCreationException e) {
                            ExtractHelper.logger.log(Level.WARNING, e.getMessage());
                        }
                    }
                }
        );
        return 0;
    }


    public  List<String> extractAcronyms(String apikey) {
        List<String> list = new ArrayList<>();
        int responseCode;
        try {
            URL url = new URL(AppConfig.getInstance().getApiUrl() + "?apikey=" + apikey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            responseCode = conn.getResponseCode();
            if (responseCode != 200) return Collections.emptyList();
            String inline = "";
            Scanner scanner = new Scanner(url.openStream());

            //Write all the JSON data into a string using a scanner
            while (scanner.hasNext()) {
                inline += scanner.nextLine();
            }

            //Close the scanner
            scanner.close();
            JSONParser parse = new JSONParser();
            JSONArray ontos = (JSONArray) parse.parse(inline);
            for (int i = 0; i < ontos.toArray().length; i++) {
                JSONObject ont = (JSONObject) ontos.get(i);
                list.add(ont.getAsString("acronym"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return list;

        //List<String> templist = new ArrayList<>();
//
//        templist.add("HRMO");
//        templist.add("OMG");
//
//
//        return templist ;
    }

    public  List<String> getClassesByAcronym(String acronym, String apikey) {
        ExtractHelper.logger.info("=====getting classes for ontology " + acronym + " from portal");
        List<String> classes = new ArrayList<>();
        int page = 1;
        int pageCount;
        int totalCount;
        do {
            int responseCode;
            try {
                //URL url = new URL(Config.API_URL + "ontologies/" + acronym + "/classes?"+ "apikey=" + apikey +"&page=" + page );
                URL url = new URL(AppConfig.getInstance().getApiUrl() + "ontologies/" + acronym + "/classes?page=" + page + "&apikey=" + apikey );

                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                        .header("accept", "*/*")
                        .header("Content-Type", "application/json;charset=utf-8")
                        .GET()
                        .build();
                HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) {
                    ExtractHelper.logger.log( java.util.logging.Level.SEVERE,  "======error in getting classes for ontology " + acronym + " from portal, response:");
                    ExtractHelper.logger.log(Level.SEVERE, url.toString());
                    ExtractHelper.logger.log(java.util.logging.Level.SEVERE, res.body());
                    return Collections.emptyList();
                }

                JSONParser parse = new JSONParser();
                JSONObject response = (JSONObject) parse.parse(res.body());

                // Get pagination details
                page = (int) response.get("page");
                pageCount = (int) response.get("pageCount");
                totalCount = (int) response.get("totalCount");


                JSONArray clss = (JSONArray) response.get("collection");
                for (int i = 0; i < clss.toArray().length; i++) {
                    JSONObject cls = (JSONObject) clss.get(i);

                    classes.add(cls.getAsString("@id"));


                }
            } catch (IOException | ParseException | InterruptedException e) {
                ExtractHelper.logger.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
                throw new RuntimeException(e);
            }
            page++;
        } while (page <=  pageCount && classes.size() < totalCount  );

        ExtractHelper.logger.info("found " + classes.size() + " classes in " + acronym + " ontology");
        return classes;

    }


}
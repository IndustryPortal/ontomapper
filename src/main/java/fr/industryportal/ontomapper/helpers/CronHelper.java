package fr.industryportal.ontomapper.helpers;

import fr.industryportal.ontomapper.controller.ManchesterOwlController;
import fr.industryportal.ontomapper.services.ApiService;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class CronHelper {


    private static final String API_URL = "http://data.industryportal.enit.fr/ontologies/";

    public static int parseAllPortalClasses(String apikey, String username) {
        CronHelper.extractAcronyms(apikey).forEach(
                acro -> {
                    parseOntology(apikey, username, acro);
                }
        );

        return 1;

    }

    public static int parseOntology(String apikey, String username, String acro) {
        for (String cls : CronHelper.getClassesByAcronym(acro, apikey)) {
            try {
                ApiService.extractManchesterMappings(apikey, username, acro, cls);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return 1;
    }

    public static List<String> extractAcronyms(String apikey) {
        List<String> list = new ArrayList<>();
        int responseCode;
        try {
            URL url = new URL(API_URL + "?apikey=" + apikey);
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

    public static List<String> getClassesByAcronym(String acronym, String apikey) {
        List<String> classes = new ArrayList<>();
        int responseCode;
        try {
            URL url = new URL(API_URL + acronym + "/classes" + "?apikey=" + apikey);
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
            JSONObject response = (JSONObject) parse.parse(inline);
            JSONArray clss = (JSONArray) response.get("collection");
            for (int i = 0; i < clss.toArray().length; i++ ) {
                JSONObject cls = (JSONObject) clss.get(i);

                classes.add( cls.getAsString("@id") );


            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        return classes;

    }




}
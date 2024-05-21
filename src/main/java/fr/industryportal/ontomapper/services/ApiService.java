package fr.industryportal.ontomapper.services;

import fr.industryportal.ontomapper.config.AppConfig;
import fr.industryportal.ontomapper.helpers.CronHelper;
import fr.industryportal.ontomapper.model.entities.LinkedDataMapping;
import fr.industryportal.ontomapper.model.repos.LinkedDataMappingRepository;
import fr.industryportal.ontomapper.model.requests.LinkedDataMappingRequest;
import fr.industryportal.ontomapper.model.requests.User;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.base.Sys;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class ApiService {

    public static Long postMappingSet(String apikey, String username, JSONObject mappingSet) {

        JSONArray body = new JSONArray();
        body.put(mappingSet);
        // Set request parameters
        String queryParams = "username=" + username + "&apikey=" + apikey;

        // Build the request URI with parameters
        URI uri = URI.create(AppConfig.getInstance().getSelfUrl() + "set" + "?" + queryParams);

        // Create the HttpClient
        HttpClient httpClient = HttpClient.newBuilder().build();

        // Build the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .header("Content-Type", "Application/json")
                .build();

        try {
            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject responseJson = new JSONObject(response.body());
            return responseJson.getLong("set_id");
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any errors that occur during the request
            return null;
        }
    }
    public static Integer postMappings(String apikey, String username, JSONArray mappings) {

        // Set request parameters
        String queryParams = "username=" + username + "&apikey=" + apikey;

        // Build the request URI with parameters
        URI uri = URI.create(AppConfig.getInstance().getSelfUrl() + "mapping" + "?" + queryParams);

        // Create the HttpClient
        HttpClient httpClient = HttpClient.newBuilder().build();

        // Build the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(mappings.toString(), StandardCharsets.UTF_8))
                .header("Content-Type", "Application/json")
                .build();

        try {
            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any errors that occur during the request
            return null;
        }
    }

    public static Integer extractManchesterMappings(String apikey, String username,  String acronym, String classUri ) throws UnsupportedEncodingException {

        // Set request parameters
        String encodedUsername = URLEncoder.encode(username, "UTF-8");
        String encodedApikey = URLEncoder.encode(apikey, "UTF-8");
        String encodedClassUri = URLEncoder.encode(classUri, "UTF-8");

        String queryParams = "username=" + encodedUsername +
                "&apikey=" + encodedApikey +
                "&classUri=" + encodedClassUri;


        // Build the request URI with parameters
        URI uri = URI.create(AppConfig.getInstance().getSelfUrl() + "manchester/" + acronym + "/extract" + "?" + queryParams);

        // Create the HttpClient
        HttpClient httpClient = HttpClient.newBuilder().build();

        // Build the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .header("Content-Type", "Application/json")
                .build();

        try {
            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any errors that occur during the request
            return null;
        }

    }

    public static Integer uploadMappingToPortal(String apikey, LinkedDataMappingRepository repo) {

        System.out.println("=====start uploading mappings to portal");

        CronHelper.extractAcronyms(apikey).forEach(
                acronym -> {
                    System.out.println("starting with " + acronym + " mappings");
                    List<LinkedDataMapping> mappings =  repo.findBySourceOntologyAcronym(acronym);
                    System.out.println("there is " + mappings.size() + " mappings");
                    mappings.forEach(
                            m -> {
                                JSONObject mappingJson = new JSONObject();
                                mappingJson.put("creator", m.getCreator());
                                mappingJson.put("relation", new JSONArray(m.getRelation()));

                                JSONObject classMapping = new JSONObject();
                                classMapping.put(m.getSourceOntologyId(), m.getSourceOntologyAcronym());
                                if (m.isExternalMapping()) {
                                    classMapping.put( m.getTargetOntologyId(), "ext:" +  m.getTargetOntologyAcronym());
                                } else {
                                    classMapping.put( m.getTargetOntologyId(), m.getTargetOntologyAcronym());
                                }
                                mappingJson.put("classes", classMapping);
                                mappingJson.put("external_mapping", m.isExternalMapping());
                                mappingJson.put("source_contact_info", m.getSourceContactInfo());
                                mappingJson.put("source_name", m.getSourceName());
                                mappingJson.put("source", m.getCreator());
                                mappingJson.put("name", m.getName());
                                mappingJson.put("comment", m.getComment());

                                int b = 5;

                            }
                    );
                }
        );
        return 201;
    }


    public static Integer uploadOntologyMappingToPortal(String acronym, String apikey, LinkedDataMappingRepository repo) {



                    System.out.println("starting with " + acronym + " mappings");
                    List<LinkedDataMapping> mappings =  repo.findBySourceOntologyAcronym(acronym);
                    System.out.println("there is " + mappings.size() + " mappings");
                    mappings.forEach(
                            m -> {
                                JSONObject mappingJson = new JSONObject();
                                mappingJson.put("creator", m.getCreator());
                                mappingJson.put("relation", new JSONArray(m.getRelation()));

                                JSONObject classMapping = new JSONObject();
                                classMapping.put(m.getSourceId(), m.getSourceOntologyAcronym());
                                if (m.isExternalMapping()) {
                                    classMapping.put( m.getTargetId(), "ext:" +  m.getTargetOntologyId());
                                } else {
                                    classMapping.put( m.getTargetId(), m.getTargetOntologyAcronym());
                                }
                                mappingJson.put("classes", classMapping);
                                mappingJson.put("external_mapping", m.isExternalMapping());
                                mappingJson.put("source_contact_info", m.getSourceContactInfo());
                                mappingJson.put("source_name", m.getSourceName());
                                mappingJson.put("source", m.getCreator());
                                mappingJson.put("name", m.getName());
                                mappingJson.put("comment", m.getComment());

                                //post the mappingJson to http://data.industryportal.enit.fr/mappings
                                try {
                                    System.out.println("posting mpping " + mappingJson.getString("name") );
                                    String url = AppConfig.getInstance().getApiUrl()+ "mappings" + "?apikey=" + apikey;
                                    CloseableHttpClient httpClient = HttpClients.createDefault();
                                    HttpPost httpPost = new HttpPost(url);
                                    StringEntity requestEntity = new StringEntity(mappingJson.toString(), ContentType.APPLICATION_JSON);
                                    httpPost.setEntity(requestEntity);
                                    CloseableHttpResponse response = httpClient.execute(httpPost);

                                    // Optionally, you can check the response status code and handle it accordingly
                                    int statusCode = response.getStatusLine().getStatusCode();
                                    if (statusCode == 201) {
                                        System.out.println("Mapping posted successfully.");
                                    } else {

                                        System.out.println("Failed to post mapping. Status code: " + statusCode);

                                        // You can handle other status codes as needed.
                                    }

                                    // Close resources
                                    httpClient.close();
                                    response.close();
                                } catch (IOException e) {
                                    // Handle any exceptions that might occur during the HTTP POST request.
                                    e.printStackTrace();
                                    // You can return an appropriate status code or throw an exception here to indicate failure.
                                     // For example, 500 for Internal Server Error
                                }

                            }
                    );

        return 201;
    }

}

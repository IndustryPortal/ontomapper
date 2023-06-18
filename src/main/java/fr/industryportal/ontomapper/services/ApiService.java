package fr.industryportal.ontomapper.services;

import fr.industryportal.ontomapper.config.Config;
import fr.industryportal.ontomapper.model.requests.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ApiService {

    public static Long postMappingSet(String apikey, String username, JSONObject mappingSet) {

        JSONArray body = new JSONArray();
        body.put(mappingSet);
        // Set request parameters
        String queryParams = "username=" + username + "&apikey=" + apikey;

        // Build the request URI with parameters
        URI uri = URI.create(Config.SELF_URL + "set" + "?" + queryParams);

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
        URI uri = URI.create(Config.SELF_URL + "mapping" + "?" + queryParams);

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
}

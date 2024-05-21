package fr.industryportal.ontomapper.helpers;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import fr.industryportal.ontomapper.config.AppConfig;
import fr.industryportal.ontomapper.model.requests.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Abdelwadoud Rasmi
 * This is the main entrypoint of the app
 */
@Component

public class UserHelper {

    @Autowired
    AppConfig appConfig;


    /**
     * Queries the portal api to check of this user exist
     */
    public User getUser(String username, String apikey) {
        String apiUrl = appConfig.getApiUrl() + "/users/" + username + "?apikey=" + apikey;
        RestTemplate restTemplate = new RestTemplate();


        User user = restTemplate.getForObject(apiUrl, User.class);
        return user;

    }
}

package fr.industryportal.ontomapper.controller;

import fr.industryportal.ontomapper.config.AppConfig;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Abdelwadoud Rasmi
 * Controller to manage contributions
 */
@RestController
@RequestMapping("/")
public class RootController {

    @Autowired AppConfig appConfig;

    @GetMapping("")
    public JSONObject getRoot(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        json.put("project_repo", "https://github.com/IndustryPortal/ontomapper");
        json.put("note", "You have to provide the api-key and username while submitting your requests");
        json.put("description", "This service is a mapping storing service that you can add to your Ontoportal instance, it uses the SSSOM standard.");
        json.put("api", "/swagger-ui/index.html#");
        json.put("config", appConfig.getApiUrl());
        return json;
    }

}

package fr.industryportal.ontomapper.controller;

import net.minidev.json.JSONObject;
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

    @GetMapping("")
    public JSONObject getRoot(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        json.put("developer", "Abdelouadoud Rasmi, Nasreddine Bouchemel");
        json.put("email", "abdelouadoudrasmi@gmail.com, bouchemelnasreddine@gmail.com");
        json.put("github", "https://github.com/rasmi-aw");
        json.put("project_repo", "https://github.com/rasmi-aw/ontomapper");
        json.put("note", "You have to provide the api-key and username while submitting your requests");
        json.put("description", "This service is a mapping storing service that you can add to your Ontoportal instance, it uses the SSSOM standard.");
        json.put("api", "/swagger-ui/index.html#");
        return json;
    }

}

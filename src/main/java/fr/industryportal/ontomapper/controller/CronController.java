package fr.industryportal.ontomapper.controller;

import fr.industryportal.ontomapper.helpers.CronHelper;
import fr.industryportal.ontomapper.helpers.ExtractHelper;
import fr.industryportal.ontomapper.model.requests.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Nasreddine Bouchemel
 */
@RestController
@RequestMapping("/cron")
public class CronController {

    @GetMapping("")
    public String parseAllOntologies(HttpServletRequest request) {
        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();
        //CronHelper.extractAcronyms(apikey);
        CronHelper.parseAllPortalClasses(apikey, username);
        return "ok";
    }
}

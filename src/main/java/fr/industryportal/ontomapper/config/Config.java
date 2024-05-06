package fr.industryportal.ontomapper.config;

/**
 * @author Abdelwadoud Rasmi
 * Config file to hold app needed constants
 *
 */
public interface Config {
    //String SELF_URL = "http://industryportal.test.enit.fr/ontomapper/";
    String SELF_URL = "http://localhost:2500/";
    //String SELF_URL = "https://industryportal.enit.fr/ontomapper/";
    String API_URL = "http://data.industryportal.test.enit.fr/";
    //String API_URL = "https://data.industryportal.enit.fr/";
    //String API_URL = "http://localhost:8080/";
    //String API_URL = "http://data.industryportal.enit.fr/";
    //String ONTOLOGY_FOLDER = "/srv/ontoportal/ontomapper/ontologies/";
    String ONTOLOGY_FOLDER = "src/main/resources/ontologies/";
    String PROXY_HOST = "squid02.local.enit.fr";
    String PROXY_PORT = "3128";

}

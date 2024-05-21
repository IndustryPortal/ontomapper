package fr.industryportal.ontomapper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.github.cdimascio.dotenv.Dotenv;

@Component
public class AppConfig {
    private static final AppConfig instance = new AppConfig();

    private AppConfig() {
        Dotenv dotenv = Dotenv.load();
        selfUrl = dotenv.get("SELF_URL");
        apiUrl = dotenv.get("API_URL");
        ontologyFolder = dotenv.get("ONTOLOGY_FOLDER");
        proxyHost = dotenv.get("PROXY_HOST");
        proxyPort = dotenv.get("PROXY_PORT");
    }

    @Bean
    public static AppConfig getInstance() {
        return instance;
    }

    private final String selfUrl;
    private final String apiUrl;
    private final String ontologyFolder;
    private final String proxyHost;
    private final String proxyPort;


    public String getSelfUrl() {
        return selfUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getOntologyFolder() {
        return ontologyFolder;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public String getProxyPort() {
        return proxyPort;
    }
}

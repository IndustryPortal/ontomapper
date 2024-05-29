package fr.industryportal.ontomapper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {
    private static final AppConfig instance = new AppConfig();

    private AppConfig() {
    }

    @Bean
    public static AppConfig getInstance() {
        return instance;
    }

    @Value("${self.url}")
    private String selfUrl;
    @Value("${api.url}")
    private String apiUrl;
    @Value("${ontology.folder}")
    private String ontologyFolder;

    @Value("${4store.url}")
    private String tripleStoreUrl;

    @Value("${proxy.host}")
    private String proxyHost;
    @Value("${proxy.port}")
    private String proxyPort;


    public String getSelfUrl() {
        return selfUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getOntologyFolder() {
        return ontologyFolder;
    }

    public String getTripleStoreUrl() {
        return tripleStoreUrl;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public String getProxyPort() {
        return proxyPort;
    }
}

package fr.industryportal.ontomapper;

import fr.industryportal.ontomapper.config.AppConfig;
import fr.industryportal.ontomapper.helpers.TripleStoreHelper;
import fr.industryportal.ontomapper.model.repos.ContributionRepository;
import fr.industryportal.ontomapper.model.repos.ContributorRepository;
import fr.industryportal.ontomapper.model.repos.MappingRepository;
import fr.industryportal.ontomapper.model.repos.MappingSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * @author Abdelwadoud Rasmi
 * This is the main entrypoint of the app
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class OntomapperApplication extends SpringBootServletInitializer implements CommandLineRunner {

    @Autowired
    private TripleStoreHelper tripleStoreHelper;

    @Autowired
    private MappingSetRepository mappingSetRepository;

    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private ContributorRepository contributorRepository;

    @Autowired
    private ContributionRepository contributionRepository;

    static final Logger log =
            LoggerFactory.getLogger(OntomapperApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OntomapperApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(OntomapperApplication.class);
    }

    @Override
    public void run(String... args) throws Exception {

        // Set proxy settings
        System.setProperty("http.proxyHost", AppConfig.getInstance().getProxyHost());
        System.setProperty("http.proxyPort", AppConfig.getInstance().getProxyPort());
        System.setProperty("https.proxyHost", AppConfig.getInstance().getProxyHost());
        System.setProperty("https.proxyPort", AppConfig.getInstance().getProxyPort());

    //    Generating fake data
//        for (int i = 0; i < 100; i++) {
//            MappingSet set = mappingSetRepository.save(new MappingSet(
//                    (long) i,
//                    null,
//                    "set" + i,
//                    "version" + i,
//                    null,
//                    "description" + i,
//                    null,
//                    "license" + i,
//                    EntityType.RDFS_DATATYPE,
//                    "",
//                    "",
//                    EntityType.RDFS_CLASS,
//                    "",
//                    "",
//                    "",
//                    "",
//                    new Date(2023, 04, 06),
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    new Date(2023, 04, 06),
//                    null,
//                    false));
//            for (int j = 0; j < 1000; j++) {
//                Mapping mapping = mappingRepository.save(new Mapping((long) (i+j),
//                        null,
//                        i+"mapping" + j, "subject" + j, "label" + j, "category" + i, "", "", PredicateModifier.NOT, "", "", "",
//                        "", null, "", EntityType.RDFS_DATATYPE, "", "", EntityType.OWL_CLASS, "", "", "",
//                        "", MappingCardinality.MANY_TO_MANY, "", "", new Date(2023, 03, 20 ), 0, "", "", "", "",
//                        "", 0, "", "", "", "", new Date(2023, 04, 06), set,false));
//            }
//        }
//
//        for (int i = 0; i < 100; i++) {
//            Contributor contributor = contributorRepository.save(new Contributor((long) i, "contributor" + i, "label" + i, null, false));
//                contributionRepository.save(new Contribution((long) i, contributor, ContributorType.AUTHOR, mappingSetRepository.findAll(), mappingRepository.findAll(), false));
//                contributionRepository.save(new Contribution((long) i, contributor, ContributorType.CREATOR, mappingSetRepository.findAll(), null, false));
//                contributionRepository.save(new Contribution((long) i, contributor, ContributorType.REVIEWER, mappingSetRepository.findAll(), mappingRepository.findAll(), false));
//
//        }


    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

}

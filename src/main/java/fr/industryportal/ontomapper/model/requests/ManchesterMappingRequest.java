package fr.industryportal.ontomapper.model.requests;

import fr.industryportal.ontomapper.model.entities.ManchesterMapping;
import fr.industryportal.ontomapper.model.repos.ManchesterMappingRepository;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

public class ManchesterMappingRequest {


    @Id
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    private String classUri;

    @Getter
    @Setter
    private String ontologyAcronym;

    @Getter
    @Setter
    private String type;

    @Getter
    @Setter
    private String body;


    public ManchesterMapping toDbModel() {
        return new ManchesterMapping(
                id,
                classUri,
                ontologyAcronym,
                type,
                body
        );
    }

}

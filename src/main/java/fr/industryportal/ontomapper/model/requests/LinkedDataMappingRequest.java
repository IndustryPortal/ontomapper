package fr.industryportal.ontomapper.model.requests;

import fr.industryportal.ontomapper.model.entities.LinkedDataMapping;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.graph.Graph;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.List;

public class LinkedDataMappingRequest {


    @Id
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Getter
    @Setter
    @Column(updatable = false)
    private String creator;

    @Getter
    @Setter
    private List<String> relation;

    @Getter
    @Setter
    @Column(name = "source_id")
    private String sourceId;

    @Getter
    @Setter
    @Column(name = "target_id")
    private String targetId;

    @Getter
    @Setter
    @Column(name = "source_ontology_id")
    private String sourceOntologyId;

    @Getter
    @Setter
    @Column(name="source_ontology_acronym")
    private String sourceOntologyAcronym;


    @Getter
    @Setter
    @Column(name = "target_ontology_id")
    private String targetOntologyId;


    @Getter
    @Setter
    @Column(name = "target_ontology_acronym")
    private String targetOntologyAcronym;

    @Getter
    @Setter
    @Column(name = "external_mapping")
    private boolean externalMapping;

    @Getter
    @Setter
    @Column(name = "source_contact_info")
    private String sourceContactInfo;

    @Getter
    @Setter
    @Column(name = "source_name")
    private String sourceName;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String comment;

    public LinkedDataMapping toDbModel() {
        return new LinkedDataMapping(
                id,
                creator,
                relation,
                sourceId,
                targetId,
                sourceOntologyId,
                sourceOntologyAcronym,
                targetOntologyId,
                targetOntologyAcronym,
                externalMapping,
                sourceContactInfo,
                sourceName,
                name,
                comment
        );
    }

}

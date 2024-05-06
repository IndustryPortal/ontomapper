package fr.industryportal.ontomapper.model.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Entity
public class LinkedDataMapping {

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
    @ElementCollection
    private List<String> relation;

    @Getter
    @Setter
    @Column(name = "source_id", columnDefinition = "TEXT")
    private String sourceId;

    @Getter
    @Setter
    @Column(name = "target_id", columnDefinition = "TEXT")
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
    @Column(columnDefinition = "LONGTEXT")
    private String name;

    @Getter
    @Setter
    @Column(columnDefinition = "LONGTEXT")
    private String comment;


}

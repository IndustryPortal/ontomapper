package fr.industryportal.ontomapper.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.industryportal.ontomapper.model.entities.enums.EntityType;
import fr.industryportal.ontomapper.model.entities.enums.MappingCardinality;
import fr.industryportal.ontomapper.model.entities.enums.PredicateModifier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * @author Abdelwadoud Rasmi
 * Entity for a mapping
 */
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(indexes = {@Index(name = "created_by", columnList = "created_by")})
public class Mapping {

    @Id
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @Column(updatable = false)
    @JsonIgnore
    private String created_by;

    @Getter
    @Setter
    @Column(unique = true)
    private String mapping_id;

    @Getter
    @Setter
    private String subject_id;

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String subject_label;

    @Getter
    @Setter
    private String subject_category;

    @Getter
    @Setter
    private String predicate_id;

    @Getter
    @Setter
    private String predicate_label;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    private PredicateModifier predicate_modifier;

    @Getter
    @Setter
    private String object_id;

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String object_label;

    @Getter
    @Setter
    private String object_category;

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String justification;

    @Getter
    @Setter
    @ManyToMany
    private List<Contribution> contributions;

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String license;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    private EntityType subject_type;

    @Getter
    @Setter
    private String subject_source;

    @Getter
    @Setter
    private String subject_source_version;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    private EntityType object_type;

    @Getter
    @Setter
    private String object_source;

    @Getter
    @Setter
    private String object_source_version;

    @Getter
    @Setter
    private String mapping_provider;

    @Getter
    @Setter
    private String mapping_source;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    private MappingCardinality cardinality;

    @Getter
    @Setter
    private String mapping_tool;

    @Getter
    @Setter
    private String mapping_tool_version;

    @Getter
    @Setter
    @Temporal(TemporalType.TIMESTAMP)
    private Date mapping_date;

    @Getter
    @Setter
    private double confidence;

    @Getter
    @Setter
    private String subject_match_field;

    @Getter
    @Setter
    private String object_match_field;

    @Getter
    @Setter
    private String match_string;

    @Getter
    @Setter
    private String subject_preprocessing;

    @Getter
    @Setter
    private String object_preprocessing;

    @Getter
    @Setter
    private double semantic_similarity_score;

    @Getter
    @Setter
    private String semantic_similarity_measure;

    @Getter
    @Setter
    private String see_also;

    @Getter
    @Setter
    private String other;

    @Getter
    @Setter
    private String comment;

    @Getter
    @Setter
    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date created_at;

    @Getter
    @Setter
    @ManyToOne
    @JsonIgnore
    private MappingSet set;

    @Getter
    @Setter
    @JsonIgnore
    private boolean deleted;

    @Override
    public boolean equals(Object mapping) {
        if (mapping == null || !(mapping instanceof Mapping)) return false;
        Mapping m = ((Mapping) mapping);
        return (m.id != null && m.id.equals(id))
                || (m.mapping_id != null && m.mapping_id.equals(mapping_id));
    }

    @Override
    public int hashCode() {
        int k = mapping_id.length() + id.intValue() / 3;
        return id.intValue();
    }
}

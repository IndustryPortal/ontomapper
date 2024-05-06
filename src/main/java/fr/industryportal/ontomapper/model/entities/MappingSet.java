package fr.industryportal.ontomapper.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.industryportal.ontomapper.model.entities.enums.EntityType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * @author Abdelwadoud Rasmi
 * Entity for a set of mappings
 */
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(indexes = {@Index(name = "created_by", columnList = "created_by")})
public class MappingSet {

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
    @Column(unique = true, updatable = false)
    private String mapping_set_id;

    @Getter
    @Setter
    private String version;

    @Getter
    @Setter
    @OneToMany(fetch = FetchType.EAGER)
    private List<MappingSet> source;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    @ManyToMany
    @JsonIgnore
    private List<Contribution> creators;

    @Getter
    @Setter
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
    private String mapping_tool;

    @Getter
    @Setter
    @Temporal(TemporalType.TIMESTAMP)
    private Date mapping_date;

    @Getter
    @Setter
    private String subject_match_field;

    @Getter
    @Setter
    private String object_match_field;

    @Getter
    @Setter
    private String subject_preprocessing;

    @Getter
    @Setter
    private String object_preprocessing;

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

//    @Getter
//    @Setter
//    @OneToMany(mappedBy = "set")
//    @JsonIgnore
//    private List<Mapping> mappings;

    @Getter
    @Setter
    @JsonIgnore
    private boolean deleted;


    @Override
    public boolean equals(Object set) {
        if (set == null || !(set instanceof MappingSet)) return false;
        MappingSet s = ((MappingSet) set);
        return (s.id != null && s.id.equals(id))
                || (s.mapping_set_id != null && s.mapping_set_id.equals(mapping_set_id));
    }
}

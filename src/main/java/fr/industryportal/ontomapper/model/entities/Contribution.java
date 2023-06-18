package fr.industryportal.ontomapper.model.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.industryportal.ontomapper.model.entities.enums.ContributorType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

/**
 * @author Abdelwadoud Rasmi
 * Entity for a contributor, he can be an author, reviewer
 */
@AllArgsConstructor
@NoArgsConstructor
@Entity
//@Table(indexes = {@Index(name = "contributor", columnList = "contributor", unique = true)})
public class Contribution {

    @Id
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @ManyToOne
    private Contributor contributor;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    private ContributorType type;

    @Getter
    @Setter
    @ManyToMany
    private List<MappingSet> sets;

    @Getter
    @Setter
    @ManyToMany
    private List<Mapping> mappings;

    @Getter
    @Setter
    @JsonIgnore
    private boolean deleted;
}

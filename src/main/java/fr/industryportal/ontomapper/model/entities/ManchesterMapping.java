package fr.industryportal.ontomapper.model.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * @author Nasreddine Bouchemel
 * Entity for a mapping in owl manchester format
 */
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ManchesterMapping {

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
    @Column(columnDefinition = "TEXT")
    private String body;


}

package fr.industryportal.ontomapper.model.repos;


import fr.industryportal.ontomapper.model.entities.Contribution;
import fr.industryportal.ontomapper.model.entities.ManchesterMapping;
import fr.industryportal.ontomapper.model.entities.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * @author Nasreddine Bouchemel
 */
@RepositoryRestResource(exported = false)
public interface ManchesterMappingRepository extends JpaRepository<ManchesterMapping, Long> {


    @Query(value = "SELECT * FROM manchester_mapping WHERE class_uri = :cUri", nativeQuery = true)
    List<ManchesterMapping> findByStringClassUri(String cUri);


}

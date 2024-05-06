package fr.industryportal.ontomapper.model.repos;

import fr.industryportal.ontomapper.model.entities.LinkedDataMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface LinkedDataMappingRepository extends JpaRepository<LinkedDataMapping, Long> {


    List<LinkedDataMapping> findBySourceOntologyId(String sourceOntologyId);

    List<LinkedDataMapping> findBySourceId(String sourceId);


    //this method search where the source_class_id contains the acronym
    List<LinkedDataMapping> findBySourceOntologyIdContaining(String acronym);

    @Query(value="SELECT * FROM linked_data_mapping WHERE source_ontology_acronym = :acronym", nativeQuery = true)
    List<LinkedDataMapping> findBySourceOntologyAcronym(String acronym);
}

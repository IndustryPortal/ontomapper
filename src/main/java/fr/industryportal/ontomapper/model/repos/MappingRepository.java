package fr.industryportal.ontomapper.model.repos;

import fr.industryportal.ontomapper.model.entities.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * @author Abdelwadoud Rasmi
 */
@RepositoryRestResource(exported = false)
public interface MappingRepository extends JpaRepository<Mapping, Long> {

    /**
     * get a list of mappings depending based on their set_id, and "after <= id < before"
     */
    @Query(value = "SELECT * FROM Mapping m WHERE (set_id = :setId) AND (id >= :afterOrEqual)  LIMIT 50 ", nativeQuery = true)
    List<Mapping> findFiftyBySetIdAndIdAndIdAfter(long setId, long afterOrEqual);

    @Override
    @Query(value = "SELECT * FROM mapping WHERE deleted <> true ", nativeQuery = true)
    List<Mapping> findAll();

    @Query(value = "SELECT * FROM mapping m WHERE m.set_id = :setId AND m.deleted <> true", nativeQuery = true)
    List<Mapping> findAllBySetId(Long setId);


    @Transactional
    @Modifying
    @Query(value = "UPDATE mapping  SET deleted = TRUE WHERE set_id = :set_id", nativeQuery = true)
    void deleteBySetId(Long set_id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE mapping  SET deleted = TRUE WHERE id = :id", nativeQuery = true)
    void deleteById(Long id);

    @Query(value = "SELECT * FROM mapping WHERE mapping_id = :mid", nativeQuery = true)
    Mapping findByStringId(String mid);

    @Query(value = "SELECT * FROM mapping WHERE (subject_id = :cid and subject_label <> '') or (object_id = :cid and object_label <> '') ", nativeQuery = true)
    Mapping findByCLassId(String cid );


    @Query(value = "SELECT * FROM mapping WHERE mapping_id = :id", nativeQuery = true)
    Optional<Mapping> findByMappingId(String id);
}

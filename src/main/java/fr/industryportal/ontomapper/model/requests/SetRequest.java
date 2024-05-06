package fr.industryportal.ontomapper.model.requests;

import fr.industryportal.ontomapper.helpers.DBCast;
import fr.industryportal.ontomapper.model.entities.Mapping;
import fr.industryportal.ontomapper.model.entities.MappingSet;
import fr.industryportal.ontomapper.model.entities.enums.EntityType;
import fr.industryportal.ontomapper.model.repos.MappingRepository;
import fr.industryportal.ontomapper.model.repos.MappingSetRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Abdelwadoud Rasmi
 * Request model
 */
@AllArgsConstructor
@NoArgsConstructor
public class SetRequest implements DBCast<MappingSet, MappingSetRepository> {

    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String mapping_set_id;

    @Getter
    @Setter
    private String version;

    @Getter
    @Setter
    private List<Long> source = new ArrayList<>();

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private List<ContributorRequest> creators;

    @Getter
    @Setter
    private String license;

    @Getter
    @Setter
    private EntityType subject_type;

    @Getter
    @Setter
    private String subject_source;

    @Getter
    @Setter
    private String subject_source_version;

    @Getter
    @Setter
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
    private Date mapping_date;

    @Getter
    @Setter
    private List<MappingRequest> mappings;

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

    @Override
    public MappingSet toDBModel(MappingSetRepository mappingSetRepository, String createdBY) {
        return new MappingSet(id, createdBY, mapping_set_id, version,
                source == null ? null : mappingSetRepository.findAllById(source), description,
                null, license, subject_type, subject_source, subject_source_version,
                object_type, object_source, object_source_version, mapping_provider,
                mapping_tool, mapping_date, subject_match_field, object_match_field,
                subject_preprocessing, object_preprocessing, see_also, other, comment,
                new Date(), false);
    }
}

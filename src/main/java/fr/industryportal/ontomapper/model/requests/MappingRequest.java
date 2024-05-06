package fr.industryportal.ontomapper.model.requests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.industryportal.ontomapper.helpers.DBCast;
import fr.industryportal.ontomapper.model.entities.Mapping;
import fr.industryportal.ontomapper.model.entities.enums.EntityType;
import fr.industryportal.ontomapper.model.entities.enums.MappingCardinality;
import fr.industryportal.ontomapper.model.entities.enums.PredicateModifier;
import fr.industryportal.ontomapper.model.repos.MappingSetRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;
import java.util.UUID;


/**
 * @author Abdelwadoud Rasmi
 * Request model
 */
@AllArgsConstructor
@NoArgsConstructor
public class MappingRequest implements DBCast<Mapping, MappingSetRepository> {

    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String mapping_id;

    @Getter
    @Setter
    private String subject_id;

    @Getter
    @Setter
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
    private PredicateModifier predicate_modifier;

    @Getter
    @Setter
    private String object_id;

    @Getter
    @Setter
    private String object_label;

    @Getter
    @Setter
    private String object_category;

    @Getter
    @Setter
    private String justification;

    @Getter
    @Setter
    private List<ContributorRequest> contributors;

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
    private String mapping_source;

    @Getter
    @Setter
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
    private Long set_id;

    public static MappingRequest fromJSON(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, MappingRequest.class);
    }

    @Override
    public Mapping toDBModel(MappingSetRepository mappingSetRepository, String createdBy) {
        return new Mapping(id, createdBy, mapping_id, subject_id, subject_label, subject_category,
                predicate_id, predicate_label, predicate_modifier, object_id,
                object_label, object_category, justification, null,
                license, subject_type, subject_source, subject_source_version,
                object_type, object_source, object_source_version,
                mapping_provider, mapping_source, cardinality, mapping_tool,
                mapping_tool_version, mapping_date, confidence, subject_match_field,
                object_match_field, match_string, subject_preprocessing,
                object_preprocessing, semantic_similarity_score, semantic_similarity_measure,
                see_also, other, comment, new Date(),
                mappingSetRepository.findById(set_id).get(), false);
    }
}

package fr.industryportal.ontomapper.model.entities.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Abdelwadoud Rasmi
 * Enumeral values for Entity that is being mapped
 */
@AllArgsConstructor
public enum MappingCardinality {
    ONE_TO_ONE("1:1"),
    ONE_TO_MANY("1:n"),
    MANY_TO_ONE("n:1"),
    ONE_TO_NONE("1:0"),
    NONE_TO_ONE("0:1"),
    MANY_TO_MANY("n:n");

    @Getter
    private final String value;

    public String getValue() {
        return value == null || value.equals("") ? "" : value;
    }

    @Override
    public String toString() {
        return getValue();
    }


}

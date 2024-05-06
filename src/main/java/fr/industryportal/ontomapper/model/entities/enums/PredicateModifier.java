package fr.industryportal.ontomapper.model.entities.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Abdelwadoud Rasmi
 * Enumeral values for Predicate modifier
 */
@AllArgsConstructor
public enum PredicateModifier {
    NOT("Not"),
    EMPTY("");

    @Getter
    private String value;

    PredicateModifier() {
        this.value = "";
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

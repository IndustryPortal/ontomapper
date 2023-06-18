package fr.industryportal.ontomapper.model.entities.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Abdelwadoud Rasmi
 * Enumeral values for Entity that is being mapped
 */
@AllArgsConstructor
public enum EntityType {
    OWL_CLASS("owl:Class"),
    OWL_OBJECT_PROPERTY("owl:ObjectProperty"),
    OWL_DATA_PROPERTY("owl:DataProperty"),
    OWL_ANNOTATION_PROPERTY("owl:AnnotationProperty"),
    OWL_NAMED_INDIVIDUAL("owl:NamedIndividual"),
    SKOS_CONCEPT("skos:Concept"),
    RDFS_RESOURCE("rdfs:Resource"),
    RDFS_CLASS("rdfs:Class"),
    RDFS_LITERAL("rdfs:Literal"),
    RDFS_DATATYPE("rdfs:Datatype"),
    RDF_PROPERTY("rdf:Property"),
    OWL_RESTRICTION("owl:Restriction"), // new type
    XSD_NON_NEGATIVE_INTEGER("xsd:nonNegativeInteger"), // new type
    RDF_LIST("rdf:List"),
    NULL(null);

    private final String value;

    public String getValue() {
        return value == null || value.equals("") ? "" : value;
    }


    @Override
    public String toString() {
        return getValue();
    }
}

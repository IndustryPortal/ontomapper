package fr.industryportal.ontomapper.model.entities.enums;

import org.semanticweb.owlapi.model.AxiomType;

public enum MappingType {
    SUBCLASS_OF(AxiomType.SUBCLASS_OF),
    EQUIVALENT_CLASSES(AxiomType.EQUIVALENT_CLASSES),
    DISJOINT_CLASSES(AxiomType.DISJOINT_CLASSES),
    DISJOINT_UNION(AxiomType.DISJOINT_UNION);

    private final AxiomType<?> axiomType;

    MappingType(AxiomType<?> axiomType) {
        this.axiomType = axiomType;
    }

    public AxiomType<?> getAxiomType() {
        return axiomType;
    }

    public static boolean isMappingType(AxiomType<?> axiomType) {
        for (MappingType mappingType : MappingType.values()) {
            if (mappingType.getAxiomType().equals(axiomType)) {
                return true;
            }
        }
        return false;
    }

}

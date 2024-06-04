package fr.industryportal.ontomapper.helpers;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class OntologyHelper {
    public Ontology getOntologyWithModelFromFile(File file) throws OWLOntologyCreationException {
        return OntManagers.createManager().loadOntologyFromOntologyDocument(file);
    }

    public static String getClassLabel(String classUri, Ontology ontology) {
//        OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
//        OWLClass cls = dataFactory.getOWLClass(IRI.create(classUri));
//
//        ontology.getAnnotations().iterator().next().annotationValue();
//
//        for (OWLAnnotationAssertionAxiom axiom : ontology.getAnnotationAssertionAxioms(cls.getIRI())) {
//
//            if (  axiom.getProperty().isLabel()) {
//                OWLLiteral literal = (OWLLiteral) axiom.getValue();
//                String label = literal.getLiteral();
//                return label;
//
//            }
//        }
//
//        // Return the class URI as the label if no label annotation is found
//        return classUri;
        // Get the class for the given URI
        // Get the class for the given URI
        // Get the class for the given URI
 /*       OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLClass cls = dataFactory.getOWLClass(IRI.create(classUri));

        // Retrieve the annotations on the class for rdfs:label
        OWLAnnotationProperty labelProperty = dataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
        for (OWLAnnotationAssertionAxiom annotationAxiom : ontology.getAnnotationAssertionAxioms(cls.getIRI())) {
            if (annotationAxiom.getProperty().equals(labelProperty)) {
                OWLAnnotationValue value = annotationAxiom.getValue();
                if (value instanceof OWLLiteral) {
                    return ((OWLLiteral) value).getLiteral();
                }
            }
        }*/
//        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
//        OWLDataFactory dataFactory = manager.getOWLDataFactory();
//        OWLClass targetClass = dataFactory.getOWLClass(IRI.create(classUri));
//
//        for (OWLAnnotationAssertionAxiom axiom : ontology.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
//            OWLAnnotationSubject subject = axiom.getSubject();
//            if (subject instanceof IRI && subject.equals(targetClass.getIRI())) {
//                OWLAnnotationValue value = axiom.getValue();
//                if (value instanceof OWLLiteral) {
//                    return ((OWLLiteral) value).getLiteral();
//                }
//            }
//        }
//
//        return targetClass.getIRI().getFragment(); // Use the class fragment (the last part of the iri) as the label if no rdfs:label found

        Resource classResource = ontology.asGraphModel().createResource(classUri);

        StmtIterator iterator = ontology.asGraphModel().listStatements(classResource, RDFS.label, (String) null);

        while (iterator.hasNext()) {
            Statement stmt = iterator.nextStatement();
            if (stmt.getObject().isLiteral()) {
                return stmt.getObject().asLiteral().getString();
            }
        }

        // If no label is found, return null or a default value
        return classUri;
    }

}

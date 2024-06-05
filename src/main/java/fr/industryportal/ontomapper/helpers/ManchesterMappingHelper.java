package fr.industryportal.ontomapper.helpers;

import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.owlapi.axioms.DisjointClassesAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DisjointUnionAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.EquivalentClassesAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.SubClassOfAxiomImpl;
import fr.industryportal.ontomapper.model.repos.ManchesterMappingRepository;
import fr.industryportal.ontomapper.model.requests.ManchesterMappingRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.manchester.cs.owl.owlapi.OWLDisjointClassesAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDisjointUnionAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLEquivalentClassesAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

@Component
public class ManchesterMappingHelper {


    @Autowired
    private ExtractHelper extractHelper;


    public JSONArray extractManchesterMappingsByClassId(Ontology ontology, String acronym, String classUri, String ontologyFilePath, ManchesterMappingRepository repo) {
        System.out.println("======looking for mapping of class:" + classUri + " from " + acronym + " ontology");
        ExtractHelper.logger.info("======looking for mapping of class:" + classUri + " from " + acronym + " ontology");

        JSONArray result = new JSONArray();
        for (OWLAxiom axiom : ontology.getLogicalAxioms()) {

//                logical axioms:
//                SubClassOf.
//                EquivalentClasses.
//                DisjointClasses.
//                DisjointUnion
//                SubObjectPropertyOf
//                EquivalentObjectProperties
//                InverseObjectProperties
//                FunctionalObjectProperty
//                InverseFunctionalObjectProperty
//                ObjectPropertyDomain
//                ObjectPropertyRange
//                SubDataPropertyOf
//                EquivalentDataProperties
//                DataPropertyDomain
//                DataPropertyRange

            if (axiom instanceof SubClassOfAxiomImpl) {
                OWLClassExpression ce = ((SubClassOfAxiomImpl) axiom).getSubClass();

                for (OWLClass c : ce.getClassesInSignature()) {
                    if (c.getIRI().toString().equals(classUri)) {
                        JSONObject obj = new JSONObject();
                        String axiomType = axiom.getAxiomType().getName();
                        String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
                        if (axiomManchesterSyntax.equals("")) continue;
                        String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontologyFilePath, axiom);
                        if (formattedBody.contains("Prefix")) continue;
                        obj.put("classUri", classUri );
                        obj.put("ontologyAcronym", acronym);
                        obj.put("type", axiomType);
                        obj.put("body", formattedBody);
                        result.put(obj );
                        break;
                    }
                }
            } else if (axiom instanceof EquivalentClassesAxiomImpl) {
                Set<OWLClassExpression> ce = ((EquivalentClassesAxiomImpl) axiom).getClassExpressions();
                for (OWLClass c : ce.iterator().next().getClassesInSignature() ) {
                    if (c.getIRI().toString().equals(classUri)) {
                        JSONObject obj = new JSONObject();
                        String axiomType = axiom.getAxiomType().getName();
                        String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
                        if (axiomManchesterSyntax.equals("")) continue;
                        String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontologyFilePath, axiom);
                        if (formattedBody.contains("Prefix")) continue;
                        obj.put("classUri", classUri );
                        obj.put("ontologyAcronym", acronym);
                        obj.put("type", axiomType);
                        obj.put("body", formattedBody);
                        result.put(obj );
                        break;
                    }
                }
            } else if (axiom instanceof DisjointClassesAxiomImpl) {
                Set<OWLClassExpression> ce = ((DisjointClassesAxiomImpl) axiom).getClassExpressions();
                for (OWLClass c : ce.iterator().next().getClassesInSignature() ) {
                    if (c.getIRI().toString().equals(classUri)) {
                        JSONObject obj = new JSONObject();
                        String axiomType = axiom.getAxiomType().getName();
                        String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
                        if (axiomManchesterSyntax.equals("")) continue;
                        String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontologyFilePath, axiom);
                        if (formattedBody.contains("Prefix")) continue;
                        obj.put("classUri", classUri );
                        obj.put("ontologyAcronym", acronym);
                        obj.put("type", axiomType);
                        obj.put("body", formattedBody);
                        result.put(obj );
                        break;
                    }
                }
            } else if (axiom instanceof DisjointUnionAxiomImpl) {
                if (((DisjointUnionAxiomImpl) axiom).getOWLClass().getIRI().toString().equals(classUri)) {
                    JSONObject obj = new JSONObject();
                    String axiomType = axiom.getAxiomType().getName();
                    String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
                    if (axiomManchesterSyntax.equals("")) continue;
                    String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontologyFilePath, axiom);
                    if (formattedBody.contains("Prefix")) continue;
                    obj.put("classUri", classUri );
                    obj.put("ontologyAcronym", acronym);
                    obj.put("type", axiomType);
                    obj.put("body", formattedBody);
                    result.put(obj );
                }


            }

//            if (axiom instanceof OWLSubClassOfAxiomImpl) {
//
//                OWLClassExpression ce = ((OWLSubClassOfAxiomImpl) axiom).getSubClass();
//
//                for (OWLClass c : ce.getClassesInSignature()) {
//                    if (c.getIRI().toString().equals(classUri)) {
//                        JSONObject obj = new JSONObject();
//                        String axiomType = axiom.getAxiomType().getName();
//                        String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
//                        if (axiomManchesterSyntax.equals("")) continue;
//                        String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontologyFilePath, axiom);
//                        if (formattedBody.contains("Prefix")) continue;
//                        obj.put("classUri", classUri );
//                        obj.put("ontologyAcronym", acronym);
//                        obj.put("type", axiomType);
//                        obj.put("body", formattedBody);
//                        result.put(obj );
//                        break;
//                    }
//                }
//            }
//            else if (axiom instanceof OWLEquivalentClassesAxiomImpl) {
//                Set<OWLClassExpression> ce = ((OWLEquivalentClassesAxiomImpl) axiom).getClassExpressions();
//                for (OWLClass c : ce.iterator().next().getClassesInSignature() ) {
//                    if (c.getIRI().toString().equals(classUri)) {
//                        JSONObject obj = new JSONObject();
//                        String axiomType = axiom.getAxiomType().getName();
//                        String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
//                        if (axiomManchesterSyntax.equals("")) continue;
//                        String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontologyFilePath, axiom);
//                        if (formattedBody.contains("Prefix")) continue;
//                        obj.put("classUri", classUri );
//                        obj.put("ontologyAcronym", acronym);
//                        obj.put("type", axiomType);
//                        obj.put("body", formattedBody);
//                        result.put(obj );
//                        break;
//                    }
//                }
//            }
//            else if (axiom instanceof OWLDisjointClassesAxiomImpl) {
//                Set<OWLClassExpression> ce = ((OWLDisjointClassesAxiomImpl) axiom).getClassExpressions();
//                for (OWLClass c : ce.iterator().next().getClassesInSignature() ) {
//                    if (c.getIRI().toString().equals(classUri)) {
//                        JSONObject obj = new JSONObject();
//                        String axiomType = axiom.getAxiomType().getName();
//                        String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
//                        if (axiomManchesterSyntax.equals("")) continue;
//                        String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontologyFilePath, axiom);
//                        if (formattedBody.contains("Prefix")) continue;
//                        obj.put("classUri", classUri );
//                        obj.put("ontologyAcronym", acronym);
//                        obj.put("type", axiomType);
//                        obj.put("body", formattedBody);
//                        result.put(obj );
//                        break;
//                    }
//                }
//            }
//            else if (axiom instanceof OWLDisjointUnionAxiomImpl) {
//                if (((OWLDisjointUnionAxiomImpl) axiom).getOWLClass().getIRI().toString().equals(classUri)) {
//                    JSONObject obj = new JSONObject();
//                    String axiomType = axiom.getAxiomType().getName();
//                    String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
//                    if (axiomManchesterSyntax.equals("")) continue;
//                    String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontologyFilePath, axiom);
//                    if (formattedBody.contains("Prefix")) continue;
//                    obj.put("classUri", classUri );
//                    obj.put("ontologyAcronym", acronym);
//                    obj.put("type", axiomType);
//                    obj.put("body", formattedBody);
//                    result.put(obj );
//                }
//            }

//                Set<OWLClass> classes = axiom.getClassesInSignature();
//                for (OWLClass c : classes) {
//                    if (c.getIRI().toString().equals(classUri)) {
//                        JSONObject obj = new JSONObject();
//                        String axiomManchesterSyntax = convertToManchesterSyntax(axiom, classUri);
//                        if (axiomManchesterSyntax.equals("")) continue;
//                        obj.put("type", axiom.getAxiomType().getName());
//                        obj.put("body", axiomManchesterSyntax);
//                        result.put(obj );
//                    }
//                }
        }
        System.out.println("found " + result.length() + " mapping(s) for class " + classUri + " from " + acronym + " ontology");
        ExtractHelper.logger.info("found " + result.length() + " mapping(s) for class " + classUri + " from " + acronym + " ontology");
        storeManchesterMappings(repo, result);
        return result;
    }

    public static int storeManchesterMappings(ManchesterMappingRepository repo,  JSONArray mappings) {
        for (int i = 0; i <= mappings.length()-1; i++ ) {
            JSONObject obj = mappings.getJSONObject(i);

            ManchesterMappingRequest mapping = new ManchesterMappingRequest();

            mapping.setClassUri(obj.getString("classUri"));
            mapping.setOntologyAcronym(obj.getString("ontologyAcronym"));
            mapping.setType(obj.getString("type"));
            mapping.setBody(obj.getString("body"));



            repo.save(mapping.toDbModel());
            ExtractHelper.logger.info("mapping for " + mapping.getClassUri()  + " saved to db");

        }
        return 1;
    }

}

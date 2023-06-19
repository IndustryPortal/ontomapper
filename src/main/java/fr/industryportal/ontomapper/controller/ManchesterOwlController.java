package fr.industryportal.ontomapper.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.industryportal.ontomapper.helpers.ExtractHelper;
import fr.industryportal.ontomapper.model.entities.ManchesterMapping;
import fr.industryportal.ontomapper.model.repos.ManchesterMappingRepository;
import fr.industryportal.ontomapper.model.requests.ManchesterMappingRequest;
import fr.industryportal.ontomapper.model.requests.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDisjointClassesAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDisjointUnionAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLEquivalentClassesAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * @author Nasreddine Bouchemel
 */
@RestController
@RequestMapping("/manchester")
public class ManchesterOwlController {


    @Autowired
    private ManchesterMappingRepository manchesterMappingRepository;

    @GetMapping("/{acronym}/extract")
    public String extractClassRelationsByCLassId(HttpServletRequest request, @PathVariable String acronym, @RequestParam String classUri) {
        ExtractHelper extractHelper = new ExtractHelper();
        classUri = classUri.trim();
        User user = ((User) request.getAttribute("user"));
        String apikey = user.getApikey();
        String username = user.getUsername();
        String filePath = extractHelper.downloadOntologyFile(apikey, acronym);
        //String filePath = Config.ONTOLOGY_FOLDER + "go.owl";
        if (filePath == null ) {
            JSONObject o = new JSONObject();
            o.put("message", "error in getting ontology source file from portal");
            return o.toString();
        }
        OntologyConfigurator configurator = new OntologyConfigurator();
        configurator.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        manager.setOntologyConfigurator(configurator);
        OWLOntology ontology = null;

        JSONArray result = new JSONArray();
        try {
            ontology = manager.loadOntologyFromOntologyDocument(new File(filePath));

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

                if (axiom instanceof OWLSubClassOfAxiomImpl) {

                    OWLClassExpression ce = ((OWLSubClassOfAxiomImpl) axiom).getSubClass();

                    for (OWLClass c : ce.getClassesInSignature()) {
                        if (c.getIRI().toString().equals(classUri)) {
                            JSONObject obj = new JSONObject();
                            String axiomType = axiom.getAxiomType().getName();
                            String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
                            if (axiomManchesterSyntax.equals("")) continue;
                            obj.put("classUri", classUri );
                            obj.put("ontologyAcronym", acronym);
                            obj.put("type", axiomType);
                            String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontology, axiom);
                            obj.put("body", formattedBody);
                            result.put(obj );
                            break;
                        }
                    }
                }
                else if (axiom instanceof OWLEquivalentClassesAxiomImpl) {
                    Set<OWLClassExpression> ce = ((OWLEquivalentClassesAxiomImpl) axiom).getClassExpressions();
                    for (OWLClass c : ce.iterator().next().getClassesInSignature() ) {
                        if (c.getIRI().toString().equals(classUri)) {
                            JSONObject obj = new JSONObject();
                            String axiomType = axiom.getAxiomType().getName();
                            String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
                            if (axiomManchesterSyntax.equals("")) continue;
                            obj.put("classUri", classUri );
                            obj.put("ontologyAcronym", acronym);
                            obj.put("type", axiomType);
                            String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontology, axiom);
                            obj.put("body", formattedBody);
                            result.put(obj );
                            break;
                        }
                    }
                }
                else if (axiom instanceof OWLDisjointClassesAxiomImpl) {
                    Set<OWLClassExpression> ce = ((OWLDisjointClassesAxiomImpl) axiom).getClassExpressions();
                    for (OWLClass c : ce.iterator().next().getClassesInSignature() ) {
                        if (c.getIRI().toString().equals(classUri)) {
                            JSONObject obj = new JSONObject();
                            String axiomType = axiom.getAxiomType().getName();
                            String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
                            if (axiomManchesterSyntax.equals("")) continue;
                            obj.put("classUri", classUri );
                            obj.put("ontologyAcronym", acronym);
                            obj.put("type", axiomType);
                            String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontology, axiom);
                            obj.put("body", formattedBody);
                            result.put(obj );
                            break;
                        }
                    }
                }
                else if (axiom instanceof OWLDisjointUnionAxiomImpl) {
                    if (((OWLDisjointUnionAxiomImpl) axiom).getOWLClass().getIRI().toString().equals(classUri)) {
                        JSONObject obj = new JSONObject();
                        String axiomType = axiom.getAxiomType().getName();
                        String axiomManchesterSyntax = extractHelper.convertToManchesterSyntax(axiom, classUri, axiomType);
                        if (axiomManchesterSyntax.equals("")) continue;
                        obj.put("classUri", classUri );
                        obj.put("ontologyAcronym", acronym);
                        obj.put("type", axiomType);
                        String formattedBody = extractHelper.replaceClassURIsWithLabels(axiomManchesterSyntax, ontology, axiom);
                        obj.put("body", formattedBody);
                        result.put(obj );
                    }
                }

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

        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        //saving mapping before returning them
        for (int i = 0; i <= result.length()-1; i++ ) {
            JSONObject obj = result.getJSONObject(i);

            ManchesterMappingRequest mapping = new ManchesterMappingRequest();

            mapping.setClassUri(obj.getString("classUri"));
            mapping.setOntologyAcronym(obj.getString("ontologyAcronym"));
            mapping.setType(obj.getString("type"));
            mapping.setBody(obj.getString("body"));



            manchesterMappingRepository.save(mapping.toDbModel());

        }

        return  result.toString();
    }

    @GetMapping("/{acronym}/getClassRelations")
    public String getRelationsByClassId(HttpServletRequest request, @PathVariable String acronym, @RequestParam String classUri) {

        List<ManchesterMapping> mappings = new ArrayList<>();

        mappings.addAll(manchesterMappingRepository.findByStringClassUri(classUri));

        if (mappings.isEmpty()) {
            return "not found";
        } else {
            //return the mappings table in a json format
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String jsonMappins = objectMapper.writeValueAsString(mappings);
                return  jsonMappins;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "error";
            }
        }

    }

}

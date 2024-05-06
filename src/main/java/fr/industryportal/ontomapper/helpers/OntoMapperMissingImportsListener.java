package fr.industryportal.ontomapper.helpers;

import org.semanticweb.owlapi.model.MissingImportEvent;
import org.semanticweb.owlapi.model.MissingImportListener;

public class OntoMapperMissingImportsListener implements MissingImportListener {
    @Override
    public void importMissing(MissingImportEvent event) {
        System.out.println("===missing import:" + event.getImportedOntologyURI().toString());
    }
}

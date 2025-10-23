package com.gointerop.fhir.converter;

import java.util.List;

import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.springframework.stereotype.Component;

import com.gointerop.fhir.model.Concept;
import com.gointerop.fhir.model.ConceptName;
import com.gointerop.fhir.model.Source;

@Component
public class ConceptConceptDefinitionComponentConverter {
    public ConceptDefinitionComponent toConceptDefinitionComponent(Source source, Concept concept, List<ConceptName> conceptNames) {
        ConceptDefinitionComponent conceptDefinitionComponent = new ConceptDefinitionComponent();
        conceptDefinitionComponent.setCode(concept.getId());

        // Protege se nomes forem nulos ou vazios
        if (conceptNames != null && !conceptNames.isEmpty()) {
            // Tenta preferido
            for (ConceptName conceptName : conceptNames) {
                if (conceptName.getLocale().equals(source.getDefaultLocale())) {
                    conceptDefinitionComponent.setDisplay(conceptName.getName());
                    break;
                }
            }
            // Se não encontrou preferido, pega o primeiro disponível
            if (conceptDefinitionComponent.getDisplay() == null) {
                conceptDefinitionComponent.setDisplay(conceptNames.get(0).getName());
            }

            // Designations: outros idiomas
            for (ConceptName conceptName : conceptNames) {
                if (!conceptName.getLocale().equals(source.getDefaultLocale())) {
                    conceptDefinitionComponent.addDesignation()
                            .setLanguage(conceptName.getLocale())
                            .setValue(conceptName.getName());
                }
            }
        } else {
            conceptDefinitionComponent.setDisplay(concept.getDisplayName());
        }

        return conceptDefinitionComponent;
    }

}

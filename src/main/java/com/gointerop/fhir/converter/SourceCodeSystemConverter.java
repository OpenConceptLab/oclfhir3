package com.gointerop.fhir.converter;

import java.time.Instant;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Narrative;
import org.springframework.stereotype.Component;

import com.gointerop.fhir.model.Source;

@Component
public class SourceCodeSystemConverter {

    public CodeSystem toCodeSystem(Source source) {
        CodeSystem cs = new CodeSystem();

        // ID interno no servidor FHIR
        cs.setId(source.getId());

        // URL canônica
        cs.setUrl(source.getCanonicalUrl());

        cs.setName(source.getName());
        cs.setTitle(source.getFullName() != null ? source.getFullName() : source.getName());
        cs.setVersion(source.getVersion());
        cs.setStatus(PublicationStatus.ACTIVE);
        cs.setExperimental(Boolean.TRUE.equals(source.getExperimental()));
        cs.setDescription(source.getDescription());
        cs.setPublisher(source.getOwner());

        // ✅ Set caseSensitive explicitamente (resolve Warning 2)
        cs.setCaseSensitive(source.getCaseSensitive() != null ? source.getCaseSensitive() : Boolean.TRUE);

        // ✅ Add Narrative (resolve Warning dom-6)
        cs.setText(generateNarrative(cs));

        // Convert String to Date for setLastUpdated
        if (source.getUpdatedAt() != null && !source.getUpdatedAt().isEmpty()) {
            try {
                Instant instant = Instant.parse(source.getUpdatedAt());
                cs.getMeta().setLastUpdated(java.util.Date.from(instant));
            } catch (Exception e) {
                System.out.println("ERROR: Invalid date format for source " + source.getId() + " updatedAt: " + source.getUpdatedAt());
            }
        }

        // Jurisdiction (opcional)
        /*if (source.getJurisdiction() != null && !source.getJurisdiction().isEmpty()) {
            cs.addJurisdiction(new CodeableConcept().setText(source.getJurisdiction().toString()));
        }*/

        // Contact (se existir)
        /*if (source.getContact() != null && !source.getContact().isEmpty()) {
            ContactDetail contact = new ContactDetail();
            Object name = source.getContact().get("name");
            Object email = source.getContact().get("email");
            if (name != null) {
                contact.setName(name.toString());
            }
            if (email != null) {
                contact.addTelecom(new ContactPoint()
                        .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                        .setValue(email.toString()));
            }
            cs.addContact(contact);
        }*/

        if (source.getDefaultLocale() != null) cs.setLanguage(source.getDefaultLocale());
        cs.setContent(CodeSystemContentMode.NOTPRESENT);

        return cs;
    }

    /**
     * 🔥 Geração de Narrative simples para atender o DOM-6
     */
    private Narrative generateNarrative(CodeSystem cs) {
        Narrative narrative = new Narrative();
        narrative.setStatus(Narrative.NarrativeStatus.GENERATED);

        String div = String.format(
                "<div xmlns=\"http://www.w3.org/1999/xhtml\">" +
                        "<p><b>CodeSystem: %s</b></p>" +
                        "<p>Title: %s</p>" +
                        "<p>Version: %s</p>" +
                        "<p>URL: %s</p>" +
                        "<p>Description: %s</p>" +
                        "</div>",
                cs.getName(),
                cs.getTitle() != null ? cs.getTitle() : "",
                cs.getVersion() != null ? cs.getVersion() : "",
                cs.getUrl(),
                cs.getDescription() != null ? cs.getDescription() : "No description"
        );

        narrative.setDivAsString(div);
        return narrative;
    }
}

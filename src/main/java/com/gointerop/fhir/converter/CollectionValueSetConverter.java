package com.gointerop.fhir.converter;

import com.gointerop.fhir.model.Collection;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CollectionValueSetConverter {

    public ValueSet toValueSet(Collection collection) {
        ValueSet vs = new ValueSet();

        vs.setId(collection.getId());
        vs.setUrl(collection.getCanonicalUrl());
        vs.setName(collection.getName());
        vs.setTitle(collection.getFullName() != null ? collection.getFullName() : collection.getName());
        vs.setVersion(collection.getVersion());
        vs.setStatus(PublicationStatus.ACTIVE);
        vs.setDescription(collection.getDescription());
        vs.setPublisher(collection.getPublisher() != null ? collection.getPublisher() : collection.getOwner());

        // Narrative para dom-6
        vs.setText(generateNarrative(vs));

        // Meta updatedOn
        if (collection.getUpdatedOn() != null) {
            Instant instant = Instant.parse(collection.getUpdatedOn());
            vs.getMeta().setLastUpdated(java.util.Date.from(instant));
        }

        // Jurisdiction
        if (collection.getJurisdiction() != null) {
            vs.addJurisdiction().setText(collection.getJurisdiction().toString());
        }

        // Contact
        if (collection.getContact() != null && !collection.getContact().isEmpty()) {
            ContactDetail contact = new ContactDetail();
            Object name = ((java.util.Map<?, ?>) collection.getContact()).get("name");
            Object email = ((java.util.Map<?, ?>) collection.getContact()).get("email");
            if (name != null) {
                contact.setName(name.toString());
            }
            if (email != null) {
                contact.addTelecom(new ContactPoint()
                        .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                        .setValue(email.toString()));
            }
            vs.addContact(contact);
        }

        if(collection.getDefaultLocale() != null) vs.setLanguage(collection.getDefaultLocale());

        return vs;
    }

    private Narrative generateNarrative(ValueSet vs) {
        Narrative narrative = new Narrative();
        narrative.setStatus(Narrative.NarrativeStatus.GENERATED);

        String div = String.format(
                "<div xmlns=\"http://www.w3.org/1999/xhtml\">" +
                        "<p><b>ValueSet: %s</b></p>" +
                        "<p>Title: %s</p>" +
                        "<p>Version: %s</p>" +
                        "<p>URL: %s</p>" +
                        "<p>Description: %s</p>" +
                        "</div>",
                vs.getName(),
                vs.getTitle() != null ? vs.getTitle() : "",
                vs.getVersion() != null ? vs.getVersion() : "",
                vs.getUrl(),
                vs.getDescription() != null ? vs.getDescription() : "No description"
        );

        narrative.setDivAsString(div);
        return narrative;
    }
}

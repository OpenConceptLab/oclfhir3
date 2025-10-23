package com.gointerop.fhir.converter;

import java.time.Instant;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Narrative;
import org.springframework.stereotype.Component;

import com.gointerop.fhir.model.Organization;

@Component
public class OrgOrganizationConverter {
    public org.hl7.fhir.r4.model.Organization toFhirOrganization(Organization org) {
        org.hl7.fhir.r4.model.Organization fhirOrg = new org.hl7.fhir.r4.model.Organization();

        fhirOrg.setId(org.getUuid() != null ? org.getUuid() : org.getId());
        fhirOrg.setName(org.getName());

        fhirOrg.getMeta().setLastUpdated(java.util.Date.from(Instant.parse(org.getUpdatedOn())));

        fhirOrg.setActive(true);

        // Narrative para dom-6
        Narrative narrative = new Narrative();
        narrative.setStatus(Narrative.NarrativeStatus.GENERATED);

        String div = String.format(
                "<div xmlns=\"http://www.w3.org/1999/xhtml\">" +
                        "<p><b>Organization: %s</b></p>" +
                        "<p>Company: %s</p>" +
                        "<p>Website: %s</p>" +
                        "<p>Location: %s</p>" +
                        "</div>",
                org.getName(),
                org.getCompany(),
                org.getWebsite(),
                org.getLocation());
        narrative.setDivAsString(div);
        fhirOrg.setText(narrative);

        if (org.getWebsite() != null) {
            fhirOrg.addTelecom().setSystem(ContactPoint.ContactPointSystem.URL).setValue(org.getWebsite());
        }

        if (org.getLocation() != null) {
            fhirOrg.addAddress(new Address().setText(org.getLocation()));
        }

        return fhirOrg;
    }
}

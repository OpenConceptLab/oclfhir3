package com.gointerop.fhir.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.TerminologyCapabilities;

import ca.uhn.fhir.model.api.annotation.ChildOrder;
import ca.uhn.fhir.model.api.annotation.ResourceDef;

@ResourceDef(name="TerminologyCapabilities", profile="http://hl7.org/fhir/StructureDefinition/TerminologyCapabilities")
@ChildOrder(names={"url", "version", "name", "title", "status", "experimental", "date", "publisher", "contact", "description", "useContext", "jurisdiction", "purpose", "copyright", "kind", "software", "implementation", "lockedDate", "codeSystem", "expansion", "codeSearch", "validateCode", "translation", "closure"})
public class FHIRTerminologyCapabilities extends TerminologyCapabilities implements IBaseConformance, Serializable {

	private static final long serialVersionUID = 1L;
	public FHIRTerminologyCapabilities withDefaults(List<TerminologyCapabilitiesCodeSystemComponent> codeSystems) {
		setContact();
		setCodeSystem(codeSystems);
		setName("OclOnFhirTerminologyService");
		setStatus(PublicationStatus.DRAFT);
		setTitle("OCL on FHIR Terminology Service");
		setVersion("1.0.0");
		return this;
	}

	private void setContact() {
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setSystem(ContactPointSystem.EMAIL);
		contactPoint.setValue("contato@gointerop.com");
		ContactDetail contactDetail = new ContactDetail();
		contactDetail.addTelecom(contactPoint);
		setContact(Collections.singletonList(contactDetail));
	}

}
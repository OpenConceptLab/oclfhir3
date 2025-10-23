package com.gointerop.fhir.service;

import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.TerminologyCapabilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gointerop.fhir.converter.SourceFHIRTerminologyCapabilitiesConverter;
import com.gointerop.fhir.model.FHIRTerminologyCapabilities;
import com.gointerop.fhir.repository.SourceRepository;

@Service
public class TerminologyCapabilitiesService extends TerminologyCapabilities implements IBaseConformance {

	private static final long serialVersionUID = 1L;

	@Autowired
	SourceRepository sourceRepository;

	@Autowired
	SourceFHIRTerminologyCapabilitiesConverter sourceFHIRTerminologyCapabilitiesConverter;

	public FHIRTerminologyCapabilities getTerminologyCapabilities() {
		FHIRTerminologyCapabilities terminologyCapabilities = new FHIRTerminologyCapabilities();
		terminologyCapabilities.withDefaults(sourceRepository.fetchAllSources()
				.stream()
				.filter(source -> source.getCanonicalUrl() != null && !source.getCanonicalUrl().isEmpty())
				.map(sourceFHIRTerminologyCapabilitiesConverter::toTerminologyCapabilitiesCodeSystemComponent)
				.collect(Collectors.toList()));
		
		return terminologyCapabilities;
	}
}
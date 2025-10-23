package com.gointerop.fhir.converter;

import org.hl7.fhir.r4.model.TerminologyCapabilities.TerminologyCapabilitiesCodeSystemComponent;
import org.hl7.fhir.r4.model.TerminologyCapabilities.TerminologyCapabilitiesCodeSystemVersionComponent;
import org.springframework.stereotype.Component;

import com.gointerop.fhir.model.Source;

@Component
public class SourceFHIRTerminologyCapabilitiesConverter {
    public TerminologyCapabilitiesCodeSystemComponent toTerminologyCapabilitiesCodeSystemComponent(Source source) {
        TerminologyCapabilitiesCodeSystemComponent codeSystemComponent = new TerminologyCapabilitiesCodeSystemComponent();
        
        codeSystemComponent.setUri(source.getCanonicalUrl());

        TerminologyCapabilitiesCodeSystemVersionComponent versionComponent = new TerminologyCapabilitiesCodeSystemVersionComponent();
        versionComponent.setCode(source.getVersion());

        codeSystemComponent.addVersion(versionComponent);

        return codeSystemComponent;
    }
}

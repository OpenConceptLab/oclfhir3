package com.gointerop.fhir.utils;

import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.parser.IParser;

@Component
public class FHIRUtil {
    private FhirContext fhirContext;
    private IFhirPath iFhirPath;
    private IParser iParser;
    

    public FHIRUtil() {
        this.fhirContext = FhirContext.forR4();        
        this.iParser = this.fhirContext.newJsonParser();
        this.iFhirPath = this.fhirContext.newFhirPath();
    }

    public FhirContext getFhirContext() {
        return this.fhirContext;
    }

    public IParser getIParser() {
        return this.iParser;
    }

    public IFhirPath getIFhirPath() {
        return this.iFhirPath;
    }
}

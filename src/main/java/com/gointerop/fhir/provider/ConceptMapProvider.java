package com.gointerop.fhir.provider;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.gointerop.fhir.service.MappingService;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

@Controller
public class ConceptMapProvider {

    @Autowired
    private MappingService service;

    @Operation(name = "$translate", idempotent = true, type = ConceptMap.class)
    public IBaseResource translate(
            RequestDetails requestDetails,
            @OperationParam(name = "url") UriType url,
            @OperationParam(name = "conceptMap") ConceptMap conceptMap,
            @OperationParam(name = "conceptMapVersion") StringType conceptMapVersion,
            @OperationParam(name = "code") CodeType code,
            @OperationParam(name = "system") UriType system,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "coding") Coding coding,
            @OperationParam(name = "codeableConcept") CodeableConcept codeableConcept,
            @OperationParam(name = "target") UriType target,
            @OperationParam(name = "targetsystem") UriType targetSystem,
            @OperationParam(name = "reverse") BooleanType reverse,
            @OperationParam(name = "x-system-cache-id") StringType xSystemCacheId) {

        return service.translate(
                url != null ? url.getValue() : null,
                conceptMap,
                conceptMapVersion != null ? conceptMapVersion.getValue() : null,
                code != null ? code.getValue() : null,
                system != null ? system.getValue() : null,
                version != null ? version.getValue() : null,
                coding,
                codeableConcept,
                target != null ? target.getValue() : null,
                targetSystem != null ? targetSystem.getValue() : null,
                reverse != null ? reverse.booleanValue() : null,
                xSystemCacheId != null ? xSystemCacheId.getValue() : null);
    }
}

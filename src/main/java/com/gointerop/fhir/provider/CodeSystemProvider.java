package com.gointerop.fhir.provider;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.gointerop.fhir.service.SourceService;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@Controller
public class CodeSystemProvider {

    @Autowired
    private SourceService service;

    @Read(type = CodeSystem.class)
    public CodeSystem readCodeSystem(
            @IdParam IdType theId,
            RequestDetails requestDetails) {
        System.out.println("Reading CodeSystem with ID: " + theId.getIdPart());
        CodeSystem codeSystem = service.fetchCodeSystemById(theId.getIdPart());
        if (codeSystem == null) {
            throw new ResourceNotFoundException("CodeSystem not found for ID: " + theId.getIdPart());
        }

        String accept = requestDetails.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            Narrative narrative = new Narrative();
            narrative.setStatus(NarrativeStatus.GENERATED);
            String html = String.format(
                    "<div xmlns=\"http://www.w3.org/1999/xhtml\">" +
                            "<h1>%s</h1>" +
                            "<p>Canonical URL: %s</p>" +
                            "<p>Version: %s</p>" +
                            "<p>Description: %s</p>" +
                            "</div>",
                    codeSystem.getName(),
                    codeSystem.getUrl(),
                    codeSystem.getVersion(),
                    codeSystem.getDescription());
            narrative.setDivAsString(html);
            codeSystem.setText(narrative);
        }

        return codeSystem;
    }

    @Search
    public List<CodeSystem> search(
            RequestDetails requestDetails,
            @OptionalParam(name = CodeSystem.SP_URL) UriParam url,
            @OptionalParam(name = CodeSystem.SP_SYSTEM) UriParam system,
            @OptionalParam(name = CodeSystem.SP_NAME) StringParam name,
            @OptionalParam(name = CodeSystem.SP_VERSION) TokenParam version,
            @OptionalParam(name = CodeSystem.SP_IDENTIFIER) TokenParam identifier,
            @OptionalParam(name = CodeSystem.SP_PUBLISHER) StringParam publisher,
            @OptionalParam(name = CodeSystem.SP_TITLE) StringParam title,
            @OptionalParam(name = CodeSystem.SP_DESCRIPTION) StringParam description,
            @OptionalParam(name = CodeSystem.SP_JURISDICTION) TokenParam jurisdiction,
            @OptionalParam(name = CodeSystem.SP_STATUS) TokenParam status,
            @OptionalParam(name = CodeSystem.SP_DATE) DateParam date,
            @OptionalParam(name = CodeSystem.SP_CONTENT_MODE) TokenParam contentMode,
            @OptionalParam(name = "x-system-cache-id") StringType xSystemCacheId) {
        System.out.println("Searching CodeSystem with parameters: " +
                "url=" + (url != null ? url.getValue() : "null") +
                ", system=" + (system != null ? system.getValue() : "null") +
                ", name=" + (name != null ? name.getValue() : "null") +
                ", version=" + (version != null ? version.getValue() : "null") +
                ", identifier=" + (identifier != null ? identifier.getValue() : "null") +
                ", publisher=" + (publisher != null ? publisher.getValue() : "null") +
                ", title=" + (title != null ? title.getValue() : "null") +
                ", description=" + (description != null ? description.getValue() : "null") +
                ", jurisdiction=" + (jurisdiction != null ? jurisdiction.getValue() : "null") +
                ", status=" + (status != null ? status.getValue() : "null") +
                ", date=" + (date != null ? date.getValueAsString() : "null") +
                ", contentMode=" + (contentMode != null ? contentMode.getValue() : "null"));
        return service.searchCodeSystems(
                url != null ? url.getValue() : null,
                system != null ? system.getValue() : null,
                name != null ? name.getValue() : null,
                version != null ? version.getValue() : null,
                identifier != null ? identifier.getValue() : null,
                publisher != null ? publisher.getValue() : null,
                title != null ? title.getValue() : null,
                description != null ? description.getValue() : null,
                jurisdiction != null ? jurisdiction.getValue() : null,
                status != null ? status.getValue() : null,
                date != null ? date.getValue() : null,
                contentMode != null ? contentMode.getValue() : null,
                xSystemCacheId != null ? xSystemCacheId.getValue() : null);
    }

    @Operation(name = "$lookup", idempotent = true, type = CodeSystem.class)
    public IBaseResource lookup(
            RequestDetails requestDetails,
            @OperationParam(name = "system") UriType system,
            @OperationParam(name = "code") CodeType code,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "displayLanguage") StringType displayLanguage,
            @OperationParam(name = "property") List<StringType> properties,
            @OperationParam(name = "x-system-cache-id") StringType xSystemCacheId) {

        System.out.println("Performing CodeSystem/$lookup with parameters: " +
                "system=" + (system != null ? system.getValue() : "null") +
                ", code=" + (code != null ? code.getValue() : "null") +
                ", version=" + (version != null ? version.getValue() : "null") +
                ", properties=" + (properties != null ? properties.stream().map(StringType::getValue).collect(Collectors.toList()) : "null"));

        if (system == null || code == null) {
            throw new IllegalArgumentException("Both 'system' and 'code' are required for $lookup");
        }

        return service.lookup(
                system != null ? system.getValue() : null,
                code != null ? code.getValue() : null,
                version != null ? version.getValue() : null,
                displayLanguage != null ? displayLanguage.getValue() : null,
                properties,
                xSystemCacheId != null ? xSystemCacheId.getValue() : null);
    }

    @Operation(name = "$validate-code", idempotent = true, type = CodeSystem.class)
    public IBaseResource validateCode(
            RequestDetails requestDetails,
            @OperationParam(name = "url") UriType url,
            @OperationParam(name = "codeSystem") CodeSystem codeSystem,
            @OperationParam(name = "code") StringType code,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "display") StringType display,
            @OperationParam(name = "coding") Coding coding,
            @OperationParam(name = "codeableConcept") CodeableConcept codeableConcept,
            @OperationParam(name = "displayLanguage") StringType displayLanguage,
            @OperationParam(name = "x-system-cache-id") StringType xSystemCacheId) {

        System.out.println("[DEBUG] Performing CodeSystem/$validate-code:");
        System.out.println("  - url:                " + (url != null ? url.getValue() : "null"));
        System.out.println("  - codeSystem:         " + (codeSystem != null ? codeSystem.getUrl() : "null"));
        System.out.println("  - code (raw):         " + (code != null ? code.getValue() : "null"));
        System.out.println("  - version:            " + (version != null ? version.getValue() : "null"));
        System.out.println("  - display:            " + (display != null ? display.getValue() : "null"));
        System.out.println("  - displayLanguage:    " + (displayLanguage != null ? displayLanguage.getValue() : "null"));
        System.out.println("  - x-system-cache-id:  " + (xSystemCacheId != null ? xSystemCacheId.getValue() : "null"));

        if (coding != null) {
            System.out.println("  - coding.code:        " + coding.getCode());
            System.out.println("  - coding.system:      " + coding.getSystem());
            System.out.println("  - coding.version:     " + coding.getVersion());
            System.out.println("  - coding.display:     " + coding.getDisplay());
        }

        if (codeableConcept != null) {
            System.out.println("  - codeableConcept.text: " + codeableConcept.getText());
            List<Coding> codings = codeableConcept.getCoding();
            for (int i = 0; i < codings.size(); i++) {
                Coding c = codings.get(i);
                System.out.println("    - coding[" + i + "].code:    " + c.getCode());
                System.out.println("    - coding[" + i + "].system:  " + c.getSystem());
                System.out.println("    - coding[" + i + "].display: " + c.getDisplay());
            }
        }

        return service.validateCode(
                url != null ? url.getValue() : null,
                codeSystem,
                code != null ? code.getValue() : null,
                version != null ? version.getValue() : null,
                display != null ? display.getValue() : null,
                coding,
                codeableConcept,
                displayLanguage != null ? displayLanguage.getValue() : null,
                xSystemCacheId != null ? xSystemCacheId.getValue() : null);
    }
}

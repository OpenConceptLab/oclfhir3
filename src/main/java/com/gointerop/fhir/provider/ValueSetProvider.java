package com.gointerop.fhir.provider;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.gointerop.fhir.service.CollectionService;

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
public class ValueSetProvider {

    private static final Set<String> authoritativeCodeSystems = Set.of(
            "http://www.saude.gov.br/fhir/r4/CodeSystem/",
            "https://terminologia.saude.gov.br/fhir/CodeSystem/",
            "https://mangara.hsl.org.br/fhir/CodeSystem/");

    private static final Set<String> authoritativeValuesets = Set.of(
            "http://www.saude.gov.br/fhir/r4/ValueSet/",
            "https://terminologia.saude.gov.br/fhir/ValueSet/",
            "https://mangara.hsl.org.br/fhir/ValueSet/");

    @Value("${tx.external.url:https://tx.fhir.org/tx-reg/resolve}")
    private String externalTxUrl;

    @Autowired
    private CollectionService service;

    @Read(type = ValueSet.class)
    public ValueSet readValueSet(@IdParam IIdType theId, RequestDetails requestDetails) {
        System.out.printf("📘 Lendo ValueSet com ID: %s%n", theId.getIdPart());
        ValueSet vs = service.fetchValueSetById(theId.getIdPart());
        if (vs == null) {
            throw new ResourceNotFoundException("ValueSet/" + theId.getIdPart() + " not found");
        }

        String accept = requestDetails.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            Narrative narrative = new Narrative();
            narrative.setStatus(NarrativeStatus.GENERATED);
            StringBuilder html = new StringBuilder();
            html.append("<div xmlns=\"http://www.w3.org/1999/xhtml\">")
                .append("<h1>ValueSet: ").append(vs.getName()).append("</h1>")
                .append("<p><strong>URL:</strong> ").append(vs.getUrl()).append("</p>")
                .append("<p><strong>Version:</strong> ").append(vs.getVersion()).append("</p>")
                .append("<h2>Concepts</h2><ul>");
            vs.getCompose().getInclude().forEach(include -> {
                include.getConcept().forEach(concept -> {
                    html.append("<li>").append(concept.getCode()).append(" - ")
                        .append(concept.getDisplay()).append("</li>");
                });
            });
            html.append("</ul></div>");
            narrative.setDivAsString(html.toString());
            vs.setText(narrative);
        }
        return vs;
    }

    @Search
    public List<ValueSet> search(
        RequestDetails requestDetails,
        @OptionalParam(name = ValueSet.SP_URL) UriParam url,
        @OptionalParam(name = ValueSet.SP_VERSION) TokenParam version,
        @OptionalParam(name = ValueSet.SP_NAME) StringParam name,
        @OptionalParam(name = ValueSet.SP_TITLE) StringParam title,
        @OptionalParam(name = ValueSet.SP_STATUS) TokenParam status,
        @OptionalParam(name = ValueSet.SP_PUBLISHER) StringParam publisher,
        @OptionalParam(name = ValueSet.SP_DESCRIPTION) StringParam description,
        @OptionalParam(name = ValueSet.SP_JURISDICTION) TokenParam jurisdiction,
        @OptionalParam(name = ValueSet.SP_DATE) DateParam date,
        @OptionalParam(name = "x-system-cache-id") StringType xSystemCacheId) {

        System.out.println("[DEBUG] Performing ValueSet");
        System.out.printf("- url: %s%n", url != null ? url.getValue() : "null");
        System.out.printf("- version: %s%n", version != null ? version.getValue() : "null");
        System.out.printf("- name: %s%n", name != null ? name.getValue() : "null");
        System.out.printf("- title: %s%n", title != null ? title.getValue() : "null");
        System.out.printf("- status: %s%n", status != null ? status.getValue() : "null");
        System.out.printf("- publisher: %s%n", publisher != null ? publisher.getValue() : "null");
        System.out.printf("- description: %s%n", description != null ? description.getValue() : "null");
        System.out.printf("- jurisdiction: %s%n", jurisdiction != null ? jurisdiction.getValue() : "null");
        System.out.printf("- date: %s%n", date != null ? date.getValueAsString() : "null");
        System.out.printf("- x-system-cache-id: %s%n", xSystemCacheId != null ? xSystemCacheId.getValue() : "null");

        return service.searchValueSets(
            url != null ? url.getValue() : null,
            version != null ? version.getValue() : null,
            name != null ? name.getValue() : null,
            title != null ? title.getValue() : null,
            status != null ? status.getValue() : null,
            publisher != null ? publisher.getValue() : null,
            description != null ? description.getValue() : null,
            jurisdiction != null ? jurisdiction.getValue() : null,
            date != null ? date.getValue() : null,
            xSystemCacheId != null ? xSystemCacheId.getValue() : null
        );
    }

    @Operation(name = "$expand", idempotent = true, type = ValueSet.class)
    public IBaseResource expandValueSet(
        RequestDetails requestDetails,
        @OperationParam(name = "url") UriType url,
        @OperationParam(name = "valueSet") ValueSet txResource,
        @OperationParam(name = "valueSetVersion") StringType valueSetVersion,
        @OperationParam(name = "filter") StringType filter,
        @OperationParam(name = "date") StringType date,
        @OperationParam(name = "offset") IntegerType offset,
        @OperationParam(name = "count") IntegerType count,
        @OperationParam(name = "includeDesignations") BooleanType includeDesignations,
        @OperationParam(name = "displayLanguage") StringType displayLanguage,
        @OperationParam(name = "x-system-cache-id") StringType xSystemCacheId) {

        System.out.println("[DEBUG] Performing ValueSet/$expand");
        System.out.printf("- url: %s%n", url != null ? url.getValue() : "null");
        System.out.printf("- valueSet.url: %s%n", txResource != null ? txResource.getUrl() : "null");
        System.out.printf("- valueSetVersion: %s%n", valueSetVersion != null ? valueSetVersion.getValue() : "null");
        System.out.printf("- filter: %s%n", filter != null ? filter.getValue() : "null");
        System.out.printf("- date: %s%n", date != null ? date.getValue() : "null");
        System.out.printf("- offset: %s%n", offset != null ? offset.getValue() : "null");
        System.out.printf("- count: %s%n", count != null ? count.getValue() : "null");
        System.out.printf("- includeDesignations: %s%n", includeDesignations != null ? includeDesignations.getValue() : "null");
        System.out.printf("- displayLanguage: %s%n", displayLanguage != null ? displayLanguage.getValue() : "null");
        System.out.printf("- x-system-cache-id: %s%n", xSystemCacheId != null ? xSystemCacheId.getValue() : "null");

        return service.expandValueSet(
            url != null ? url.getValue() : null,
            txResource,
            valueSetVersion != null ? valueSetVersion.getValue() : null,
            filter != null ? filter.getValue() : null,
            date != null ? date.getValue() : null,
            offset != null ? offset.getValue() : null,
            count != null ? count.getValue() : null,
            includeDesignations != null ? includeDesignations.getValue() : false,
            displayLanguage != null ? displayLanguage.getValue() : null,
            xSystemCacheId != null ? xSystemCacheId.getValue() : null
        );
    }

    @Operation(name = "$validate-code", idempotent = true, type = ValueSet.class)
    public IBaseResource validateCode(
        RequestDetails requestDetails,
        @OperationParam(name = "url") UriType url,
        @OperationParam(name = "valueSet") ValueSet valueSet,
        @OperationParam(name = "valueSetVersion") StringType version,
        @OperationParam(name = "code") StringType code,
        @OperationParam(name = "system") UriType system,
        @OperationParam(name = "display") StringType display,
        @OperationParam(name = "coding") Coding coding,
        @OperationParam(name = "codeableConcept") CodeableConcept codeableConcept,
        @OperationParam(name = "displayLanguage") StringType displayLanguage,
        @OperationParam(name = "x-system-cache-id") StringType xSystemCacheId) {

        System.out.println("[DEBUG] Performing ValueSet/$validate-code");
        System.out.printf("- url: %s%n", url != null ? url.getValue() : "null");
        System.out.printf("- valueSet.url: %s%n", valueSet != null ? valueSet.getUrl() : "null");
        System.out.printf("- valueSetVersion: %s%n", version != null ? version.getValue() : "null");
        System.out.printf("- code: %s%n", code != null ? code.getValue() : "null");
        System.out.printf("- system: %s%n", system != null ? system.getValue() : "null");
        System.out.printf("- display: %s%n", display != null ? display.getValue() : "null");

        if (coding != null) {
            System.out.printf("- coding.code: %s | system: %s | version: %s | display: %s%n",
                coding.getCode(), coding.getSystem(), coding.getVersion(), coding.getDisplay());
        } else {
            System.out.println("- coding: null");
        }

        if (codeableConcept != null) {
            System.out.printf("- codeableConcept.text: %s%n", codeableConcept.getText());
            for (int i = 0; i < codeableConcept.getCoding().size(); i++) {
                Coding c = codeableConcept.getCoding().get(i);
                System.out.printf("   └─ coding[%d] = system: %s | code: %s | display: %s%n",
                    i, c.getSystem(), c.getCode(), c.getDisplay());
            }
        } else {
            System.out.println("- codeableConcept: null");
        }

        System.out.printf("- displayLanguage: %s%n", displayLanguage != null ? displayLanguage.getValue() : "null");
        System.out.printf("- x-system-cache-id: %s%n", xSystemCacheId != null ? xSystemCacheId.getValue() : "null");

        return service.validateCodeInValueSet(
            url != null ? url.getValue() : null,
            valueSet,
            version != null ? version.getValue() : null,
            code != null ? code.getValue() : null,
            system != null ? system.getValue() : null,
            display != null ? display.getValue() : null,
            coding,
            codeableConcept,
            displayLanguage != null ? displayLanguage.getValue() : null,
            xSystemCacheId != null ? xSystemCacheId.getValue() : null
        );
    }
}

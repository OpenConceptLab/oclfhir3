package com.gointerop.fhir.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class Source implements Serializable {

    // Identificação
    private String id;
    private String uuid;
    private String name;
    private String mnemonic;
    private String fullName;
    private String shortCode;
    private String version;
    private String externalId;

    // Organização
    private String owner;
    private String ownerType;
    private String ownerUrl;

    // Metadata de controle
    private String createdBy;
    private String updatedBy;
    private String createdOn;
    private String updatedOn;
    private String updatedAt;
    private String revisionDate;

    // Atributos funcionais
    private String type; // Sempre "Source"
    private String sourceType; // Ex.: "Dictionary"
    private String description;
    private String purpose;
    private String copyright;
    private String defaultLocale;
    private List<String> supportedLocales;
    private String website;
    private Boolean experimental;
    private Boolean released;
    private String publicAccess;
    private Boolean isCanonical;
    private Boolean versionNeeded;
    private Boolean caseSensitive;
    private String contentType;
    private String hierarchyMeaning;
    private String canonicalUrl;
    private String conceptsUrl;
    private String mappingsUrl;
    private String hierarchyRootUrl;
    private String collectionReference;
    private String versionsUrl;

    // Auto-ID Configurations
    private String autoidConceptDescriptionExternalId;
    private Integer autoidConceptExternalIdStartFrom;
    private String autoidConceptExternalId;
    private String autoidConceptMnemonic;
    private Integer autoidConceptMnemonicStartFrom;
    private String autoidMappingExternalId;
    private Integer autoidMappingExternalIdStartFrom;
    private String autoidMappingMnemonic;
    private Integer autoidMappingMnemonicStartFrom;

    // Dados complementares
    /*private Map<String, Object> checksums;
    private Map<String, Object> contact;
    private Map<String, Object> jurisdiction;
    private Map<String, Object> identifier;
    private Map<String, Object> extras;
    private Map<String, Object> meta;*/
    private String logoUrl;
    private String text;
}

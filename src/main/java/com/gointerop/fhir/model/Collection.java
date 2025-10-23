package com.gointerop.fhir.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class Collection implements Serializable {

    private String type;
    private String uuid;
    private String id;

    @JsonProperty("short_code")
    private String shortCode;

    private String name;

    @JsonProperty("full_name")
    private String fullName;

    private String description;

    @JsonProperty("collection_type")
    private String collectionType;

    @JsonProperty("custom_validation_schema")
    private String customValidationSchema;

    @JsonProperty("public_access")
    private String publicAccess;

    @JsonProperty("default_locale")
    private String defaultLocale;

    @JsonProperty("supported_locales")
    private List<String> supportedLocales;

    private String website;

    private String url;

    private String owner;

    @JsonProperty("owner_type")
    private String ownerType;

    @JsonProperty("owner_url")
    private String ownerUrl;

    @JsonProperty("created_on")
    private String createdOn;

    @JsonProperty("updated_on")
    private String updatedOn;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("updated_by")
    private String updatedBy;

    private Map<String, Object> extras;

    @JsonProperty("external_id")
    private String externalId;

    @JsonProperty("versions_url")
    private String versionsUrl;

    private String version;

    @JsonProperty("concepts_url")
    private String conceptsUrl;

    @JsonProperty("mappings_url")
    private String mappingsUrl;

    @JsonProperty("expansions_url")
    private String expansionsUrl;

    @JsonProperty("custom_resources_linked_source")
    private String customResourcesLinkedSource;

    @JsonProperty("preferred_source")
    private String preferredSource;

    @JsonProperty("canonical_url")
    private String canonicalUrl;

    private Map<String, Object> identifier;

    private String publisher;

    private Map<String, Object> contact;

    private Map<String, Object> jurisdiction;

    private String purpose;

    private String copyright;

    private Map<String, Object> meta;

    private Boolean immutable;

    @JsonProperty("revision_date")
    private String revisionDate;

    @JsonProperty("logo_url")
    private String logoUrl;

    private Map<String, Object> text;

    private Boolean experimental;

    @JsonProperty("locked_date")
    private String lockedDate;

    @JsonProperty("autoexpand_head")
    private Boolean autoexpandHead;

    @JsonProperty("expansion_url")
    private String expansionUrl;

    private Map<String, Object> checksums;
}

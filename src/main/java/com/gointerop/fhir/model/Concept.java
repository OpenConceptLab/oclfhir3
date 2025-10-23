package com.gointerop.fhir.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Concept implements Serializable {

    private String uuid;

    private String id;

    @JsonProperty("external_id")
    private String externalId;

    @JsonProperty("concept_class")
    private String conceptClass;

    private String datatype;

    private String url;

    private boolean retired;

    private String source;

    private String owner;

    @JsonProperty("owner_type")
    private String ownerType;

    @JsonProperty("owner_url")
    private String ownerUrl;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("display_locale")
    private String displayLocale;

    private String version;

    private String locale;

    private String versionUrl;

    private String versionsUrl;

    @JsonProperty("version_created_by")
    private String versionCreatedBy;

    @JsonProperty("version_created_on")
    private String versionCreatedOn;

    @JsonProperty("version_updated_by")
    private String versionUpdatedBy;

    @JsonProperty("version_updated_on")
    private String versionUpdatedOn;

    @JsonProperty("versioned_object_id")
    private String versionedObjectId;

    private String type;

    @JsonProperty("update_comment")
    private String updateComment;

    @JsonProperty("latest_source_version")
    private String latestSourceVersion;

    private String createdOn;

    private String updatedOn;

    private String createdBy;

    private String updatedBy;

    @JsonProperty("public_can_view")
    private boolean publicCanView;

    @JsonProperty("checksums")
    private Map<String, String> checksums;

    private Map<String, Object> extras;

    private List<ConceptName> names;

    private List<Object> descriptions;

    // ====================
    // Convenience helpers
    // ====================

    public String getEffectiveCode() {
        return externalId != null ? externalId : id;
    }

    public String getEffectiveDisplay() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (names != null && !names.isEmpty()) {
            return names.get(0).getName();
        }
        return getEffectiveCode();
    }

}

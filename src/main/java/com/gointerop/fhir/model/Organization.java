package com.gointerop.fhir.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Organization implements Serializable {

    private String type;
    private String uuid;
    private String id;

    @JsonProperty("public_access")
    private String publicAccess;

    private String name;

    private String company;

    private String website;

    private String location;

    private Integer members;

    @JsonProperty("created_on")
    private String createdOn;

    @JsonProperty("updated_on")
    private String updatedOn;

    private String url;

    private String description;

    private String text;

    private String created_by;
    private String updated_by;

    private String logo_url;

    @JsonProperty("sources_url")
    private String sourcesUrl;

    @JsonProperty("collections_url")
    private String collectionsUrl;

    @JsonProperty("public_sources")
    private Integer publicSources;

    @JsonProperty("public_collections")
    private Integer publicCollections;

    private Object extras;
}

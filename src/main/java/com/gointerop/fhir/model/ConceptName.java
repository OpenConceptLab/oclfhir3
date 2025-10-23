package com.gointerop.fhir.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ConceptName implements Serializable {

    private String uuid;

    private String name;

    @JsonProperty("external_id")
    private String externalId;

    private String type;

    private String locale;

    @JsonProperty("locale_preferred")
    private boolean localePreferred;

    @JsonProperty("name_type")
    private String nameType;

    private String checksum;
}

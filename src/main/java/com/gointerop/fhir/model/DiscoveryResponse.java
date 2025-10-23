package com.gointerop.fhir.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DiscoveryResponse {

    @JsonProperty("last-update")
    private String last_update;

    @JsonProperty("master-url")
    private String masterUrl;

    private List<DiscoveredEndpoint> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DiscoveredEndpoint implements Serializable {

        @JsonProperty("server-name")
        private String serverName;

        @JsonProperty("server-code")
        private String serverCode;

        @JsonProperty("registry-name")
        private String registryName;

        @JsonProperty("registry-code")
        private String registryCode;

        @JsonProperty("registry-url")
        private String registryUrl;

        private String url;

        @JsonProperty("version")
        private String version;

        private String error;

        @JsonProperty("last-success")
        private long lastSuccess;

        private int systems;

        private List<String> authoritative;

        @JsonProperty("authoritative-valuesets")
        private List<String> authoritativeValuesets = null;

        private List<String> candidate = null;

        @JsonProperty("candidate-valuesets")
        private List<String> candidateValuesets = null;

        private boolean open;
        private boolean password;
        private boolean token;
        private boolean oauth;
        private boolean smart;
        private boolean cert;

        // Não precisa de getters/setters extra — Lombok já faz.
    }
}

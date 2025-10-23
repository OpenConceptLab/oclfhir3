package com.gointerop.fhir.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ResolutionResponse {

    @JsonProperty("formatVersion")
    private String formatVersion;

    @JsonProperty("registry-url")
    private String registryUrl;

    private List<ResolvedEndpoint> authoritative;

    // ⚠️ Plural igual ao JSON real!
    private List<ResolvedEndpoint> candidates;

    @Data
    public static class ResolvedEndpoint implements Serializable {

        @JsonProperty("server-name")
        private String serverName;

        private String url;

        @JsonProperty("fhirVersion")
        private String fhirVersion;

        private boolean open;
        private boolean password;
        private boolean token;
        private boolean oauth;
        private boolean smart;
        private boolean cert;

        @JsonProperty("access_info")
        private String accessInfo;
    }
}

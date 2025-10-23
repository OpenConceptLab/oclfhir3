package com.gointerop.fhir.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gointerop.fhir.model.DiscoveryResponse;
import com.gointerop.fhir.model.DiscoveryResponse.DiscoveredEndpoint;
import com.gointerop.fhir.model.ResolutionResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DiscoveryService {

    // 🔗 Seus prefixos autoritativos
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

    private final RestTemplate restTemplate;

    public DiscoveryService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(unsafeHttpClient()))
                .build();
    }

    private CloseableHttpClient unsafeHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE);
            HttpClientBuilder httpClientBuilder = HttpClients.custom();
            httpClientBuilder.setSSLSocketFactory(csf);
            return httpClientBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DiscoveryResponse discover(String registry, String server, String fhirVersion, String url,
            boolean authoritativeOnly) {

        log.info("Discovery called");

        // Passo 1: Buscar o registry oficial do HL7
        ResponseEntity<byte[]> registryResponse = restTemplate.getForEntity(
                "https://tx.fhir.org/tx-reg", byte[].class);

        byte[] rawBytes = registryResponse.getBody();
        if (rawBytes == null) {
            throw new RuntimeException("Empty registry response from tx.fhir.org");
        }

        String jsonPayload = new String(rawBytes, StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        DiscoveryResponse externalRegistry;
        try {
            externalRegistry = mapper.readValue(jsonPayload, DiscoveryResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse external registry JSON", e);
        }

        // Passo 2: Monta o seu endpoint
        DiscoveredEndpoint myEndpoint = new DiscoveredEndpoint();
        myEndpoint.setServerName("GOInterop Local Terminology Server");
        myEndpoint.setServerCode("gointerop-local");
        myEndpoint.setRegistryName("GOInterop Terminology Registry");
        myEndpoint.setRegistryCode("gointerop");
        myEndpoint.setRegistryUrl("https://fhir.github.io/ig-registry/tx-servers.json");
        myEndpoint.setUrl("https://tx.gointerop.com/r4");
        myEndpoint.setVersion("4.0.1");
        myEndpoint.setLastSuccess(0L);
        myEndpoint.setSystems(1);
        myEndpoint.setAuthoritative(List.copyOf(authoritativeCodeSystems));
        myEndpoint.setAuthoritativeValuesets(List.copyOf(authoritativeValuesets));
        myEndpoint.setOpen(true);

        // Passo 3: Mescla o seu com os do ecossistema
        List<DiscoveredEndpoint> finalEndpoints = externalRegistry.getResults();
        finalEndpoints.add(myEndpoint);

        // Passo 4: Retorna tudo
        DiscoveryResponse response = new DiscoveryResponse();
        response.setLast_update(Instant.now().toString());
        response.setMasterUrl("https://fhir.github.io/ig-registry/tx-servers.json");
        response.setResults(finalEndpoints);

        return response;
    }

    public ResolutionResponse resolve(String fhirVersion, String url, String valueSet, boolean authoritativeOnly) {

        boolean isInternal = false;

        if (url != null) {
            isInternal = authoritativeCodeSystems.stream().anyMatch(url::startsWith);
        }

        if (valueSet != null) {
            isInternal = authoritativeValuesets.stream().anyMatch(valueSet::startsWith);
        }

        if (isInternal) {
            log.info("Resolving with local Terminology Server for {}", valueSet != null ? valueSet : url);

            ResolutionResponse.ResolvedEndpoint myEndpoint = new ResolutionResponse.ResolvedEndpoint();
            myEndpoint.setServerName("GOInterop Local Terminology Server");
            myEndpoint.setUrl("https://tx.gointerop.com/r4");
            myEndpoint.setOpen(true);
            myEndpoint.setAccessInfo("https://tx.gointerop.com/r4");

            ResolutionResponse response = new ResolutionResponse();
            response.setFormatVersion("1");
            response.setRegistryUrl("https://fhir.github.io/ig-registry/tx-servers.json");
            response.setCandidates(List.of(myEndpoint));
            response.setAuthoritative(null);

            return response;

        } else {
            log.info("Forwarding to tx.fhir.org for {}", valueSet != null ? valueSet : url);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(externalTxUrl)
                    .queryParam("fhirVersion", fhirVersion);

            if (url != null)
                builder.queryParam("url", url);
            if (valueSet != null)
                builder.queryParam("valueSet", valueSet);
            if (authoritativeOnly)
                builder.queryParam("authoritativeOnly", "true");

            // 🔑 FAÇA MANUAL: GET raw bytes
            ResponseEntity<byte[]> responseEntity = restTemplate.getForEntity(builder.toUriString(), byte[].class);

            byte[] rawBytes = responseEntity.getBody();
            if (rawBytes == null) {
                throw new RuntimeException("Empty response body from external terminology server");
            }

            // 🔑 Decode explicitamente com UTF-8
            String jsonPayload = new String(rawBytes, StandardCharsets.UTF_8);

            log.debug("External tx.fhir.org raw response: {}", jsonPayload);

            // 🔑 Desserialize com ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(jsonPayload, ResolutionResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse ResolutionResponse JSON", e);
            }
        }
    }
}

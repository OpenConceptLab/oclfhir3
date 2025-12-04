package com.gointerop.fhir.repository;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.gointerop.fhir.model.Concept;
import com.gointerop.fhir.model.Mapping;

@Repository
public class MappingRepository {

    @Value("${ocl.base-url}")
    private String oclBaseUrl;

    private final RestTemplate restTemplate;

    public MappingRepository(RestTemplateBuilder builder) {
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

    @Cacheable(value = "mappings-by-org-and-source-and-concept", key = "#org + '-' + #source + '-' + #sourceConcept + '-' + #mapType")
    public List<Mapping> mappingsByOrgAndSourceAndConcept(String org, String source, String sourceConcept) {
        System.out.println("DEBUG: Fetching mappings by organization-source-concept: " + org + ", sourceId: " + source + ", concept: " + sourceConcept);
        String url = String.format("%s/orgs/%s/sources/%s/concepts/%s/mappings", oclBaseUrl, org, source, sourceConcept);
        ResponseEntity<Mapping[]> response = restTemplate.getForEntity(url, Mapping[].class);
        return Arrays.asList(Objects.requireNonNull(response.getBody()));
        
    }
}

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
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import com.gointerop.fhir.model.ConceptName;

@Repository
public class ConceptNameRepository {

    private final RestTemplate restTemplate;

    @Value("${ocl.base-url}")
    private String oclBaseUrl;

    public ConceptNameRepository(RestTemplateBuilder builder) {
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
            return HttpClients.custom().setSSLSocketFactory(csf).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ✅ Buscar todos os ConceptNames de um Concept
     */
    @Cacheable(value = "concept-names", key = "#org + '-' + #sourceId + '-' + #conceptId")
    public List<ConceptName> fetchConceptNames(String org, String sourceId, String conceptId) {
        System.out.println("DEBUG: Fetching concept names for org: " + org + ", sourceId: " + sourceId + ", conceptId: "
                + conceptId);
        String url = String.format("%s/orgs/%s/sources/%s/concepts/%s/names/", oclBaseUrl, org, sourceId, conceptId);
        ResponseEntity<ConceptName[]> response = restTemplate.getForEntity(url, ConceptName[].class);
        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

}

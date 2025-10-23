package com.gointerop.fhir.repository;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
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

import com.gointerop.fhir.model.Concept;

@Repository
public class ConceptRepository {

    private final RestTemplate restTemplate;

    public ConceptRepository(RestTemplateBuilder builder) {
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

    @Value("${ocl.base-url}")
    private String oclBaseUrl;

    /**
     * ✅ Lista todos os conceitos de um source específico.
     */
    @Cacheable(value = "concepts-by-org-and-source", key = "#org + '-' + #sourceId")
    public List<Concept> fetchConceptsByOrgAndSource(String org, String sourceId) {
        System.out.println("DEBUG: Fetching concepts by organization: " + org + ", sourceId: " + sourceId);
        String url = String.format("%s/orgs/%s/sources/%s/concepts/", oclBaseUrl, org, sourceId);
        ResponseEntity<Concept[]> response = restTemplate.getForEntity(url, Concept[].class);
        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

    /**
     * ✅ Lista todos os conceitos de uma collection específica.
     */
    @Cacheable(value = "concepts-by-org-and-collection", key = "#org + '-' + #collectionId + '-' + #count + '-' + #offset")
    public List<Concept> fetchConceptsByOrgAndCollection(String org, String collectionId, int count, int offset) {
        System.out.println("DEBUG: Fetching concepts by organization: " + org + ", collectionId: " + collectionId);
        String baseUrl = String.format("%s/orgs/%s/collections/%s/concepts/", oclBaseUrl, org, collectionId);
        return fetchAllPaged(baseUrl, Concept[].class, count, offset);
    }

    @Cacheable(value = "concepts-by-org-and-collection-and-filter", key = "#org + '-' + #collectionId + '-' + #count + '-' + #offset + '-' + #filter")
    public List<Concept> fetchConceptsByOrgAndCollectionAndFilter(String org, String collectionId, int count, int offset, String filter) {
        System.out.println("DEBUG: Fetching concepts by organization: " + org + ", collectionId: " + collectionId);
        String baseUrl = String.format("%s/orgs/%s/collections/%s/concepts/", oclBaseUrl, org, collectionId);
        return fetchAllPagedWithFilter(baseUrl, Concept[].class, count, offset, filter);
    }

    @Cacheable(value = "concepts-by-source", key = "#org + '-' + #sourceId + '-' + #count + '-' + #offset")
    public List<Concept> fetchConceptsBySource(String org, String sourceId, int count, int offset) {
        System.out.println("DEBUG: Fetching concepts by organization: " + org + ", sourceId: " + sourceId);
        String baseUrl = String.format("%s/orgs/%s/sources/%s/concepts/", oclBaseUrl, org, sourceId);
        return fetchAllPaged(baseUrl, Concept[].class, count, offset);
    }

    /**
     * ✅ Busca um Concept específico de um source.
     */
    @Cacheable(value = "concept-by-source", key = "#org + '-' + #sourceId" + " + '-' + #conceptId")
    public Concept fetchConceptBySource(String org, String sourceId, String conceptId) {
        System.out.println(
                "DEBUG: Fetching concept by source: " + org + ", sourceId: " + sourceId + ", conceptId: " + conceptId);
        String url = String.format("%s/orgs/%s/sources/%s/concepts/%s/", oclBaseUrl, org, sourceId, conceptId);
        ResponseEntity<Concept> response = restTemplate.getForEntity(url, Concept.class);
        return response.getBody();
    }

    @Cacheable(value = "concept-by-collection", key = "#org + '-' + #collectionId" + " + '-' + #conceptId")
    public Concept fetchConceptByCollection(String org, String collectionId, String conceptId) {
        System.out.println("DEBUG: Fetching concept by collection: " + org + ", collectionId: " + collectionId
                + ", conceptId: " + conceptId);
        String url = String.format("%s/orgs/%s/collections/%s/concepts/%s/", oclBaseUrl, org, collectionId, conceptId);
        ResponseEntity<Concept> response = restTemplate.getForEntity(url, Concept.class);
        return response.getBody();
    }

    private <T> List<T> fetchAllPaged(String baseUrl, Class<T[]> responseType, int count, int offset) {
        List<T> allItems = new ArrayList<>();

        // Fetch first page to determine if there are results
        String firstUrl = String.format("%s?page=%d&limit=%d", baseUrl, (offset / count) + 1, count);
        ResponseEntity<T[]> firstResponse = restTemplate.getForEntity(firstUrl, responseType);
        T[] firstBody = firstResponse.getBody();
        if (firstBody == null || firstBody.length == 0) {
            return allItems;
        }
        allItems.addAll(Arrays.asList(firstBody));

        return allItems;
    }

    private <T> List<T> fetchAllPagedWithFilter(String baseUrl, Class<T[]> responseType, int count, int offset, String filter) {
        List<T> allItems = new ArrayList<>();

        // Fetch first page to determine if there are results
        String firstUrl = String.format("%s?page=%d&limit=%d&q=%s", baseUrl, (offset / count) + 1, count, filter);
        ResponseEntity<T[]> firstResponse = restTemplate.getForEntity(firstUrl, responseType);
        T[] firstBody = firstResponse.getBody();
        if (firstBody == null || firstBody.length == 0) {
            return allItems;
        }
        allItems.addAll(Arrays.asList(firstBody));

        return allItems;
    }
}

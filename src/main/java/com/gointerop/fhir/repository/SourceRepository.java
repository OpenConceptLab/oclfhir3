package com.gointerop.fhir.repository;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

import com.gointerop.fhir.model.Source;

@Repository
public class SourceRepository {

    @Value("${ocl.base-url}")
    private String oclBaseUrl;

    private final RestTemplate restTemplate;

    public SourceRepository(RestTemplateBuilder builder) {
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

    @Cacheable("sources-by-id")
    public Source fetchSourceById(String id) {
        System.out.println("DEBUG: Fetching source by ID: " + id);
        String url = String.format("%s/sources/%s/", oclBaseUrl, id);
        ResponseEntity<Source> response = restTemplate.getForEntity(url, Source.class);
        return response.getBody();
    }

    @Cacheable("all-sources")
    public List<Source> fetchAllSources() {
        System.out.println("DEBUG: Fetching all sources");
        String baseUrl = String.format("%s/sources/", oclBaseUrl);
        return fetchAllPaged(baseUrl, Source[].class);
    }

    @Cacheable(value = "sources-by-org", key = "#org")
    public List<Source> fetchSourcesByOrg(String org) {
        System.out.println("DEBUG: Fetching sources by organization: " + org);
        String url = String.format("%s/orgs/%s/sources/", oclBaseUrl, org);
        ResponseEntity<Source[]> response = restTemplate.getForEntity(url, Source[].class);
        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

    @Cacheable(value = "source-by-org-and-id", key = "#owner + '-' + #sourceId")
    public Source fetchSourceByOwnerAndId(String owner, String sourceId) {
        System.out.println("DEBUG: Fetching source by owner and ID: " + owner + ", " + sourceId);
        String url = String.format("%s/orgs/%s/sources/%s/", oclBaseUrl, owner, sourceId);
        ResponseEntity<Source> response = restTemplate.getForEntity(url, Source.class);
        return response.getBody();
    }

    @Cacheable(value = "sources-by-canonical-url", key = "#canonicalUrl")
    public List<Source> fetchSourcesByCanonicalUrl(String canonicalUrl) {
        System.out.println("DEBUG: Fetching sources by canonical URL: " + canonicalUrl);
        String url = String.format("%s/sources/", oclBaseUrl);
        List<Source> sources = fetchAllPaged(url, Source[].class);
        return sources.stream()
                .filter(source -> canonicalUrl.equals(source.getCanonicalUrl()))
                .collect(Collectors.toList());
    }

    public <T> List<T> fetchAllPaged(String baseUrl, Class<T[]> responseType) {
        int page = 1;
        int pageSize = 100; // Ou ajuste conforme o limite ideal do OCL
        List<T> allItems = new ArrayList<>();

        while (true) {
            String url = String.format("%s?page=%d&limit=%d", baseUrl, page, pageSize);
            ResponseEntity<T[]> response = restTemplate.getForEntity(url, responseType);
            T[] body = response.getBody();
            if (body == null || body.length == 0)
                break;

            allItems.addAll(Arrays.asList(body));
            if (body.length < pageSize)
                break; // Última página
            page++;
        }

        return allItems;
    }
}

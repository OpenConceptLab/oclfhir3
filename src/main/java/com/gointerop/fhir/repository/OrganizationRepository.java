package com.gointerop.fhir.repository;

import com.gointerop.fhir.model.Organization;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class OrganizationRepository {
    
    @Value("${ocl.base-url}")
    private String oclBaseUrl;

    private final RestTemplate restTemplate;

    public OrganizationRepository(RestTemplateBuilder builder) {
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

    @Cacheable("all-organizations")
    public Organization[] fetchAllOrganizations() {
        System.out.println("DEBUG: Fetching all organizations");
        String url = String.format("%s/orgs/", oclBaseUrl);
        return restTemplate.getForObject(url, Organization[].class);
    }

    @Cacheable(value = "organization-by-id", key = "#orgId")
    public Organization fetchOrganizationById(String orgId) {
        System.out.println("DEBUG: Fetching organization by ID: " + orgId);
        String url = String.format("%s/orgs/%s/", oclBaseUrl, orgId);
        return restTemplate.getForObject(url, Organization.class);
    }
}

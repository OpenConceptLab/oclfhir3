package com.gointerop.fhir.service;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.gointerop.fhir.converter.MappingParametersConverter;
import com.gointerop.fhir.helper.OperationOutcomeHelper;
import com.gointerop.fhir.model.Concept;
import com.gointerop.fhir.model.Mapping;
import com.gointerop.fhir.model.Source;
import com.gointerop.fhir.repository.ConceptRepository;
import com.gointerop.fhir.repository.MappingRepository;
import com.gointerop.fhir.repository.SourceRepository;

@Service
public class MappingService {

    @Value("${ocl.base-url}")
    private String oclBaseUrl;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private MappingRepository repository;

    @Autowired
    private MappingParametersConverter converter;

    @Autowired
    OperationOutcomeHelper operationOutcomeHelper;

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

    @Cacheable(value = "translateCache", keyGenerator = "translateKeyGenerator")
    public BaseResource translate(
            String url,
            ConceptMap conceptMap,
            String conceptMapVersion,
            String code,
            String system,
            String version,
            Coding coding,
            CodeableConcept codeableConcept,
            String target,
            String targetSystem,
            Boolean reverse,
            String xSystemCacheId) {

        if (system == null || system.isEmpty()) {
            return buildError("In the current implementation, system parameter is required for translation.");
        }

        if ((code == null || code.isEmpty()) && (coding == null) && (codeableConcept == null)) {
            return buildError("code, coding or codeableConcept parameter are required for translation.");
        }

        String codeToBeMatched =
                code != null ? code :
                (coding != null ? coding.getCode() :
                (codeableConcept != null && !codeableConcept.getCoding().isEmpty() ?
                        codeableConcept.getCodingFirstRep().getCode() : null));

        List<Source> sources = sourceRepository.fetchSourcesByCanonicalUrl(system);

        Source matchedSource = sources.stream()
                .filter(source -> source.getCanonicalUrl() != null
                        && source.getCanonicalUrl().equalsIgnoreCase(system))
                .findFirst()
                .orElse(null);

        if (matchedSource == null) {
            return buildError("No CodeSystem found for system URL '" + system + "'");
        }

        Concept concept = null;

        try {
            concept = conceptRepository.fetchConceptBySource(
                    matchedSource.getOwner(),
                    matchedSource.getId(),
                    codeToBeMatched);
        } catch (Exception e) {
            return buildError("Code '" + codeToBeMatched + "' not found in system '" + system + "'.");
        }

        List<Mapping> mappings = repository.mappingsByOrgAndSourceAndConcept(
                matchedSource.getOwner(),
                matchedSource.getId(),
                concept.getId());

        if (mappings == null || mappings.isEmpty()) {
            return buildError("No mapping found for concept '" + concept.getId() + "' in source '" + matchedSource.getId() + "'.");
        }

        List<Mapping> filteredMappings = new ArrayList<>();
        List<Source> targetSources = new ArrayList<>();
        for (Mapping map : mappings) {
            Source targetSource = sourceRepository.fetchSourceByOwnerAndId(map.getToSourceOwner(),
                    toSourceId(map.getToSourceUrl()));

            String targetCanonical = targetSource != null ? targetSource.getCanonicalUrl() : map.getToSourceUrl();

            if (targetSystem != null && !targetSystem.isEmpty()) {
                if (targetCanonical == null || !targetSystem.equalsIgnoreCase(targetCanonical)) {
                    continue;
                }
            }

            if (targetSource == null) {
                continue;
            }

            filteredMappings.add(map);
            targetSources.add(targetSource);
        }

        if (filteredMappings.isEmpty()) {
            if (targetSystem != null && !targetSystem.isEmpty()) {
                return buildError("No mapping found for concept '" + concept.getId() + "' in source '" + matchedSource.getId() +
                        "' matching target system '" + targetSystem + "'.");
            }
            return buildError("No mapping found for concept '" + concept.getId() + "' in source '" + matchedSource.getId() + "'.");
        }

        return converter.toParameters(filteredMappings, targetSources);
    }

    private OperationOutcome buildError(String details) {
        return operationOutcomeHelper.help(IssueSeverity.ERROR,IssueType.INVARIANT, details);
    }

    private String toSourceId(String sourceAccessionUrl) {
        if (sourceAccessionUrl == null || sourceAccessionUrl.isEmpty()) {
            return null;
        }
        String[] parts = sourceAccessionUrl.split("/");
        return parts[parts.length - 1];
    }
}

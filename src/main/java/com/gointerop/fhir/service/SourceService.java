package com.gointerop.fhir.service;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.gointerop.fhir.converter.SourceCodeSystemConverter;
import com.gointerop.fhir.helper.OperationOutcomeHelper;
import com.gointerop.fhir.model.Concept;
import com.gointerop.fhir.model.ConceptName;
import com.gointerop.fhir.model.Source;
import com.gointerop.fhir.repository.ConceptNameRepository;
import com.gointerop.fhir.repository.ConceptRepository;
import com.gointerop.fhir.repository.SourceRepository;
import com.gointerop.fhir.utils.FHIRUtil;

@Service
public class SourceService {

    @Value("${tx.external.url:https://tx.fhir.org/r4}")
    private String externalTxUrl;

    // 🔗 Seus prefixos autoritativos
    private static final Set<String> authoritativeCodeSystems = Set.of(
            "http://www.saude.gov.br/fhir/r4/CodeSystem/",
            "https://terminologia.saude.gov.br/fhir/CodeSystem/",
            "https://mangara.hsl.org.br/fhir/CodeSystem/");

    private static final Set<String> authoritativeValuesets = Set.of(
            "http://www.saude.gov.br/fhir/r4/ValueSet/",
            "https://terminologia.saude.gov.br/fhir/ValueSet/",
            "https://mangara.hsl.org.br/fhir/ValueSet/");

    @Autowired
    private FHIRUtil fhirUtil;

    @Autowired
    private OperationOutcomeHelper operationOutcomeHelper;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private ConceptNameRepository conceptNameRepository;

    @Autowired
    private SourceCodeSystemConverter sourceConverter;

    private final RestTemplate restTemplate;

    public SourceService(RestTemplateBuilder builder) {
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

    @PostConstruct
    public void warmUp() {
        sourceRepository.fetchAllSources();
    }

    @Cacheable(value = "fetchCodeSystemById", key = "#id")
    public CodeSystem fetchCodeSystemById(String id) {
        Source source = sourceRepository.fetchAllSources()
                .stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElse(null);
        if (source == null) {
            return null;
        }
        return sourceConverter.toCodeSystem(source);
    }

    @Cacheable(value = "searchCodeSystems", key = "#url")
    public List<CodeSystem> searchCodeSystems(
            String url,
            String system,
            String name,
            String version,
            String identifier,
            String publisher,
            String title,
            String description,
            String jurisdiction,
            String status,
            Date date,
            String contentMode,
            String xSystemCacheId) {

        List<CodeSystem> results = new ArrayList<>();

        List<Source> filteredSources = sourceRepository.fetchAllSources().stream()
                .filter(s -> (url == null || s.getCanonicalUrl() != null && s.getCanonicalUrl().contains(url))
                        && (system == null || s.getCanonicalUrl() != null && s.getCanonicalUrl().contains(system))
                        && (name == null || s.getName() != null && s.getName().contains(name))
                        && s.getCanonicalUrl() != null)
                .collect(Collectors.toList());

        for (Source sourceDetail : filteredSources) {
            CodeSystem cs = sourceConverter.toCodeSystem(sourceDetail);

            cs.setContent(CodeSystem.CodeSystemContentMode.NOTPRESENT);

            results.add(cs);
        }

        return results;
    }

    @Cacheable(value = "lookup", key = "#system + '_' + #code + '_' + #version + '_' + #displayLanguage + '_' + #xSystemCacheId")
    public IBaseResource lookup(String system, String code, String version, String displayLanguage,
            List<StringType> properties, String xSystemCacheId) {

        if (!isAuthoritativeCodeSystem(system)) {
            return delegateLookup(system, code, version, displayLanguage, properties, xSystemCacheId);
        }

        List<Source> sources = sourceRepository.fetchSourcesByCanonicalUrl(system);

        Source matchedSource = sources.stream()
                .filter(source -> system.equalsIgnoreCase(source.getCanonicalUrl()))
                .findFirst()
                .orElse(null);

        if (matchedSource == null) {
            return buildError("No CodeSystem found for system URL '" + system + "'");
        }

        Concept concept = conceptRepository.fetchConceptBySource(matchedSource.getOwner(),
                matchedSource.getId(), code);

        if (concept == null) {
            return buildError("Code '" + code + "' not found in system '" + system + "'.");
        }

        Parameters params = new Parameters();
        params.addParameter().setName("result").setValue(new BooleanType(true));
        params.addParameter().setName("name").setValue(new StringType(matchedSource.getName()));
        params.addParameter().setName("version").setValue(new StringType(matchedSource.getVersion()));

        Optional<ConceptName> bestName = Optional.empty();
        if (concept.getNames() != null && !concept.getNames().isEmpty()) {
            if (displayLanguage != null) {
                bestName = concept.getNames().stream()
                        .filter(n -> displayLanguage.equalsIgnoreCase(n.getLocale()))
                        .findFirst();
            }
            if (bestName.isEmpty()) {
                bestName = concept.getNames().stream().filter(ConceptName::isLocalePreferred).findFirst();
            }
        }

        if (bestName.isPresent()) {
            params.addParameter().setName("display").setValue(new StringType(bestName.get().getName()));
        } else if (concept.getDisplayName() != null) {
            params.addParameter().setName("display").setValue(new StringType(concept.getDisplayName()));
        }

        List<ConceptName> names = conceptNameRepository.fetchConceptNames(matchedSource.getOwner(),
                matchedSource.getId(), concept.getId());
        if (names != null) {
            for (ConceptName name : names) {
                Parameters.ParametersParameterComponent designation = params.addParameter().setName("designation");
                designation.addPart().setName("language").setValue(new StringType(name.getLocale()));
                designation.addPart().setName("use").setValue(new Coding().setCode(name.getNameType()));
                designation.addPart().setName("value").setValue(new StringType(name.getName()));
            }
        }

        if (properties != null) {
            for (StringType prop : properties) {
                if ("status".equalsIgnoreCase(prop.getValue())) {
                    params.addParameter().setName("property")
                            .addPart().setName("code").setValue(new StringType("status"))
                            .addPart().setName("value").setValue(new StringType(concept.getConceptClass()));
                }
                if ("retired".equalsIgnoreCase(prop.getValue())) {
                    params.addParameter().setName("property")
                            .addPart().setName("code").setValue(new StringType("retired"))
                            .addPart().setName("value").setValue(new BooleanType(concept.isRetired()));
                }
            }
        }

        return params;
    }

    @Cacheable(value = "validateCode", key = "#url + '_' + #txResource?.url + '_' + #code + '_' + #version + '_' + '_' + #coding + '_' + #codeableConcept + '_' + #xSystemCacheId")
    public IBaseResource validateCode(
            String url,
            CodeSystem txResource,
            String code,
            String version,
            String display,
            Coding coding,
            CodeableConcept codeableConcept,
            String displayLanguage,
            String xSystemCacheId) {

        String systemUrl = url != null ? url
                : coding != null && coding.getSystem() != null ? coding.getSystem()
                        : codeableConcept != null && codeableConcept.getCodingFirstRep().getSystem() != null
                                ? codeableConcept.getCodingFirstRep().getSystem()
                                : null;

        if (systemUrl == null && txResource == null) {
            return buildError("Either 'url' or 'tx-resource' (CodeSystem) must be provided.");
        }

        if (!isAuthoritativeCodeSystem(systemUrl)) {
            return delegateValidateCode(url, txResource, code, version, display, coding, codeableConcept,
                    displayLanguage, xSystemCacheId);
        }

        if (txResource != null) {
            if (txResource.getConcept() == null || txResource.getConcept().isEmpty()) {
                return buildError("Cannot validate code against a CodeSystem with 'not-present' content mode.");
            }

            boolean codeFound = txResource.getConcept().stream()
                    .anyMatch(concept -> concept.getCode().equals(code));
            if (!codeFound) {
                return buildError("Code '" + code + "' not found in provided CodeSystem.");
            }
        }

        List<Source> sources = sourceRepository.fetchSourcesByCanonicalUrl(systemUrl);
        Source matchedSource = sources.stream()
                .filter(source -> systemUrl.equalsIgnoreCase(source.getCanonicalUrl()))
                .findFirst()
                .orElse(null);

        if (matchedSource == null) {
            return buildError("No CodeSystem found for URL '" + systemUrl + "'");
        }

        Concept concept = conceptRepository.fetchConceptBySource(matchedSource.getOwner(), matchedSource.getId(),
                code != null ? code
                        : coding != null ? coding.getCode()
                                : codeableConcept.getCodingFirstRep().getCode());

        if (concept == null) {
            return buildError("Code '" + code + "' not found in CodeSystem.");
        }

        if (version != null && !version.equalsIgnoreCase(concept.getVersion())) {
            return buildError("Provided version does not match concept version.");
        }

        if (display != null && !display.equalsIgnoreCase(concept.getEffectiveDisplay())) {
            return buildError("Provided display does not match CodeSystem display.");
        }

        Parameters output = new Parameters();
        output.addParameter().setName("result").setValue(new BooleanType(true));
        output.addParameter().setName("system").setValue(new UriType(systemUrl));
        output.addParameter().setName("code").setValue(new StringType(code));
        output.addParameter().setName("display").setValue(new StringType(
                display != null ? display : concept.getEffectiveDisplay()));
        output.addParameter().setName("version").setValue(new StringType(matchedSource.getVersion()));

        return output;
    }

    private boolean isAuthoritativeCodeSystem(String system) {
        return authoritativeCodeSystems.stream().anyMatch(system::startsWith);
    }

    @Cacheable(value = "delegateLookup", key = "#system + '_' + #code + '_' + #version + '_' + '_' + #xSystemCacheId")
    private IBaseResource delegateLookup(String system, String code, String version,
            String displayLanguage, List<StringType> properties, String xSystemCacheId) {

        Parameters params = new Parameters();
        params.addParameter().setName("system").setValue(new UriType(system));
        params.addParameter().setName("code").setValue(new StringType(code));
        
        if(xSystemCacheId != null) {
            params.addParameter().setName("x-system-cache-id").setValue(new StringType(xSystemCacheId));
        }

        String body = fhirUtil.getFhirContext().newJsonParser().encodeResourceToString(params);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/fhir+json");
        headers.add("Content-Type", "application/fhir+json");

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        Parameters response = null;

        try {
            String responseJson = restTemplate.postForObject(
                externalTxUrl + "/CodeSystem/$lookup",
                entity,
                String.class // ⚠️ pegue como String bruta!
            );

            response = fhirUtil.getFhirContext().newJsonParser().parseResource(Parameters.class, responseJson);
        } catch (Exception e) {
            return operationOutcomeHelper.help(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.NOTFOUND,
                    "Unable to provide support for CodeSystem " + system != null ? system : "unknown");
        }

        return response;
    }

    @Cacheable(value = "delegateValidateCode", key = "#url + '_' + #txResource?.url + '_' + #code + '_' + #version + '_' + '_' + #coding + '_' + #codeableConcept + '_' + #xSystemCacheId")
    private Parameters delegateValidateCode(
            String url,
            CodeSystem txResource,
            String code,
            String version,
            String display,
            Coding coding,
            CodeableConcept codeableConcept,
            String displayLanguage,
            String xSystemCacheId) {

        Parameters params = new Parameters();

        if (url != null) {
            params.addParameter().setName("url").setValue(new UriType(url));
        }
        if (txResource != null) {
            params.addParameter().setName("resource").setResource(txResource);
        }
        if (code != null) {
            params.addParameter().setName("code").setValue(new StringType(code));
        }
        if (version != null) {
            params.addParameter().setName("version").setValue(new StringType(version));
        }
        if (display != null) {
            params.addParameter().setName("display").setValue(new StringType(display));
        }
        if (coding != null) {
            params.addParameter().setName("coding").setValue(coding);
        }
        if (codeableConcept != null) {
            params.addParameter().setName("codeableConcept").setValue(codeableConcept);
        }
        if (displayLanguage != null) {
            params.addParameter().setName("displayLanguage").setValue(new StringType(displayLanguage));
        }

        if (xSystemCacheId != null) {
            params.addParameter().setName("x-system-cache-id").setValue(new StringType(xSystemCacheId));
        }

        // ✅ Serializa com HAPI FHIR
        String body = fhirUtil.getFhirContext().newJsonParser().encodeResourceToString(params);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/fhir+json");
        headers.add("Content-Type", "application/fhir+json");

        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

        // ✅ Recebe como String e parseia com o FHIR parser
        String responseJson = restTemplate.postForObject(
                externalTxUrl + "/CodeSystem/$validate-code",
                requestEntity,
                String.class);

        return fhirUtil.getFhirContext().newJsonParser().parseResource(Parameters.class, responseJson);
    }

    private Parameters buildError(String message) {
        Parameters output = new Parameters();
        output.addParameter().setName("result").setValue(new BooleanType(false));
        output.addParameter().setName("message").setValue(new StringType(message));
        return output;
    }
}

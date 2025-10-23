package com.gointerop.fhir.service;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.gointerop.fhir.converter.CollectionValueSetConverter;
import com.gointerop.fhir.helper.OperationOutcomeHelper;
import com.gointerop.fhir.helper.ParametersHelper;
import com.gointerop.fhir.model.Collection;
import com.gointerop.fhir.model.Concept;
import com.gointerop.fhir.model.Source;
import com.gointerop.fhir.repository.CollectionRepository;
import com.gointerop.fhir.repository.ConceptRepository;
import com.gointerop.fhir.repository.SourceRepository;
import com.gointerop.fhir.utils.FHIRUtil;

@Service
public class CollectionService {

    @Value("${tx.external.url:https://tx.fhir.org/r4}")
    private String externalTxUrl;

    private static final Set<String> KNOWN_MIME_TYPES = Set.of(
            "audio/aac",
            "application/x-abiword",
            "application/x-freearc",
            "image/avif",
            "video/x-msvideo",
            "application/vnd.amazon.ebook",
            "application/octet-stream",
            "image/bmp",
            "application/x-bzip",
            "application/x-bzip2",
            "application/x-cdf",
            "application/x-csh",
            "text/css",
            "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-fontobject",
            "application/epub+zip",
            "application/gzip",
            "image/gif",
            "text/html",
            "image/vnd.microsoft.icon",
            "text/calendar",
            "application/java-archive",
            "image/jpeg",
            "text/javascript",
            "application/json",
            "application/ld+json",
            "audio/midi",
            "audio/x-midi",
            "audio/mpeg",
            "video/mp4",
            "video/mpeg",
            "application/vnd.apple.installer+xml",
            "application/vnd.oasis.opendocument.presentation",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.text",
            "audio/ogg",
            "video/ogg",
            "application/ogg",
            "audio/opus",
            "font/otf",
            "image/png",
            "application/pdf",
            "application/x-httpd-php",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.rar",
            "application/rtf",
            "application/x-sh",
            "image/svg+xml",
            "application/x-tar",
            "image/tiff",
            "video/mp2t",
            "font/ttf",
            "text/plain",
            "application/vnd.visio",
            "audio/wav",
            "audio/webm",
            "video/webm",
            "image/webp",
            "font/woff",
            "font/woff2",
            "application/xhtml+xml",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/xml",
            "text/xml",
            "application/atom+xml",
            "application/vnd.mozilla.xul+xml",
            "application/zip",
            "video/3gpp",
            "audio/3gpp",
            "video/3gpp2",
            "audio/3gpp2",
            "application/x-7z-compressed",
            "text/hl7v2",
            "text/rtf",
            "application/cda+xml",
            "application/fhir+json",
            "application/fhir+xml");

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
    private ParametersHelper parametersHelper;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private CollectionRepository repository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private CollectionValueSetConverter converter;

    private final RestTemplate restTemplate;

    public CollectionService(RestTemplateBuilder builder) {
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

    @Cacheable(value = "fetchValueSetById", key = "#id")
    public ValueSet fetchValueSetById(String id) {
        Collection collection = repository.fetchAllCollections()
                .stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElse(null);
        if (collection == null) {
            return null;
        }
        return converter.toValueSet(collection);
    }

    @Cacheable(value = "searchValueSets", key = "#url")
    public List<ValueSet> searchValueSets(
            String url,
            String version,
            String name,
            String title,
            String status,
            String publisher,
            String description,
            String jurisdiction,
            Date date,
            String xSystemCacheId) {
        List<ValueSet> results = new ArrayList<>();

        if (url == null)
            return results;

        String valueSetCandidate = url;

        if (!valueSetCandidate.equals("none") && !isAuthoritativeValueSet(valueSetCandidate)) {
            return delegateSearch(url, version, name, title, status, publisher, description, jurisdiction, date,
                    xSystemCacheId);
        }

        String suffixPagedValueSet = null;

        // 2) Pega a Collection do repositório (para buscar conceitos)
        Collection matchedCollection = repository.fetchCollectionsByCanonicalUrl(url)
                .stream()
                .filter(c -> version == null || version.equalsIgnoreCase(c.getVersion()))
                .findFirst()
                .orElse(null);

        ValueSet result = converter.toValueSet(matchedCollection);

        result.setUrl(suffixPagedValueSet != null ? suffixPagedValueSet : result.getUrl());

        int offset = 0;
        int count = 1000;

        // 3) Busca os conceitos dessa Collection
        List<Concept> concepts = null;

        concepts = conceptRepository.fetchConceptsByOrgAndCollection(
                    matchedCollection.getOwner(), matchedCollection.getId(), count, offset);
        
        // 4) Agrupa conceitos por Source.canonical_url
        Map<String, List<Concept>> groupedConcepts = concepts.stream()
                .collect(Collectors.groupingBy(concept -> {
                    Source source = sourceRepository.fetchSourceByOwnerAndId(
                            concept.getOwner(), concept.getSource());
                    String canonicalUrl = source.getCanonicalUrl();
                    if (canonicalUrl == null) {
                        throw new IllegalStateException(
                                "Source canonical URL is null for sourceId: " + concept.getSource());
                    }
                    return canonicalUrl;
                }));

        // 5) Monta Compose com Includes por Source
        result.getCompose().getInclude().clear();

        for (Map.Entry<String, List<Concept>> entry : groupedConcepts.entrySet()) {
            String sourceCanonicalUrl = entry.getKey();
            List<Concept> conceptsInSource = entry.getValue();

            ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
            include.setSystem(sourceCanonicalUrl);

            for (Concept concept : conceptsInSource) {
                ValueSet.ConceptReferenceComponent ref = new ValueSet.ConceptReferenceComponent();
                ref.setCode(concept.getId());
                ref.setDisplay(concept.getDisplayName());

                if (concept.getNames() != null) {
                    concept.getNames().forEach(conceptName -> {
                        ValueSet.ConceptReferenceDesignationComponent designation = new ValueSet.ConceptReferenceDesignationComponent();
                        designation.setLanguage(conceptName.getLocale());
                        designation.setValue(conceptName.getName());
                        ref.addDesignation(designation);
                    });
                }

                include.addConcept(ref);
            }

            result.getCompose().addInclude(include);
        }

        results.add(result);
        
        return results;
    }

    @Cacheable(value = "expandValueSet", keyGenerator = "expandValueSetKeyGenerator")
    public IBaseResource expandValueSet(
            String url,
            ValueSet txResource,
            String version,
            String filter,
            String date,
            Integer offset,
            Integer count,
            boolean includeDesignations,
            String displayLanguage,
            String xSystemCacheId) {
        String valueSetCandidate = url != null ? url
                : txResource != null && txResource.getUrl() != null && !txResource.getUrl().isEmpty()
                        ? txResource.getUrl()
                        : "none";
        String codeSystemCandidate = txResource != null && txResource.getCompose().getIncludeFirstRep() != null
                && txResource.getCompose().getIncludeFirstRep().getSystem() != null
                && !txResource.getCompose().getIncludeFirstRep().getSystem().isEmpty()
                        ? txResource.getCompose().getIncludeFirstRep().getSystem()
                        : "none";
        if (!valueSetCandidate.equals("none") && !isAuthoritativeValueSet(valueSetCandidate)
                || !codeSystemCandidate.equals("none") && !isAuthoritativeCodeSystem(codeSystemCandidate)
                        && valueSetCandidate.equals("none") && xSystemCacheId != null) {
            return delegateExpand(url, txResource, version, filter, date, offset, count, includeDesignations,
                    displayLanguage, xSystemCacheId);
        } else if (!codeSystemCandidate.equals("none") && !isAuthoritativeCodeSystem(codeSystemCandidate)
                || !valueSetCandidate.equals("none") && !isAuthoritativeValueSet(valueSetCandidate)
                        && codeSystemCandidate.equals("none") && xSystemCacheId != null) {
            return delegateExpand(url, txResource, version, filter, date, offset, count, includeDesignations,
                    displayLanguage, xSystemCacheId);
        }

        if (valueSetCandidate.equals("none") && codeSystemCandidate.equals("none") && xSystemCacheId == null) {
            return operationOutcomeHelper.help(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.NOTFOUND,
                    "Unable to find value set to expand (not provided by id, identifier, or directly)");
        }

        String suffixPagedValueSet = null;

        if (txResource != null && txResource.getUrl() != null && txResource.getUrl().contains("-inc-")) {
            String[] parts = txResource.getUrl().split("-inc-");

            txResource.setUrl(parts[0]);

            suffixPagedValueSet = parts[0] + "-inc-" + parts[1];
        }

        // 2) Pega a Collection do repositório (para buscar conceitos)
        Collection matchedCollection = repository.fetchCollectionsByCanonicalUrl(url != null ? url
                : txResource != null && txResource.getUrl() != null ? txResource.getUrl() : "none")
                .stream()
                .filter(c -> version == null || version.equalsIgnoreCase(c.getVersion()))
                .findFirst()
                .orElse(null);

        if (matchedCollection == null) {
            // ValueSet abstrato: definido apenas com compose.include
            if (txResource != null && txResource.hasCompose() && txResource.getCompose().hasInclude()) {
                Map<String, List<Concept>> groupedConcepts = new java.util.HashMap<>();

                for (ValueSet.ConceptSetComponent include : txResource.getCompose().getInclude()) {
                    String system = include.getSystem();
                    if (system == null || system.isEmpty())
                        continue;

                    List<Source> sources = sourceRepository.fetchSourcesByCanonicalUrl(system);
                    if (sources.isEmpty())
                        continue;

                    Source source = sources.get(0);
                    List<Concept> concepts = conceptRepository.fetchConceptsByOrgAndSource(source.getOwner(),
                            source.getId());

                    // Se txResource.define conceitos específicos, filtrar
                    if (include.hasConcept()) {
                        Set<String> allowedCodes = include.getConcept().stream()
                                .map(ValueSet.ConceptReferenceComponent::getCode)
                                .collect(Collectors.toSet());

                        concepts = concepts.stream()
                                .filter(c -> allowedCodes.contains(c.getId()))
                                .collect(Collectors.toList());
                    }

                    if (filter != null && !filter.isBlank()) {
                        String filterLower = filter.toLowerCase();
                        concepts = concepts.stream()
                                .filter(c -> c.getEffectiveDisplay() != null &&
                                        c.getEffectiveDisplay().toLowerCase().contains(filterLower))
                                .collect(Collectors.toList());
                    }

                    groupedConcepts.put(system, concepts);
                }

                // 6) Prepara expansão final
                ValueSetExpansionComponent expansion = new ValueSetExpansionComponent();
                expansion.setTimestamp(new Date());

                for (Map.Entry<String, List<Concept>> entry : groupedConcepts.entrySet()) {
                    String system = entry.getKey();
                    for (Concept concept : entry.getValue()) {
                        ValueSetExpansionContainsComponent contains = new ValueSetExpansionContainsComponent();
                        contains.setSystem(system);
                        contains.setCode(concept.getId());
                        contains.setDisplay(concept.getDisplayName());

                        if (includeDesignations && concept.getNames() != null) {
                            concept.getNames().forEach(name -> {
                                ValueSet.ConceptReferenceDesignationComponent d = new ValueSet.ConceptReferenceDesignationComponent();
                                d.setLanguage(name.getLocale());
                                d.setValue(name.getName());
                                contains.addDesignation(d);
                            });
                        }

                        expansion.addContains(contains);
                    }
                }

                ValueSet result = new ValueSet();
                result.setUrl("urn:uuid:" + java.util.UUID.randomUUID());
                result.setStatus(Enumerations.PublicationStatus.DRAFT);
                result.setExpansion(expansion);
                result.getText().setStatus(NarrativeStatus.GENERATED);
                result.getText()
                        .setDivAsString("<div xmlns=\"http://www.w3.org/1999/xhtml\">Expanded Abstract ValueSet</div>");

                return result;
            }

            // Nenhuma expansão possível
            return new ValueSet();
        }

        ValueSet result = converter.toValueSet(matchedCollection);

        result.setUrl(suffixPagedValueSet != null ? suffixPagedValueSet : result.getUrl());

        offset = (offset == null || offset < 0) ? 0 : offset;
        count = (count == null || count <= 0) ? 1000 : count;

        // 3) Busca os conceitos dessa Collection
        List<Concept> concepts = null;

        if (filter == null) {
            concepts = conceptRepository.fetchConceptsByOrgAndCollection(
                    matchedCollection.getOwner(), matchedCollection.getId(), count, offset);
        } else {
            concepts = conceptRepository.fetchConceptsByOrgAndCollectionAndFilter(
                    matchedCollection.getOwner(), matchedCollection.getId(), count, offset, filter);
        }

        // 4) Agrupa conceitos por Source.canonical_url
        Map<String, List<Concept>> groupedConcepts = concepts.stream()
                .filter(concept -> {
                    if (filter == null)
                        return true;
                    String display = concept.getEffectiveDisplay();
                    return display != null && display.toLowerCase().contains(filter.toLowerCase());
                })
                .collect(Collectors.groupingBy(concept -> {
                    Source source = sourceRepository.fetchSourceByOwnerAndId(
                            concept.getOwner(), concept.getSource());
                    String canonicalUrl = source.getCanonicalUrl();
                    if (canonicalUrl == null) {
                        throw new IllegalStateException(
                                "Source canonical URL is null for sourceId: " + concept.getSource());
                    }
                    return canonicalUrl;
                }));

        // 5) Monta Compose com Includes por Source
        result.getCompose().getInclude().clear();

        for (Map.Entry<String, List<Concept>> entry : groupedConcepts.entrySet()) {
            String sourceCanonicalUrl = entry.getKey();
            List<Concept> conceptsInSource = entry.getValue();

            ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
            include.setSystem(sourceCanonicalUrl);

            for (Concept concept : conceptsInSource) {
                ValueSet.ConceptReferenceComponent ref = new ValueSet.ConceptReferenceComponent();
                ref.setCode(concept.getId());
                ref.setDisplay(concept.getDisplayName());

                if (includeDesignations && concept.getNames() != null) {
                    concept.getNames().forEach(name -> {
                        ValueSet.ConceptReferenceDesignationComponent designation = new ValueSet.ConceptReferenceDesignationComponent();
                        designation.setLanguage(name.getLocale());
                        designation.setValue(name.getName());
                        ref.addDesignation(designation);
                    });
                }

                include.addConcept(ref);
            }

            result.getCompose().addInclude(include);
        }

        // 6) Prepara expansão final
        ValueSetExpansionComponent expansion = new ValueSetExpansionComponent();
        expansion.setTimestamp(new Date());

        // Adiciona os conceitos agrupados
        for (ValueSet.ConceptSetComponent include : result.getCompose().getInclude()) {
            for (ValueSet.ConceptReferenceComponent concept : include.getConcept()) {
                ValueSetExpansionContainsComponent contains = new ValueSetExpansionContainsComponent();
                contains.setSystem(include.getSystem());
                contains.setCode(concept.getCode());
                contains.setDisplay(concept.getDisplay());
                if (includeDesignations && concept.hasDesignation()) {
                    contains.getDesignation().addAll(concept.getDesignation());
                }
                expansion.addContains(contains);
            }
        }

        // 7) Exclude: remove conceitos explicitamente excluídos
        if (result.getCompose().hasExclude()) {
            List<String> codesToExclude = new ArrayList<>();
            for (ValueSet.ConceptSetComponent exclude : result.getCompose().getExclude()) {
                for (ValueSet.ConceptReferenceComponent c : exclude.getConcept()) {
                    codesToExclude.add(c.getCode());
                }
            }
            expansion.getContains().removeIf(c -> codesToExclude.contains(c.getCode()));
        }

        result.setExpansion(expansion);

        result.getText().setStatus(NarrativeStatus.GENERATED);
        result.getText().setDivAsString("<div xmlns=\"http://www.w3.org/1999/xhtml\">Expanded ValueSet</div>");

        return result;
    }

    @Cacheable(value = "validateCode", key = "#url + '_' + #txResource?.url + '_' + #code + '_' + #version + '_' + '_' + #coding + '_' + #codeableConcept + '_' + #xSystemCacheId")
    public IBaseResource validateCodeInValueSet(
            String url,
            ValueSet txResource,
            String version,
            String code,
            String system,
            String display,
            Coding coding,
            CodeableConcept codeableConcept,
            String displayLanguage,
            String xSystemCacheId) {

        String valueSetCandidate = url != null ? url
                : txResource != null && txResource.getUrl() != null && !txResource.getUrl().isEmpty()
                        ? txResource.getUrl()
                        : "none";
        String codeSystemCandidate = system != null ? system
                : txResource != null && txResource.getCompose().getIncludeFirstRep() != null
                        && txResource.getCompose().getIncludeFirstRep().getSystem() != null
                        && !txResource.getCompose().getIncludeFirstRep().getSystem().isEmpty()
                                ? txResource.getCompose().getIncludeFirstRep().getSystem()
                                : "none";
        if (!valueSetCandidate.equals("none") && !isAuthoritativeValueSet(valueSetCandidate)
                || !codeSystemCandidate.equals("none") && !isAuthoritativeCodeSystem(codeSystemCandidate)
                        && valueSetCandidate.equals("none") && xSystemCacheId != null) {
            return delegateValidateCode(url, txResource, version, code, system, display, coding, codeableConcept,
                    displayLanguage, xSystemCacheId);
        } else if (!codeSystemCandidate.equals("none") && !isAuthoritativeCodeSystem(codeSystemCandidate)
                || !valueSetCandidate.equals("none") && !isAuthoritativeValueSet(valueSetCandidate)
                        && codeSystemCandidate.equals("none") && xSystemCacheId != null) {
            return delegateValidateCode(url, txResource, version, code, system, display, coding, codeableConcept,
                    displayLanguage, xSystemCacheId);
        }

        if (valueSetCandidate.equals("none") && codeSystemCandidate.equals("none") && xSystemCacheId == null) {
            return operationOutcomeHelper.help(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.NOTFOUND,
                    "Unable to find value set to validate code (not provided by id, identifier, or directly)");
        }

        List<ValueSetExpansionContainsComponent> expansionList = new ArrayList<>();

        if (txResource != null) {
            // ✅ Usa expansão se já existir
            if (txResource.hasExpansion() && txResource.getExpansion().hasContains()) {
                expansionList.addAll(txResource.getExpansion().getContains());
            } else {
                // ⚡️ Se não vier expandido, monta com compose.include
                if (!txResource.hasCompose() || txResource.getCompose().getInclude().isEmpty()) {
                    return operationOutcomeHelper.help(
                            OperationOutcome.IssueSeverity.ERROR,
                            OperationOutcome.IssueType.NOTFOUND,
                            "ValueSet does not have compose.include to expand.");
                }
                for (ValueSet.ConceptSetComponent include : txResource.getCompose().getInclude()) {
                    if (system == null)
                        system = include.getSystem();

                    for (ValueSet.ConceptReferenceComponent concept : include.getConcept()) {
                        ValueSetExpansionContainsComponent contains = new ValueSetExpansionContainsComponent();
                        contains.setSystem(system);
                        contains.setCode(concept.getCode());
                        contains.setDisplay(concept.getDisplay());
                        expansionList.add(contains);
                    }
                }
            }
        } else {
            // ✅ Collection → expande com conceitos reais
            Collection matchedCollection = repository.fetchCollectionsByCanonicalUrl(url)
                    .stream()
                    .filter(c -> url.equalsIgnoreCase(c.getCanonicalUrl()))
                    .findFirst()
                    .orElse(null);

            if (matchedCollection == null) {
                return operationOutcomeHelper.help(
                        OperationOutcome.IssueSeverity.ERROR,
                        OperationOutcome.IssueType.NOTFOUND,
                        "No collection found for the provided URL.");
            }

            String org = matchedCollection.getOwner();
            String collectionId = matchedCollection.getId();

            Concept concept = conceptRepository.fetchConceptByCollection(org, collectionId, code);
            if (concept == null) {
                return operationOutcomeHelper.help(
                        OperationOutcome.IssueSeverity.ERROR,
                        OperationOutcome.IssueType.NOTFOUND,
                        "No concept found for code '" + code + "' in collection.");
            }

            ValueSetExpansionContainsComponent contains = new ValueSetExpansionContainsComponent();
            contains.setSystem(matchedCollection.getCanonicalUrl());
            contains.setCode(concept.getId());
            contains.setDisplay(concept.getDisplayName());
            expansionList.add(contains);
        }

        // ✅ Exclude aplica sempre (se compose estiver presente)
        if (txResource != null && txResource.hasCompose() && txResource.getCompose().hasExclude()) {
            List<String> codesToExclude = txResource.getCompose().getExclude().stream()
                    .flatMap(exclude -> exclude.getConcept().stream())
                    .map(ValueSet.ConceptReferenceComponent::getCode)
                    .collect(Collectors.toList());
            expansionList.removeIf(c -> codesToExclude.contains(c.getCode()));
        }

        // 🔍 Verifica na expansão real
        boolean found = false;
        String effectiveDisplay = null;

        for (ValueSetExpansionContainsComponent contains : expansionList) {
            if (code != null && code.equalsIgnoreCase(contains.getCode())) {
                found = true;
                effectiveDisplay = contains.getDisplay();
                break;
            }
            if (coding != null && coding.getCode() != null && coding.getCode().equalsIgnoreCase(contains.getCode())) {
                found = true;
                effectiveDisplay = contains.getDisplay();
                break;
            }
            if (codeableConcept != null && codeableConcept.hasCoding()) {
                for (Coding c : codeableConcept.getCoding()) {
                    if (c.getCode() != null && c.getCode().equalsIgnoreCase(contains.getCode())) {
                        found = true;
                        effectiveDisplay = contains.getDisplay();
                        break;
                    }
                }
            }
        }

        if (!found) {
            return operationOutcomeHelper.help(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.NOTFOUND,
                    "Code not found in ValueSet expansion.");
        }

        if (display != null && effectiveDisplay != null && !display.equalsIgnoreCase(effectiveDisplay)) {
            return operationOutcomeHelper.help(
                    OperationOutcome.IssueSeverity.WARNING,
                    OperationOutcome.IssueType.VALUE,
                    "Code found in ValueSet expansion, but display does not match. Expected: '"
                            + effectiveDisplay + "', provided: '" + display + "'.");
        }

        Parameters params = new Parameters();
        params.addParameter().setName("result").setValue(new BooleanType(true));
        if (effectiveDisplay != null) {
            params.addParameter().setName("display").setValue(new StringType(effectiveDisplay));
        }

        return params;
    }

    @Cacheable(value = "delegateExpand", key = "#url + '_' + #txResource?.url + '_' + #version + #xSystemCacheId")
    private IBaseResource delegateExpand(
            String url,
            ValueSet txResource,
            String version,
            String filter,
            String date,
            Integer offset,
            Integer count,
            boolean includeDesignations,
            String displayLanguage,
            String xSystemCacheId) {

        Parameters params = new Parameters();
        if (url != null) {
            params.addParameter().setName("url").setValue(new UriType(url));
        }
        if (txResource != null) {
            params.addParameter().setName("valueSet").setResource(txResource);
        }
        if (version != null) {
            params.addParameter().setName("version").setValue(new StringType(version));
        }
        if (filter != null) {
            params.addParameter().setName("filter").setValue(new StringType(filter));
        }
        if (date != null) {
            params.addParameter().setName("date").setValue(new StringType(date));
        }
        if (offset != null && offset >= 0) {
            params.addParameter().setName("offset").setValue(new IntegerType(offset));
        }
        if (count != null && count >= 0) {
            params.addParameter().setName("count").setValue(new IntegerType(count));
        }
        if (includeDesignations) {
            params.addParameter().setName("includeDesignations").setValue(new BooleanType(true));
        }
        if (displayLanguage != null) {
            params.addParameter().setName("displayLanguage").setValue(new StringType(displayLanguage));
        }

        if (xSystemCacheId != null) {
            params.addParameter().setName("x-system-cache-id").setValue(new StringType(xSystemCacheId));
        }

        String body = fhirUtil.getFhirContext().newJsonParser().encodeResourceToString(params);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/fhir+json");
        headers.add("Content-Type", "application/fhir+json");

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        String responseJson = null;
        ValueSet response = null;

        try {
            responseJson = restTemplate.postForObject(
                    externalTxUrl + "/ValueSet/$expand",
                    request,
                    String.class);
            response = fhirUtil.getFhirContext().newJsonParser().parseResource(ValueSet.class, responseJson);
        } catch (Exception e) {
            return operationOutcomeHelper.help(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.NOTFOUND,
                    "Unable to expand ValueSet: "
                            + (url != null ? url : txResource != null ? txResource.getUrl() : "unknown"));
        }

        return response;
    }

    @Cacheable(value = "delegateSearch", key = "#url + '_' + #version + '_' + #name + '_' + #title + '_' + #status + '_' + #publisher + '_' + #description + '_' + #jurisdiction + '_' + #date + '_' + #xSystemCacheId")
    private List<ValueSet> delegateSearch(
            String url,
            String version,
            String name,
            String title,
            String status,
            String publisher,
            String description,
            String jurisdiction,
            Date date,
            String xSystemCacheId) {

        // Build query string with search parameters
        StringBuilder query = new StringBuilder("?");
        if (url != null) {
            query.append("url=").append(url).append("&");
        }
        if (version != null) {
            query.append("version=").append(version).append("&");
        }
        if (name != null) {
            query.append("name=").append(name).append("&");
        }
        if (title != null) {
            query.append("title=").append(title).append("&");
        }
        if (status != null) {
            query.append("status=").append(status).append("&");
        }
        if (publisher != null) {
            query.append("publisher=").append(publisher).append("&");
        }
        if (description != null) {
            query.append("description=").append(description).append("&");
        }
        if (jurisdiction != null) {
            query.append("jurisdiction=").append(jurisdiction).append("&");
        }
        if (date != null) {
            query.append("date=").append(date.toString()).append("&");
        }
        if (xSystemCacheId != null) {
            query.append("x-system-cache-id=").append(xSystemCacheId).append("&");
        }
        // Remove trailing '&' if present
        if (query.length() > 1 && query.charAt(query.length() - 1) == '&') {
            query.deleteCharAt(query.length() - 1);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/fhir+json");
        headers.add("Content-Type", "application/fhir+json");

        HttpEntity<String> request = new HttpEntity<>(null, headers);

        String responseJson = null;
        List<ValueSet> valueSets = new ArrayList<>();

        try {
            org.springframework.http.ResponseEntity<String> responseEntity = restTemplate.exchange(
                    externalTxUrl + "/ValueSet" + query.toString(),
                    org.springframework.http.HttpMethod.GET,
                    request,
                    String.class);

            responseJson = responseEntity.getBody();

            org.hl7.fhir.r4.model.Bundle bundle = fhirUtil.getFhirContext().newJsonParser()
                    .parseResource(org.hl7.fhir.r4.model.Bundle.class, responseJson);

            for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof ValueSet) {
                    valueSets.add((ValueSet) entry.getResource());
                }
            }
        } catch (Exception e) {
            // Return empty list or handle error as needed
        }

        return valueSets;
    }

    @Cacheable(value = "delegateValidateCode", key = "#url + '_' + #txResource?.url + '_' + #code + '_' + #version + '_' + #system + '_' + '_' + #coding + '_' + #codeableConcept + '_' + '_' + #xSystemCacheId")
    private IBaseResource delegateValidateCode(
            String url,
            ValueSet txResource,
            String version,
            String code,
            String system,
            String display,
            Coding coding,
            CodeableConcept codeableConcept,
            String displayLanguage,
            String xSystemCacheId) {

        Parameters params = new Parameters();

        if (url != null) {
            params.addParameter().setName("url").setValue(new UriType(url));
        }
        if (version != null) {
            params.addParameter().setName("version").setValue(new StringType(version));
        }
        if (code != null) {
            params.addParameter().setName("code").setValue(new StringType(code));
        }

        if (system != null) {
            params.addParameter().setName("system").setValue(new UriType(system));
        } else if (code != null) {
            if (isKnownMimeType(code)) {
                params.addParameter().setName("system").setValue(new UriType("urn:ietf:bcp:13"));
            } else {
                params.addParameter().setName("system").setValue(new UriType("http://unitsofmeasure.org"));
            }
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

        if (txResource != null) {
            params.addParameter().setName("valueSet").setResource(txResource);
        }

        String body = fhirUtil.getFhirContext().newJsonParser().encodeResourceToString(params);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/fhir+json");
        headers.add("Content-Type", "application/fhir+json");

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        String responseJson = null;
        Parameters response = null;

        try {
            responseJson = restTemplate.postForObject(
                    externalTxUrl + "/ValueSet/$validate-code",
                    request,
                    String.class);

            response = fhirUtil.getFhirContext().newJsonParser().parseResource(Parameters.class, responseJson);
        } catch (Exception e) {
            String codeError = code != null ? code
                    : coding != null && coding.getCode() != null ? coding.getCode()
                            : codeableConcept != null && codeableConcept.getCodingFirstRep() != null
                                    ? codeableConcept.getCodingFirstRep().getCode()
                                    : "unknown";
            String systemError = url != null ? url
                    : txResource != null && txResource.getUrl() != null ? txResource.getUrl()
                            : txResource != null && txResource.getCompose() != null
                                    && txResource.getCompose().getIncludeFirstRep() != null
                                            ? txResource.getCompose().getIncludeFirstRep().getSystem()
                                            : "unknown";
            return operationOutcomeHelper.help(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.NOTFOUND,
                    "Code " + codeError + " not found in ValueSet: " + systemError);
        }

        return response;
    }

    private boolean isKnownMimeType(String code) {
        return code != null && KNOWN_MIME_TYPES.contains(code.toLowerCase());
    }

    private boolean isAuthoritativeCodeSystem(String url) {
        return url != null && authoritativeCodeSystems.stream().anyMatch(url::startsWith);
    }

    private boolean isAuthoritativeValueSet(String url) {
        return url != null && authoritativeValuesets.stream().anyMatch(url::startsWith);
    }
}

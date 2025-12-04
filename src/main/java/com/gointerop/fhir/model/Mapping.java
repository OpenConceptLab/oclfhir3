package com.gointerop.fhir.model;

import java.io.Serializable;
import java.util.function.BiFunction;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.r4.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.r4.model.ConceptMap.TargetElementComponent;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Mapping implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String url;

    @JsonProperty("map_type")
    private String mapType;

    @JsonProperty("retired")
    private Boolean retired;

    @JsonProperty("from_concept_code")
    private String fromConceptCode;

    @JsonProperty("from_concept_name")
    private String fromConceptName;

    @JsonProperty("from_concept_name_resolved")
    private String fromConceptNameResolved;

    @JsonProperty("from_concept_url")
    private String fromConceptUrl;

    @JsonProperty("from_source_owner")
    private String fromSourceOwner;

    @JsonProperty("from_source_owner_type")
    private String fromSourceOwnerType;

    @JsonProperty("from_source")
    private String fromSource;

    @JsonProperty("from_source_name")
    private String fromSourceName;

    @JsonProperty("from_source_url")
    private String fromSourceUrl;

    @JsonProperty("from_source_version")
    private String fromSourceVersion;

    @JsonProperty("from_collection_owner")
    private String fromCollectionOwner;

    @JsonProperty("from_collection")
    private String fromCollection;

    @JsonProperty("from_collection_url")
    private String fromCollectionUrl;

    @JsonProperty("to_concept_code")
    private String toConceptCode;

    @JsonProperty("to_concept_name")
    private String toConceptName;

    @JsonProperty("to_concept_name_resolved")
    private String toConceptNameResolved;

    @JsonProperty("to_concept_url")
    private String toConceptUrl;

    @JsonProperty("to_source_owner")
    private String toSourceOwner;

    @JsonProperty("to_source_owner_type")
    private String toSourceOwnerType;

    @JsonProperty("to_source")
    private String toSource;

    @JsonProperty("to_source_name")
    private String toSourceName;

    @JsonProperty("to_source_url")
    private String toSourceUrl;

    @JsonProperty("to_source_version")
    private String toSourceVersion;

    @JsonProperty("to_collection_owner")
    private String toCollectionOwner;

    @JsonProperty("to_collection")
    private String toCollection;

    @JsonProperty("to_collection_url")
    private String toCollectionUrl;

    public Coding toTargetCoding(
            BiFunction<String, String, Source> sourceResolver,
            BiFunction<String, String, Collection> collectionResolver) {
        if (!StringUtils.hasText(toConceptCode)) {
            return null;
        }

        Coding coding = new Coding();
        coding.setCode(toConceptCode);
        coding.setDisplay(firstNonBlank(toConceptNameResolved, toConceptName));

        String system = getToSourceCanonical(sourceResolver);
        if (system == null) {
            system = getToCollectionCanonical(collectionResolver);
        }
        if (system == null) {
            system = toConceptUrl;
        }
        coding.setSystem(system);
        return coding;
    }

    public Coding toSourceCoding(
            BiFunction<String, String, Source> sourceResolver,
            BiFunction<String, String, Collection> collectionResolver) {
        if (!StringUtils.hasText(fromConceptCode)) {
            return null;
        }

        Coding coding = new Coding();
        coding.setCode(fromConceptCode);
        coding.setDisplay(firstNonBlank(fromConceptNameResolved, fromConceptName));

        String system = getFromSourceCanonical(sourceResolver);
        if (system == null) {
            system = getFromCollectionCanonical(collectionResolver);
        }
        if (system == null) {
            system = fromConceptUrl;
        }
        coding.setSystem(system);
        return coding;
    }

    public String getToSourceCanonical(BiFunction<String, String, Source> sourceResolver) {
        String owner = firstNonBlank(toSourceOwner, ownerFromUrl(toSourceUrl));
        String identifier = firstNonBlank(toSource, sourceSlugFromUrl(toSourceUrl));

        if (StringUtils.hasText(owner) && StringUtils.hasText(identifier)) {
            Source source = sourceResolver.apply(owner, identifier);
            if (source != null && StringUtils.hasText(source.getCanonicalUrl())) {
                return source.getCanonicalUrl();
            }
        }

        if (StringUtils.hasText(toSourceUrl)) {
            return toSourceUrl;
        }
        return null;
    }

    public String getToCollectionCanonical(BiFunction<String, String, Collection> collectionResolver) {
        String owner = firstNonBlank(toCollectionOwner, ownerFromCollectionUrl(toCollectionUrl));
        String identifier = firstNonBlank(toCollection, collectionSlugFromUrl(toCollectionUrl));

        if (StringUtils.hasText(owner) && StringUtils.hasText(identifier)) {
            Collection collection = collectionResolver.apply(owner, identifier);
            if (collection != null && StringUtils.hasText(collection.getCanonicalUrl())) {
                return collection.getCanonicalUrl();
            }
        }

        if (StringUtils.hasText(toCollectionUrl)) {
            return toCollectionUrl;
        }
        return null;
    }

    public String getFromSourceCanonical(BiFunction<String, String, Source> sourceResolver) {
        String owner = firstNonBlank(fromSourceOwner, ownerFromUrl(fromSourceUrl));
        String identifier = firstNonBlank(fromSource, sourceSlugFromUrl(fromSourceUrl));

        if (StringUtils.hasText(owner) && StringUtils.hasText(identifier)) {
            Source source = sourceResolver.apply(owner, identifier);
            if (source != null && StringUtils.hasText(source.getCanonicalUrl())) {
                return source.getCanonicalUrl();
            }
        }

        if (StringUtils.hasText(fromSourceUrl)) {
            return fromSourceUrl;
        }
        return null;
    }

    public String getFromCollectionCanonical(BiFunction<String, String, Collection> collectionResolver) {
        String owner = firstNonBlank(fromCollectionOwner, ownerFromCollectionUrl(fromCollectionUrl));
        String identifier = firstNonBlank(fromCollection, collectionSlugFromUrl(fromCollectionUrl));

        if (StringUtils.hasText(owner) && StringUtils.hasText(identifier)) {
            Collection collection = collectionResolver.apply(owner, identifier);
            if (collection != null && StringUtils.hasText(collection.getCanonicalUrl())) {
                return collection.getCanonicalUrl();
            }
        }

        if (StringUtils.hasText(fromCollectionUrl)) {
            return fromCollectionUrl;
        }
        return null;
    }

    public static Mapping fromConceptMap(
            ConceptMapGroupComponent group,
            SourceElementComponent element,
            TargetElementComponent target,
            boolean reverse) {
        Mapping mapping = new Mapping();
        if (target != null && target.getEquivalence() != null) {
            mapping.setMapType(target.getEquivalence().toCode());
        }

        if (reverse) {
            mapping.setToConceptCode(element.getCode());
            mapping.setToConceptName(element.getDisplay());
            mapping.setToConceptUrl(group.getSource());

            mapping.setFromConceptCode(target.getCode());
            mapping.setFromConceptName(target.getDisplay());
            mapping.setFromConceptUrl(group.getTarget());
        } else {
            mapping.setFromConceptCode(element.getCode());
            mapping.setFromConceptName(element.getDisplay());
            mapping.setFromConceptUrl(group.getSource());

            mapping.setToConceptCode(target.getCode());
            mapping.setToConceptName(target.getDisplay());
            mapping.setToConceptUrl(group.getTarget());
        }

        return mapping;
    }

    private String firstNonBlank(String primary, String secondary) {
        if (StringUtils.hasText(primary)) {
            return primary;
        }
        if (StringUtils.hasText(secondary)) {
            return secondary;
        }
        return null;
    }

    private String ownerFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String[] segments = url.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if ("orgs".equals(segments[i]) && StringUtils.hasText(segments[i + 1])) {
                return segments[i + 1];
            }
        }
        return null;
    }

    private String sourceSlugFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String[] segments = url.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if ("sources".equals(segments[i]) && StringUtils.hasText(segments[i + 1])) {
                return segments[i + 1];
            }
        }
        return null;
    }

    private String ownerFromCollectionUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String[] segments = url.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if ("collections".equals(segments[i]) && i >= 2 && "orgs".equals(segments[i - 2])) {
                return segments[i - 1];
            }
        }
        return ownerFromUrl(url);
    }

    private String collectionSlugFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String[] segments = url.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if ("collections".equals(segments[i]) && StringUtils.hasText(segments[i + 1])) {
                return segments[i + 1];
            }
        }
        return null;
    }
}

package com.gointerop.fhir.caching;

import java.lang.reflect.Method;

import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import com.gointerop.fhir.utils.FHIRUtil;

@Component
public class TranslateKeyGenerator implements KeyGenerator {

    @Autowired
    private FHIRUtil fhirUtil;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String url = (String) params[0];
        ConceptMap conceptMap = (ConceptMap) params[1];
        String conceptMapVersion = (String) params[2];
        String code = (String) params[3];
        String system = (String) params[4];
        String version = (String) params[5];
        Coding coding = (Coding) params[6];
        CodeableConcept codeableConcept = (CodeableConcept) params[7];
        String targetParam = (String) params[8];
        String targetSystem = (String) params[9];
        Boolean reverse = (Boolean) params[10];
        String xSystemCacheId = (String) params[11];

        String txKey;
        if (conceptMap != null && conceptMap.getUrl() != null) {
            txKey = conceptMap.getUrl();
        } else if (conceptMap != null) {
            String json = fhirUtil.getIParser().encodeResourceToString(conceptMap);
            txKey = DigestUtils.md5Hex(json);
        } else {
            txKey = "null-resource";
        }

        return String.join("_",
                url,
                txKey,
                conceptMapVersion,
                code,
                system,
                version,
                String.valueOf(coding != null ? coding.hashCode() : "null"),
                String.valueOf(codeableConcept != null ? codeableConcept.hashCode() : "null"),
                targetParam,
                targetSystem,
                String.valueOf(reverse),
                xSystemCacheId);
    }
}
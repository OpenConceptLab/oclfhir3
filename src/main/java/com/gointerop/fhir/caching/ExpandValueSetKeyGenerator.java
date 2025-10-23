package com.gointerop.fhir.caching;

import java.lang.reflect.Method;

import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import com.gointerop.fhir.utils.FHIRUtil;

@Component
public class ExpandValueSetKeyGenerator implements KeyGenerator {

    @Autowired
    private FHIRUtil fhirUtil;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String url = (String) params[0];
        ValueSet txResource = (ValueSet) params[1];
        String version = (String) params[2];
        String filter = (String) params[3];
        String date = (String) params[4];
        Integer offset = (Integer) params[5];
        Integer count = (Integer) params[6];
        boolean includeDesignations = (boolean) params[7];
        String displayLanguage = (String) params[8];
        String xSystemCacheId = (String) params[9];

        String txKey;
        if (txResource != null && txResource.getUrl() != null) {
            txKey = txResource.getUrl();
        } else if (txResource != null) {
            String json = fhirUtil.getIParser().encodeResourceToString(txResource);
            txKey = DigestUtils.md5Hex(json);
        } else {
            txKey = "null-resource";
        }

        return String.join("_",
                url,
                txKey,
                version,
                filter,
                date,
                String.valueOf(offset),
                String.valueOf(count),
                String.valueOf(includeDesignations),
                displayLanguage,
                xSystemCacheId);
    }
}
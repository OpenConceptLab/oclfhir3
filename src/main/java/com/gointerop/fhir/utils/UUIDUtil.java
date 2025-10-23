package com.gointerop.fhir.utils;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class UUIDUtil {
    public String encodeToUUID(Long value) {
        return new UUID(0L, value).toString();
    }

    // Decodes a UUID back to a Long value
    public Long decodeFromUUID(UUID uuid) {
        return uuid.getLeastSignificantBits();
    }
}

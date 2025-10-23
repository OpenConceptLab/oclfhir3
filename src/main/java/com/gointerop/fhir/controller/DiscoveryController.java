package com.gointerop.fhir.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.gointerop.fhir.model.DiscoveryResponse;
import com.gointerop.fhir.model.ResolutionResponse;
import com.gointerop.fhir.service.DiscoveryService;

@RestController
@RequestMapping("/tx-reg")
public class DiscoveryController {

    @Autowired
    private DiscoveryService discoveryService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DiscoveryResponse> discover(
        @RequestParam(required = false) String registry,
        @RequestParam(required = false) String server,
        @RequestParam(required = false) String fhirVersion,
        @RequestParam(required = false) String url,
        @RequestParam(required = false, defaultValue = "false") boolean authoritativeOnly
    ) {
        DiscoveryResponse response = discoveryService.discover(registry, server, fhirVersion, url, authoritativeOnly);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResolutionResponse> resolve(
        @RequestParam String fhirVersion,
        @RequestParam(required = false) String url,
        @RequestParam(required = false) String valueSet,
        @RequestParam(required = false, defaultValue = "false") boolean authoritativeOnly
    ) {
        if (url == null && valueSet == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either url or valueSet must be provided");
        }

        ResolutionResponse response = discoveryService.resolve(fhirVersion, url, valueSet, authoritativeOnly);
        
        return ResponseEntity.ok(response);
    }
}

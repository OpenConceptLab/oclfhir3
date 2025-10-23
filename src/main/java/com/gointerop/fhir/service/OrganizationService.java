package com.gointerop.fhir.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gointerop.fhir.converter.OrgOrganizationConverter;
import com.gointerop.fhir.model.Organization;
import com.gointerop.fhir.repository.OrganizationRepository;

@Service
public class OrganizationService {

    @Autowired
    private OrganizationRepository repository;

    @Autowired
    private OrgOrganizationConverter converter;

    public org.hl7.fhir.r4.model.Organization getOrganizationById(String id) {
        Organization org = repository.fetchOrganizationById(id);
        return converter.toFhirOrganization(org);
    }

}

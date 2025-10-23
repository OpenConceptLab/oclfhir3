package com.gointerop.fhir.provider;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.instance.model.api.IBaseConformance;

import com.gointerop.fhir.service.TerminologyCapabilitiesService;

import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;

public class TerminologyCapabilitiesProvider extends ServerCapabilityStatementProvider {

	private final TerminologyCapabilitiesService terminologyCapabilitiesService;

	public TerminologyCapabilitiesProvider(
			RestfulServer theServer,
			TerminologyCapabilitiesService terminologyCapabilitiesService) {
		super(theServer);
		this.terminologyCapabilitiesService = terminologyCapabilitiesService;
	}

	@Metadata(cacheMillis = 0)
	public IBaseConformance getMetadataResource(HttpServletRequest request, RequestDetails requestDetails) {
		if (request.getParameter("mode") != null && request.getParameter("mode").equals("terminology")) {
			System.out.println("Returning TerminologyCapabilities");
			return terminologyCapabilitiesService.getTerminologyCapabilities();
		} else {
			System.out.println("Returning CapabilityStatement");
			return super.getServerConformance(request, requestDetails);
		}
	}
}
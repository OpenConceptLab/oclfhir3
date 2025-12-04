package com.gointerop.fhir.hapi;

import java.util.ArrayList;

import javax.servlet.annotation.WebServlet;

import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gointerop.fhir.provider.CodeSystemProvider;
import com.gointerop.fhir.provider.ConceptMapProvider;
import com.gointerop.fhir.provider.TerminologyCapabilitiesProvider;
import com.gointerop.fhir.provider.ValueSetProvider;
import com.gointerop.fhir.service.TerminologyCapabilitiesService;
import com.gointerop.fhir.utils.FHIRUtil;

import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.parser.StrictErrorHandler;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

@WebServlet("/*")
@Component
public class HapiRestfulServer extends RestfulServer {

    private static final long serialVersionUID = 2467468320139726878L;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Autowired
    private FHIRUtil fhirUtil;

    @Autowired
    private TerminologyCapabilitiesService terminologyCapabilitiesService;

    @Autowired
    private CodeSystemProvider codeSystemProvider;

    @Autowired
    private ValueSetProvider valueSetProvider;

    @Autowired
    private ConceptMapProvider conceptMapProvider;

    @Override
    protected void initialize() {
        // Leniencia de handler para atributos (Para o TerminologyCapabilities
        // funcionar)
        final LenientErrorHandler delegateHandler = new LenientErrorHandler();
        fhirUtil.getFhirContext().setParserErrorHandler(new StrictErrorHandler() {
            @Override
            public void unknownAttribute(IParseLocation theLocation, String theAttributeName) {
                delegateHandler.unknownAttribute(theLocation, theAttributeName);
            }

            @Override
            public void unknownElement(IParseLocation theLocation, String theElementName) {
                delegateHandler.unknownElement(theLocation, theElementName);
            }

            @Override
            public void unknownReference(IParseLocation theLocation, String theReference) {
                delegateHandler.unknownReference(theLocation, theReference);
            }
        });

        // context
        setFhirContext(fhirUtil.getFhirContext());

        setServerConformanceProvider(new TerminologyCapabilitiesProvider(this, terminologyCapabilitiesService));

        registerProvider(codeSystemProvider);
        registerProvider(valueSetProvider);
        registerProvider(conceptMapProvider);

        // ui
        registerInterceptor(new ResponseHighlighterInterceptor() {
            @Override
            public void capabilityStatementGenerated(RequestDetails theRequestDetails,
                    IBaseConformance theCapabilityStatement) {
                if (theCapabilityStatement instanceof CapabilityStatement) {
                    CapabilityStatement statement = (CapabilityStatement) theCapabilityStatement;
                    statement.setPublisherElement(new StringType("GOInterop Tecnologia LTDA"));
                    statement.setFormat(new ArrayList<>());
                    statement.addFormat(Constants.CT_FHIR_JSON_NEW);
                    statement.addFormat(Constants.CT_FHIR_XML_NEW);
                    super.capabilityStatementGenerated(theRequestDetails, theCapabilityStatement);
                }
            }
        });

        // capability statement
        //registerInterceptor(new CapabilityStatementCustomizer());
    }
}
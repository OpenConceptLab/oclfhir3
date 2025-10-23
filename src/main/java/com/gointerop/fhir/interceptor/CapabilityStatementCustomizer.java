package com.gointerop.fhir.interceptor;

import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.DateTimeType;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;

@Interceptor
public class CapabilityStatementCustomizer {

   @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
   public void customize(IBaseConformance theCapabilityStatement) {

      // Cast to the appropriate version
      CapabilityStatement cs = (CapabilityStatement) theCapabilityStatement;

      // Security disclaimer
      cs.getRestFirstRep()
            .getSecurity()
            .addService()
            .addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/restful-security-service")
            .setCode("OAuth")
            .setDisplay("OAuth");

      // Add instantiates
      cs.addInstantiates("http://hl7.org/fhir/CapabilityStatement/terminology-server");

      // Customize the CapabilityStatement as desired
      cs
            .getSoftware()
            .setName("OCL FHIR Server")
            .setVersion("1.0")
            .setReleaseDateElement(new DateTimeType("2025-07-10"));

   }

}
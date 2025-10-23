package com.gointerop.fhir.helper;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

@Component
public class ParametersHelper {
    public Parameters help(String message) {
        Parameters params = new Parameters();
        params.addParameter().setName("result").setValue(new BooleanType(false));
        params.addParameter().setName("message").setValue(new StringType(message));
        return params;
    }
}

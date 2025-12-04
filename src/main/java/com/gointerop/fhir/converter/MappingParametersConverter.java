package com.gointerop.fhir.converter;

import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

import com.gointerop.fhir.model.Mapping;
import com.gointerop.fhir.model.Source;

@Component
public class MappingParametersConverter {
    public Parameters toParameters(List<Mapping> mapping, List<Source> targetSources) {
        Parameters params = new Parameters();

        params.addParameter().setName("result").setValue(new BooleanType(true));

        for (int i = 0; i < mapping.size(); i++) {
            Mapping map = mapping.get(i);
            Source targetSource = targetSources.get(i);
            params.addParameter().setName("match").setPart(Arrays.asList(
                    new Parameters.ParametersParameterComponent().setName("equivalence")
                            .setValue(new CodeType(toFHIREquivalence(map.getMapType()))),
                    new Parameters.ParametersParameterComponent().setName("concept")
                            .setValue(new Coding().setCode(map.getToConceptCode()).setDisplay(
                                    map.getToConceptNameResolved())),
                    new Parameters.ParametersParameterComponent().setName("source")
                            .setValue(new StringType(targetSource.getCanonicalUrl()))
            ));
        }

        return params;
    }

    public String toFHIREquivalence(String oclEquivalence) {
        switch (oclEquivalence) {
            case "SAME-AS":
                return "equivalent";
            case "NARROWER-THAN":
                return "narrower";
            case "BROADER-THAN":
                return "broader";
            case "NOT-EQUIVALENT":
                return "disjoint";
            default:
                return "unmatched";
        }
    }
}

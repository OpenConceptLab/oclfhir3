package com.gointerop.fhir.helper;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.stereotype.Component;

@Component
public class OperationOutcomeHelper {
    public OperationOutcome help(OperationOutcome.IssueSeverity severity, OperationOutcome.IssueType issueType, String message) {
        OperationOutcome outcome = new OperationOutcome();

            // Add issue
            OperationOutcome.OperationOutcomeIssueComponent issue = outcome.addIssue();
            issue.setSeverity(severity);
            issue.setCode(issueType);
            issue.getDetails()
                    .setText(message);

            // Optional: add XHTML div (not always required, used for narrative rendering)
            outcome.getText().setStatus(org.hl7.fhir.r4.model.Narrative.NarrativeStatus.GENERATED);
            outcome.getText().setDivAsString(
                    "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p>" + message + "</p></div>");

            // Throw it with status code 422 (Unprocessable Entity)
            return outcome;
    }
}

package dev.vepo.issues.project;

import dev.vepo.issues.Given;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.restassured.http.Header;

public record ProjectTestFixtures(
                                  WorkflowResponse workflow,
                                  Header userAuthenticatedHeader,
                                  Header pmAuthenticatedHeader) {

    public static ProjectTestFixtures create() {
        return new ProjectTestFixtures(
                                       Given.simpleWorkflow(),
                                       Given.authenticatedUser(),
                                       Given.authenticatedProjectManager());
    }
}

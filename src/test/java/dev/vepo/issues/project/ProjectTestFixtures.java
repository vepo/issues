package dev.vepo.issues.project;

import java.util.UUID;

import dev.vepo.issues.Given;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.restassured.http.Header;

public record ProjectTestFixtures(
                                  WorkflowResponse workflow,
                                  Header userAuthenticatedHeader,
                                  Header pmAuthenticatedHeader,
                                  Category category) {

    public static ProjectTestFixtures create() {
        var category = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                                    .save(new Category("Bug" + UUID.randomUUID(), "red")));
        return new ProjectTestFixtures(
                                       Given.simpleWorkflow(),
                                       Given.authenticatedUser(),
                                       Given.authenticatedProjectManager(),
                                       category);
    }
}

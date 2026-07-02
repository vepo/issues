package dev.vepo.issues.ticket;

import static io.restassured.RestAssured.given;

import java.util.List;
import java.util.UUID;

import dev.vepo.issues.Given;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.workflow.StatusResponse;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

public record TicketTestFixtures(
                                 ProjectResponse project,
                                 Header userAuthenticatedHeader,
                                 Header pmAuthenticatedHeader,
                                 TicketResponse ticket,
                                 List<StatusResponse> allStatuses,
                                 Category bug,
                                 Category feature) {

    public static TicketTestFixtures create() {
        var bug = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                               .save(new Category("Bug" + UUID.randomUUID(), "red")));
        var feature = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                                   .save(new Category("Feature" + UUID.randomUUID(), "green")));
        var project = Given.simpleProject();
        var pmAuthenticatedHeader = Given.authenticatedProjectManager();
        var ticket = given().when()
                            .contentType(ContentType.JSON)
                            .header(pmAuthenticatedHeader)
                            .body("""
                                  {
                                      "title": "Test Ticket %s",
                                      "description": "This is a test ticket.",
                                      "projectId": %d,
                                      "categoryId": %d
                                  }""".formatted(UUID.randomUUID(), project.id(), bug.getId()))
                            .post("/api/tickets")
                            .then()
                            .statusCode(201)
                            .extract()
                            .as(TicketResponse.class);
        var userAuthenticatedHeader = Given.authenticatedUser();
        var allStatuses = Given.allStatuses();
        return new TicketTestFixtures(project, userAuthenticatedHeader, pmAuthenticatedHeader, ticket, allStatuses, bug, feature);
    }
}

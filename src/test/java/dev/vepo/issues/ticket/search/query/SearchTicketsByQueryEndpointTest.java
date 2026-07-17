package dev.vepo.issues.ticket.search.query;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class SearchTicketsByQueryEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should search tickets using query language")
    void shouldSearchTicketsByQuery() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "query": "title ~ \\"%s\\""
                     }""".formatted(fixtures.ticket().title()))
               .when()
               .post("/api/tickets/search/query")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].title", equalTo(fixtures.ticket().title()));
    }

    @Test
    @DisplayName("Should return bad request for invalid query")
    void shouldRejectInvalidQuery() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "query": "project ="
                     }""")
               .when()
               .post("/api/tickets/search/query")
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should return only tickets from projects readable by the authenticated caller")
    void shouldReturnOnlyTicketsFromReadableProjectsInAdvancedQuery() {
        var queryToken = "visibility-%s".formatted(UUID.randomUUID());
        var internalTicket = createTicket(createProject("INTERNAL").id(), queryToken);
        var publicTicket = createTicket(createProject("PUBLIC").id(), queryToken);
        var memberProject = createProject("PRIVATE");
        Given.addProjectMember(memberProject.id(), "user@issues.vepo.dev");
        var memberTicket = createTicket(memberProject.id(), queryToken);
        createTicket(createProject("PRIVATE").id(), queryToken);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "query": "title ~ \\"%s\\""
                     }""".formatted(queryToken))
               .when()
               .post("/api/tickets/search/query")
               .then()
               .statusCode(200)
               .body("$", hasSize(3))
               .body("identifier",
                     containsInAnyOrder(internalTicket.identifier(), publicTicket.identifier(), memberTicket.identifier()));
    }

    private ProjectResponse createProject(String securityLevel) {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "name": "Query visibility %s",
                                "description": "Query visibility regression project.",
                                "prefix": "Q%s",
                                "workflowId": %d,
                                "securityLevel": "%s"
                            }""".formatted(suffix,
                                           suffix.substring(0, 3).toUpperCase(),
                                           Given.simpleWorkflow().id(),
                                           securityLevel))
                      .post("/api/projects")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(ProjectResponse.class);
    }

    private TicketResponse createTicket(long projectId, String queryToken) {
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "title": "%s",
                                "description": "Query visibility regression ticket.",
                                "projectId": %d,
                                "categoryId": %d
                            }""".formatted(queryToken, projectId, fixtures.bug().getId()))
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }
}

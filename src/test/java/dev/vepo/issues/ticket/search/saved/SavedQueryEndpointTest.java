package dev.vepo.issues.ticket.search.saved;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

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
class SavedQueryEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should create, list, and delete saved query for owner")
    void shouldManageSavedQueryAsOwner() {
        var created = given().header(fixtures.pmAuthenticatedHeader())
                             .contentType(ContentType.JSON)
                             .accept(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "My open tickets",
                                       "query": "title ~ \\"Test\\"",
                                       "showAtHome": true
                                   }""")
                             .when()
                             .post("/api/saved-queries")
                             .then()
                             .statusCode(201)
                             .body("name", equalTo("My open tickets"))
                             .body("showAtHome", equalTo(true))
                             .extract()
                             .as(SavedQueryResponse.class);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/saved-queries")
               .then()
               .statusCode(200)
               .body("$.size()", org.hamcrest.Matchers.greaterThan(0));

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/saved-queries/by-slug/" + created.slug())
               .then()
               .statusCode(200)
               .body("savedQuery.slug", equalTo(created.slug()))
               .body("tickets", notNullValue());

        given().header(fixtures.pmAuthenticatedHeader())
               .when()
               .delete("/api/saved-queries/" + created.id())
               .then()
               .statusCode(204);
    }

    @Test
    @DisplayName("Should clone another user saved query")
    void shouldCloneSavedQueryFromOtherUser() {
        var created = given().header(fixtures.pmAuthenticatedHeader())
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "Shared query",
                                       "query": "title ~ \\"Test\\"",
                                       "showAtHome": false
                                   }""")
                             .when()
                             .post("/api/saved-queries")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(SavedQueryResponse.class);

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .post("/api/saved-queries/" + created.id() + "/clone")
               .then()
               .statusCode(201)
               .body("name", equalTo("Shared query (cópia)"))
               .body("ownerId", notNullValue());
    }

    @Test
    @DisplayName("Should execute saved query with the authenticated caller's readable projects")
    void shouldReturnOnlyTicketsFromReadableProjectsInSavedQuery() {
        var queryToken = "saved-visibility-%s".formatted(UUID.randomUUID());
        var internalTicket = createTicket(createProject("INTERNAL").id(), queryToken);
        var publicTicket = createTicket(createProject("PUBLIC").id(), queryToken);
        var memberProject = createProject("PRIVATE");
        Given.addProjectMember(memberProject.id(), "user@issues.vepo.dev");
        var memberTicket = createTicket(memberProject.id(), queryToken);
        createTicket(createProject("PRIVATE").id(), queryToken);
        var savedQuery = given().header(fixtures.pmAuthenticatedHeader())
                                .contentType(ContentType.JSON)
                                .body("""
                                      {
                                          "name": "Visibility regression %s",
                                          "query": "title ~ \\"%s\\"",
                                          "showAtHome": false
                                      }""".formatted(UUID.randomUUID(), queryToken))
                                .post("/api/saved-queries")
                                .then()
                                .statusCode(201)
                                .extract()
                                .as(SavedQueryResponse.class);

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/saved-queries/by-slug/" + savedQuery.slug())
               .then()
               .statusCode(200)
               .body("tickets", hasSize(3))
               .body("tickets.identifier",
                     containsInAnyOrder(internalTicket.identifier(), publicTicket.identifier(), memberTicket.identifier()));
    }

    private ProjectResponse createProject(String securityLevel) {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "name": "Saved query visibility %s",
                                "description": "Saved query visibility regression project.",
                                "prefix": "S%s",
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
                                "description": "Saved query visibility regression ticket.",
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

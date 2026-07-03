package dev.vepo.issues.home;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class HomeEndpointTest {

    private ProjectResponse project;
    private Header userHeader;
    private Header pmHeader;
    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        project = Given.simpleProject();
        userHeader = Given.authenticatedUser();
        pmHeader = Given.authenticatedProjectManager();
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Current tickets lists open tickets in member projects")
    void shouldListCurrentTicketsInMemberProjects() {
        given().header(userHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/home/tickets/current")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(1))
               .body("find { it.id == %d }.title".formatted(fixtures.ticket().id()), equalTo(fixtures.ticket().title()));
    }

    @Test
    @DisplayName("Assigned tickets lists open tickets where user is assignee")
    void shouldListAssignedTicketsForCurrentUser() {
        var userId = Given.userIdByEmail("user@issues.vepo.dev");
        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "assigneeId": %d
                     }""".formatted(userId))
               .patch("/api/tickets/" + fixtures.ticket().id() + "/assignee")
               .then()
               .statusCode(200);

        given().header(userHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/home/tickets/assigned")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(1))
               .body("find { it.id == %d }.identifier".formatted(fixtures.ticket().id()), equalTo(fixtures.ticket().identifier()));
    }

    @Test
    @DisplayName("Home activity includes comments on scoped tickets")
    void shouldListHomeActivityWithComments() {
        given().header(userHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "content": "Home activity comment"
                     }""")
               .post("/api/tickets/" + fixtures.ticket().id() + "/comments")
               .then()
               .statusCode(201);

        given().header(userHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/home/activity")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(1))
               .body("find { it.type == 'COMMENT' && it.ticketId == %d }.summary".formatted(fixtures.ticket().id()),
                     equalTo("Home activity comment"));
    }

    @Test
    @DisplayName("Home endpoints require authentication")
    void shouldRejectUnauthenticatedHomeRequests() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/home/tickets/current")
               .then()
               .statusCode(401);
    }
}

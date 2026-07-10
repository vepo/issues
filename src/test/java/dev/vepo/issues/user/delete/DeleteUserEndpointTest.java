package dev.vepo.issues.user.delete;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class DeleteUserEndpointTest {

    private Header authenticatedAdmin;
    private Header authenticatedUser;

    @BeforeEach
    void setUp() {
        authenticatedAdmin = Given.authenticatedAdmin();
        authenticatedUser = Given.authenticatedUser();
    }

    @Test
    void shouldSoftDeleteUserWhenNoBlockingAssignments() {
        var user = Given.randomUser();

        given().header(authenticatedAdmin)
               .when()
               .delete("/api/users/" + user.getId())
               .then()
               .statusCode(204);

        given().header(authenticatedAdmin)
               .accept(MediaType.APPLICATION_JSON)
               .when()
               .get("/api/users/" + user.getId())
               .then()
               .statusCode(404);
    }

    @Test
    void shouldRejectDeleteWhenAssigneeOnInProgressTicket() {
        var assignee = Given.randomUser();
        var fixtures = TicketTestFixtures.create();
        Given.addProjectMember(fixtures.project().id(), assignee.getId());

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "assigneeId": %d
                     }""".formatted(assignee.getId()))
               .when()
               .patch("/api/tickets/" + fixtures.ticket().id() + "/assignee")
               .then()
               .statusCode(200);

        var inProgress = Given.status("In Progress");
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "to": %d
                     }""".formatted(inProgress.getId()))
               .when()
               .post("/api/tickets/" + fixtures.ticket().id() + "/move")
               .then()
               .statusCode(200);

        given().header(authenticatedAdmin)
               .accept(MediaType.APPLICATION_JSON)
               .when()
               .delete("/api/users/" + assignee.getId())
               .then()
               .statusCode(400)
               .body("message", containsString("cannot be deleted"));
    }

    @Test
    void shouldAllowDeleteWhenAssigneeOnlyOnStartStatus() {
        var assignee = Given.randomUser();
        var fixtures = TicketTestFixtures.create();
        Given.addProjectMember(fixtures.project().id(), assignee.getId());

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "assigneeId": %d
                     }""".formatted(assignee.getId()))
               .when()
               .patch("/api/tickets/" + fixtures.ticket().id() + "/assignee")
               .then()
               .statusCode(200);

        given().header(authenticatedAdmin)
               .when()
               .delete("/api/users/" + assignee.getId())
               .then()
               .statusCode(204);
    }

    @Test
    void regularUserShouldNotDeleteUser() {
        var user = Given.randomUser();

        given().header(authenticatedUser)
               .when()
               .delete("/api/users/" + user.getId())
               .then()
               .statusCode(403);
    }
}

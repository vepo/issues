package dev.vepo.issues.ticket.assign;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class UpdateAssigneeEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be possible to update ticket assignee")
    void updateAssigneeTest() {
        var newAssignee = Given.user("user2@issues.vepo.dev");
        Given.addProjectMember(fixtures.project().id(), newAssignee.getId());
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "assigneeId": %d
                     }""".formatted(newAssignee.getId()))
               .patch("/api/tickets/" + fixtures.ticket().id() + "/assignee")
               .then()
               .statusCode(200)
               .body("assignee", equalTo(newAssignee.getId().intValue()));
    }

    @Test
    @DisplayName("It should reject assignee who is not a project member")
    void shouldRejectAssigneeWhoIsNotProjectMember() {
        var outsider = Given.user("outsider@issues.vepo.dev");
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "assigneeId": %d
                     }""".formatted(outsider.getId()))
               .patch("/api/tickets/" + fixtures.ticket().id() + "/assignee")
               .then()
               .statusCode(400)
               .body("message", equalTo("Assignee must be a member of the project"));
    }

    @Test
    void shouldNotUpdateAssigneeWithInvalidUserIdTest() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "assigneeId": 9999
                     }""")
               .patch("/api/tickets/" + fixtures.ticket().id() + "/assignee")
               .then()
               .statusCode(404)
               .body("message", equalTo("User does not found! userId=9999"));
    }
}

package dev.vepo.issues.project.members;

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
class ProjectMemberEndpointTest {

    private ProjectResponse project;
    private Header pmHeader;
    private Header userHeader;

    @BeforeEach
    void setup() {
        project = Given.simpleProject();
        pmHeader = Given.authenticatedProjectManager();
        userHeader = Given.authenticatedUser();
    }

    @Test
    @DisplayName("Project owner can list and add members")
    void shouldListAndAddProjectMembers() {
        var newUser = Given.user("member-candidate@issues.vepo.dev");

        given().header(pmHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/members")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(1));

        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "userId": %d
                     }""".formatted(newUser.getId()))
               .post("/api/projects/" + project.id() + "/members")
               .then()
               .statusCode(201)
               .body("email", equalTo("member-candidate@issues.vepo.dev"));

        given().header(pmHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/members")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.email".formatted(newUser.getId()), equalTo("member-candidate@issues.vepo.dev"));
    }

    @Test
    @DisplayName("Cannot remove member with open assigned tickets")
    void shouldBlockMemberRemovalWhenOpenTicketsAssigned() {
        var assignee = Given.user("blocked-member@issues.vepo.dev");
        Given.addProjectMember(project.id(), assignee.getId());

        var fixtures = TicketTestFixtures.create();
        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "assigneeId": %d
                     }""".formatted(assignee.getId()))
               .patch("/api/tickets/" + fixtures.ticket().id() + "/assignee")
               .then()
               .statusCode(200);

        given().header(pmHeader)
               .accept(ContentType.JSON)
               .when()
               .delete("/api/projects/" + project.id() + "/members/" + assignee.getId())
               .then()
               .statusCode(400);

        given().header(pmHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/members/" + assignee.getId() + "/open-tickets")
               .then()
               .statusCode(200)
               .body("$", hasSize(1));
    }

    @Test
    @DisplayName("Regular user cannot manage project members")
    void shouldForbidMemberManagementForNonOwner() {
        var candidate = Given.user("non-owner-pm@issues.vepo.dev");
        given().header(userHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "userId": %d
                     }""".formatted(candidate.getId()))
               .post("/api/projects/" + project.id() + "/members")
               .then()
               .statusCode(403);
    }
}

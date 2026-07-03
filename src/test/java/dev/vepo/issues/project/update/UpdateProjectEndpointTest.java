package dev.vepo.issues.project.update;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import dev.vepo.issues.Given;
import dev.vepo.issues.auth.PasswordEncoder;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.project.ProjectTestFixtures;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class UpdateProjectEndpointTest {

    private WorkflowResponse workflow;
    private Header userAuthenticatedHeader;
    private Header pmAuthenticatedHeader;
    private dev.vepo.issues.categories.Category category;

    @BeforeEach
    void setup() {
        var fixtures = ProjectTestFixtures.create();
        this.workflow = fixtures.workflow();
        this.userAuthenticatedHeader = fixtures.userAuthenticatedHeader();
        this.pmAuthenticatedHeader = fixtures.pmAuthenticatedHeader();
        this.category = fixtures.category();
    }

    @Test
    @DisplayName("Only project managers should be allowed to update projects")
    void onlyProjectManagerShouldBeAllowedToUpdateProjectsTest() {
        // First create a project
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Original Project",
                                              "description": "Original description.",
                                              "prefix": "ORG",
                                              "workflowId": %d
                                          }""".formatted(workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(ProjectResponse.class);

        // Test that regular user cannot update
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Updated Project",
                         "description": "Updated description.",
                         "prefix": "UPD",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects/" + createdProject.id())
               .then()
               .statusCode(403);

        // Test that PM can update
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Updated Project",
                         "description": "Updated description.",
                         "prefix": "UPD",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects/" + createdProject.id())
               .then()
               .statusCode(201)
               .body("id", is((int) createdProject.id()))
               .body("name", is("Updated Project"))
               .body("description", is("Updated description."))
               .body("prefix", is("UPD"))
               .body("workflow.id", is((int) workflow.id()));
    }

    @Test
    @DisplayName("Updating non-existent project should return 404")
    void updateNonExistentProjectShouldReturn404Test() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Non-existent Project",
                         "description": "This project doesn't exist.",
                         "prefix": "NEX",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects/9999")
               .then()
               .statusCode(404)
               .body("message", is("Project with ID 9999 does not exist"));
    }

    @Test
    @DisplayName("Project update can enable ticket template")
    void updateCanEnableTicketTemplateTest() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 6);
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Enable Template %s",
                                              "description": "Original.",
                                              "prefix": "ET%s",
                                              "workflowId": %d
                                          }""".formatted(suffix, suffix.substring(0, 2), workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(ProjectResponse.class);

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Enable Template %s",
                         "description": "Original.",
                         "prefix": "ET%s",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": true,
                             "title": "Updated template title",
                             "description": "Updated template description for tickets.",
                             "categoryId": %d,
                             "priority": "HIGH"
                         }
                     }""".formatted(suffix, suffix.substring(0, 2), workflow.id(), category.getId()))
               .post("/api/projects/" + createdProject.id())
               .then()
               .statusCode(201)
               .body("ticketTemplate.enabled", is(true))
               .body("ticketTemplate.title", is("Updated template title"))
               .body("ticketTemplate.priority", is("HIGH"));
    }

    @Test
    @DisplayName("Project update can disable ticket template")
    void updateCanDisableTicketTemplateTest() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 6);
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Disable Template %s",
                                              "description": "With template.",
                                              "prefix": "DT%s",
                                              "workflowId": %d,
                                              "ticketTemplate": {
                                                  "enabled": true,
                                                  "title": "Template title here",
                                                  "description": "Template description for tickets.",
                                                  "categoryId": %d,
                                                  "priority": "LOW"
                                              }
                                          }""".formatted(suffix, suffix.substring(0, 2), workflow.id(), category.getId()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(ProjectResponse.class);

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Disable Template %s",
                         "description": "With template.",
                         "prefix": "DT%s",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": false
                         }
                     }""".formatted(suffix, suffix.substring(0, 2), workflow.id()))
               .post("/api/projects/" + createdProject.id())
               .then()
               .statusCode(201)
               .body("ticketTemplate.enabled", is(false))
               .body("ticketTemplate.title", equalTo(null))
               .body("ticketTemplate.categoryId", equalTo(null));
    }

    @Test
    @DisplayName("Project owner can transfer ownership to another project manager")
    void shouldTransferProjectOwnership() {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Transfer Project %s",
                                              "description": "Ownership transfer test.",
                                              "prefix": "TR%s",
                                              "workflowId": %d
                                          }""".formatted(suffix, suffix.substring(0, 2), workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(ProjectResponse.class);

        var newOwnerId = Given.transaction(() -> {
            var email = "new-owner-" + suffix + "@issues.vepo.dev";
            return Given.inject(UserRepository.class)
                        .save(new User("pm-" + suffix,
                                       "New Owner",
                                       email,
                                       Given.inject(PasswordEncoder.class).hashPassword("password"),
                                       Set.of(Role.PROJECT_MANAGER)))
                        .getId();
        });

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Transfer Project %s",
                         "description": "Ownership transfer test.",
                         "prefix": "TR%s",
                         "workflowId": %d,
                         "ownerId": %d
                     }""".formatted(suffix, suffix.substring(0, 2), workflow.id(), newOwnerId))
               .post("/api/projects/" + createdProject.id())
               .then()
               .statusCode(201)
               .body("owner.id", is(newOwnerId.intValue()));
    }
}

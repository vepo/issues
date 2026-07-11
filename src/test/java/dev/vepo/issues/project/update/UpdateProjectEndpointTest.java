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
import dev.vepo.issues.ticket.TicketResponse;
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

    @Test
    @DisplayName("Should reject prefix change when project has tickets")
    void shouldRejectPrefixChangeWhenProjectHasTickets() {
        var project = createProject("Lock Prefix", "LP");
        createTicket(project.id(), "Ticket locking prefix");

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body(updateBody(project.name(), "NEW", project.description()))
               .post("/api/projects/" + project.id())
               .then()
               .statusCode(400)
               .body("message", is("Project prefix cannot be changed while the project has tickets"));
    }

    @Test
    @DisplayName("Should reject prefix change when only a soft-deleted ticket exists")
    void shouldRejectPrefixChangeWhenOnlySoftDeletedTicketExists() {
        var project = createProject("Soft Delete Lock", "SD");
        var ticket = createTicket(project.id(), "Soft-deleted ticket locking prefix");

        given().header(pmAuthenticatedHeader)
               .when()
               .delete("/api/tickets/" + ticket.id())
               .then()
               .statusCode(204);

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body(updateBody(project.name(), "NEW", project.description()))
               .post("/api/projects/" + project.id())
               .then()
               .statusCode(400)
               .body("message", is("Project prefix cannot be changed while the project has tickets"));
    }

    @Test
    @DisplayName("Should allow update with same prefix when project has tickets")
    void shouldAllowUpdateWithSamePrefixWhenProjectHasTickets() {
        var project = createProject("Same Prefix", "SP");
        createTicket(project.id(), "Ticket keeping same prefix");

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body(updateBody("Same Prefix Renamed", project.prefix(), "Updated description."))
               .post("/api/projects/" + project.id())
               .then()
               .statusCode(201)
               .body("name", is("Same Prefix Renamed"))
               .body("prefix", is(project.prefix()))
               .body("prefixLocked", is(true));
    }

    @Test
    @DisplayName("Should allow prefix change when project has no tickets")
    void shouldAllowPrefixChangeWhenProjectHasNoTickets() {
        var project = createProject("Empty Prefix", "EP");
        var newPrefix = "NX" + UUID.randomUUID().toString().substring(0, 2).toUpperCase();

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body(updateBody(project.name(), newPrefix, project.description()))
               .post("/api/projects/" + project.id())
               .then()
               .statusCode(201)
               .body("prefix", is(newPrefix))
               .body("prefixLocked", is(false));
    }

    @Test
    @DisplayName("Should reject workflow change when project custom field key collides with new workflow")
    void shouldRejectWorkflowChangeOnCustomFieldKeyCollision() {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var otherWorkflow = given().header(pmAuthenticatedHeader)
                                   .contentType(ContentType.JSON)
                                   .body("""
                                         {
                                             "name": "Other WF %s",
                                             "statuses": ["TODO", "Done"],
                                             "start": "TODO",
                                             "transitions": [{"from": "TODO", "to": "Done"}],
                                             "finishStatuses": [{"status": "Done", "outcome": "DONE"}]
                                         }
                                         """.formatted(suffix))
                                   .post("/api/workflows")
                                   .then()
                                   .statusCode(201)
                                   .extract()
                                   .as(WorkflowResponse.class);
        var created = given().header(pmAuthenticatedHeader)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "CF Collision %s",
                                       "description": "Workflow change collision test.",
                                       "prefix": "CC%s",
                                       "workflowId": %d
                                   }""".formatted(suffix, suffix.substring(0, 4).toUpperCase(), workflow.id()))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(ProjectResponse.class);

        var key = "shared_" + suffix;
        given().header(pmAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Project Shared",
                       "type": "STRING",
                       "required": false,
                       "stringMaxLength": 32
                     }
                     """.formatted(key))
               .post("/api/projects/%d/custom-fields".formatted(created.id()))
               .then()
               .statusCode(201);

        given().header(pmAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Workflow Shared",
                       "type": "STRING",
                       "required": false,
                       "stringMaxLength": 32
                     }
                     """.formatted(key))
               .post("/api/workflows/%d/custom-fields".formatted(otherWorkflow.id()))
               .then()
               .statusCode(201);

        given().header(pmAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "%s",
                         "description": "Workflow change collision test.",
                         "prefix": "%s",
                         "workflowId": %d
                     }""".formatted(created.name(), created.prefix(), otherWorkflow.id()))
               .post("/api/projects/" + created.id())
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should persist custom field template defaults and drop them when out of scope after workflow change")
    void shouldPersistAndDropStaleCustomFieldTemplateDefaults() {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var otherWorkflow = given().header(pmAuthenticatedHeader)
                                   .contentType(ContentType.JSON)
                                   .body("""
                                         {
                                             "name": "Template WF %s",
                                             "statuses": ["TODO", "Done"],
                                             "start": "TODO",
                                             "transitions": [{"from": "TODO", "to": "Done"}],
                                             "finishStatuses": [{"status": "Done", "outcome": "DONE"}]
                                         }
                                         """.formatted(suffix))
                                   .post("/api/workflows")
                                   .then()
                                   .statusCode(201)
                                   .extract()
                                   .as(WorkflowResponse.class);
        var created = given().header(pmAuthenticatedHeader)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "CF Template %s",
                                       "description": "Template custom defaults test.",
                                       "prefix": "CT%s",
                                       "workflowId": %d
                                   }""".formatted(suffix, suffix.substring(0, 4).toUpperCase(), workflow.id()))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(ProjectResponse.class);

        var key = "sprint_" + suffix;
        given().header(pmAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Sprint",
                       "type": "INTEGER",
                       "required": false,
                       "integerMin": 1,
                       "integerMax": 99
                     }
                     """.formatted(key))
               .post("/api/projects/%d/custom-fields".formatted(created.id()))
               .then()
               .statusCode(201);

        given().header(pmAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "%s",
                         "description": "Template custom defaults test.",
                         "prefix": "%s",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": true,
                             "title": "Default title here",
                             "customFieldDefaults": [{"key": "%s", "value": 3}]
                         }
                     }""".formatted(created.name(), created.prefix(), workflow.id(), key))
               .post("/api/projects/" + created.id())
               .then()
               .statusCode(201)
               .body("ticketTemplate.enabled", equalTo(true))
               .body("ticketTemplate.customFieldDefaults.key", org.hamcrest.Matchers.hasItem(key));

        given().header(pmAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "%s",
                         "description": "Template custom defaults test.",
                         "prefix": "%s",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": true,
                             "title": "Default title here"
                         }
                     }""".formatted(created.name(), created.prefix(), otherWorkflow.id()))
               .post("/api/projects/" + created.id())
               .then()
               .statusCode(201)
               .body("workflow.id", equalTo((int) otherWorkflow.id()))
               .body("ticketTemplate.customFieldDefaults.size()", equalTo(0));
    }

    private ProjectResponse createProject(String namePrefix, String prefixSeed) {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        return given().header(pmAuthenticatedHeader)
                      .accept(ContentType.JSON)
                      .when()
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "name": "%s %s",
                                "description": "Prefix lock test project.",
                                "prefix": "%s%s",
                                "workflowId": %d
                            }""".formatted(namePrefix, suffix, prefixSeed, suffix.substring(0, 2), workflow.id()))
                      .post("/api/projects")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(ProjectResponse.class);
    }

    private TicketResponse createTicket(long projectId, String title) {
        return given().header(pmAuthenticatedHeader)
                      .accept(ContentType.JSON)
                      .when()
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "title": "%s",
                                "description": "Ticket used to lock project prefix.",
                                "projectId": %d,
                                "categoryId": %d
                            }""".formatted(title, projectId, category.getId()))
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }

    private String updateBody(String name, String prefix, String description) {
        return """
               {
                   "name": "%s",
                   "description": "%s",
                   "prefix": "%s",
                   "workflowId": %d
               }""".formatted(name, description, prefix, workflow.id());
    }
}

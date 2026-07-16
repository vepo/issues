package dev.vepo.issues.ticket.cloneprefill;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class CloneTicketPrefillEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    void shouldPrefillIntrinsicFieldsFromReadableCrossProjectSourceWithoutPlanningOrCollaborationFields() {
        var sourceProject = createProject("Clone source", "INTERNAL");
        var targetProject = createProject("Clone target", "PRIVATE");
        Given.addProjectMember(targetProject.id(), "user@issues.vepo.dev");
        var source = createTicket(sourceProject.id(),
                                  """
                                  "title": "Cross-project clone source",
                                  "description": "Values copied for review.",
                                  "categoryId": %d,
                                  "priority": "HIGH",
                                  "ticketType": "STORY",
                                  "dueDate": "2026-12-01",
                                  "storyPoints": 13
                                  """.formatted(fixtures.bug().getId()));

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .queryParam("targetProjectId", targetProject.id())
               .when()
               .get("/api/tickets/{sourceId}/clone-prefill", source.id())
               .then()
               .statusCode(200)
               .body("sourceIdentifier", equalTo(source.identifier()))
               .body("targetProjectId", is((int) targetProject.id()))
               .body("title", equalTo("Cross-project clone source"))
               .body("description", equalTo("Values copied for review."))
               .body("categoryId", is(fixtures.bug().getId().intValue()))
               .body("priority", equalTo("HIGH"))
               .body("ticketType", equalTo("STORY"))
               .body("$", not(hasKey("assignee")))
               .body("$", not(hasKey("phase")))
               .body("$", not(hasKey("versions")))
               .body("$", not(hasKey("observedVersionId")))
               .body("$", not(hasKey("targetVersionId")))
               .body("$", not(hasKey("dueDate")))
               .body("$", not(hasKey("storyPoints")))
               .body("$", not(hasKey("comments")))
               .body("$", not(hasKey("history")))
               .body("$", not(hasKey("subscribers")))
               .body("$", not(hasKey("attachments")))
               .body("$", not(hasKey("links")))
               .body("$", not(hasKey("commits")));
    }

    @Test
    void shouldCopyOnlyCustomFieldsWithStableKeyExactTypeAndValidEnumValue() {
        var sourceProject = createProject("Custom source", "INTERNAL");
        var targetProject = createProject("Custom target", "PRIVATE");
        Given.addProjectMember(targetProject.id(), "user@issues.vepo.dev");
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var matchingKey = "match_" + suffix;
        var typeMismatchKey = "type_" + suffix;
        var matchingEnumKey = "enum_" + suffix;
        var invalidEnumKey = "invalid_" + suffix;

        createStringField(sourceProject.id(), matchingKey);
        createStringField(targetProject.id(), matchingKey);
        createStringField(sourceProject.id(), typeMismatchKey);
        createIntegerField(targetProject.id(), typeMismatchKey);
        createEnumField(sourceProject.id(), matchingEnumKey, "ready", "blocked");
        createEnumField(targetProject.id(), matchingEnumKey, "ready", "blocked");
        createEnumField(sourceProject.id(), invalidEnumKey, "legacy", "current");
        createEnumField(targetProject.id(), invalidEnumKey, "current", "future");

        var source = createTicket(sourceProject.id(),
                                  """
                                  "title": "Custom field clone source",
                                  "description": "Custom values need target mapping.",
                                  "categoryId": %d,
                                  "customFields": [
                                    {"key": "%s", "value": "copied"},
                                    {"key": "%s", "value": "wrong target type"},
                                    {"key": "%s", "value": "ready"},
                                    {"key": "%s", "value": "legacy"}
                                  ]
                                  """.formatted(fixtures.bug().getId(),
                                                matchingKey,
                                                typeMismatchKey,
                                                matchingEnumKey,
                                                invalidEnumKey));

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .queryParam("targetProjectId", targetProject.id())
               .when()
               .get("/api/tickets/{sourceId}/clone-prefill", source.id())
               .then()
               .statusCode(200)
               .body("customFields.find { it.key == '%s' }.value".formatted(matchingKey), equalTo("copied"))
               .body("customFields.find { it.key == '%s' }.value".formatted(matchingEnumKey), equalTo("ready"))
               .body("customFields.find { it.key == '%s' }".formatted(typeMismatchKey), nullValue())
               .body("customFields.find { it.key == '%s' }".formatted(invalidEnumKey), nullValue())
               .body("warnings.size()", greaterThanOrEqualTo(2));
    }

    @Test
    void shouldOmitStringCustomFieldWhenSourceValueExceedsTargetMaximumLength() {
        var sourceProject = createProject("String constraint source", "INTERNAL");
        var targetProject = createProject("String constraint target", "PRIVATE");
        Given.addProjectMember(targetProject.id(), "user@issues.vepo.dev");
        var fieldKey = "string_constraint_" + UUID.randomUUID().toString().substring(0, 8);

        createStringField(sourceProject.id(), fieldKey);
        createCustomField(targetProject.id(),
                          """
                          {
                            "key": "%s",
                            "label": "%s",
                            "type": "STRING",
                            "required": false,
                            "stringMaxLength": 5
                          }
                          """.formatted(fieldKey, fieldKey));
        var source = createTicket(sourceProject.id(),
                                  """
                                  "title": "String constraint clone source",
                                  "description": "Target constraints must apply.",
                                  "categoryId": %d,
                                  "customFields": [
                                    {"key": "%s", "value": "too long"}
                                  ]
                                  """.formatted(fixtures.bug().getId(), fieldKey));

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .queryParam("targetProjectId", targetProject.id())
               .when()
               .get("/api/tickets/{sourceId}/clone-prefill", source.id())
               .then()
               .statusCode(200)
               .body("customFields.find { it.key == '%s' }".formatted(fieldKey), nullValue())
               .body("warnings", hasItem(containsString(fieldKey)));
    }

    @Test
    void shouldOmitIntegerCustomFieldWhenSourceValueIsOutsideTargetBounds() {
        var sourceProject = createProject("Integer constraint source", "INTERNAL");
        var targetProject = createProject("Integer constraint target", "PRIVATE");
        Given.addProjectMember(targetProject.id(), "user@issues.vepo.dev");
        var fieldKey = "integer_constraint_" + UUID.randomUUID().toString().substring(0, 8);

        createIntegerField(sourceProject.id(), fieldKey);
        createCustomField(targetProject.id(),
                          """
                          {
                            "key": "%s",
                            "label": "%s",
                            "type": "INTEGER",
                            "required": false,
                            "integerMin": 10,
                            "integerMax": 20
                          }
                          """.formatted(fieldKey, fieldKey));
        var source = createTicket(sourceProject.id(),
                                  """
                                  "title": "Integer constraint clone source",
                                  "description": "Target constraints must apply.",
                                  "categoryId": %d,
                                  "customFields": [
                                    {"key": "%s", "value": 50}
                                  ]
                                  """.formatted(fixtures.bug().getId(), fieldKey));

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .queryParam("targetProjectId", targetProject.id())
               .when()
               .get("/api/tickets/{sourceId}/clone-prefill", source.id())
               .then()
               .statusCode(200)
               .body("customFields.find { it.key == '%s' }".formatted(fieldKey), nullValue())
               .body("warnings", hasItem(containsString(fieldKey)));
    }

    @Test
    void shouldRejectDeletedSourceTicket() {
        var sourceProject = createProject("Deleted source", "INTERNAL");
        var targetProject = createProject("Deleted target", "PRIVATE");
        Given.addProjectMember(targetProject.id(), "user@issues.vepo.dev");
        var source = createTicket(sourceProject.id(),
                                  """
                                  "title": "Deleted clone source",
                                  "description": "Deleted tickets cannot be cloned.",
                                  "categoryId": %d
                                  """.formatted(fixtures.bug().getId()));
        given().header(fixtures.pmAuthenticatedHeader())
               .delete("/api/tickets/{id}", source.id())
               .then()
               .statusCode(204);

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .queryParam("targetProjectId", targetProject.id())
               .when()
               .get("/api/tickets/{sourceId}/clone-prefill", source.id())
               .then()
               .statusCode(404);
    }

    @Test
    void shouldRejectReadableTargetWhenUserCannotWriteIt() {
        var sourceProject = createProject("Readable source", "INTERNAL");
        var merelyReadableTarget = createProject("Readable target", "INTERNAL");
        var source = createTicket(sourceProject.id(),
                                  """
                                  "title": "Target access source",
                                  "description": "Target must be writable.",
                                  "categoryId": %d
                                  """.formatted(fixtures.bug().getId()));

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .queryParam("targetProjectId", merelyReadableTarget.id())
               .when()
               .get("/api/tickets/{sourceId}/clone-prefill", source.id())
               .then()
               .statusCode(403);
    }

    @Test
    void shouldRejectSourceTicketWhenProjectIsNotReadable() {
        var privateSourceProject = createProject("Private source", "PRIVATE");
        var targetProject = createProject("Writable target", "PRIVATE");
        Given.addProjectMember(targetProject.id(), "user@issues.vepo.dev");
        var source = createTicket(privateSourceProject.id(),
                                  """
                                  "title": "Unreadable clone source",
                                  "description": "Source project is private.",
                                  "categoryId": %d
                                  """.formatted(fixtures.bug().getId()));

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .queryParam("targetProjectId", targetProject.id())
               .when()
               .get("/api/tickets/{sourceId}/clone-prefill", source.id())
               .then()
               .statusCode(403);
    }

    private ProjectResponse createProject(String name, String securityLevel) {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                              "name": "%s %s",
                              "description": "Clone prefill endpoint test.",
                              "prefix": "C%s",
                              "workflowId": %d,
                              "securityLevel": "%s"
                            }
                            """.formatted(name,
                                          suffix,
                                          suffix.substring(0, 3).toUpperCase(),
                                          Given.simpleWorkflow().id(),
                                          securityLevel))
                      .post("/api/projects")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(ProjectResponse.class);
    }

    private TicketResponse createTicket(long projectId, String fields) {
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                              "projectId": %d,
                              %s
                            }
                            """.formatted(projectId, fields))
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }

    private void createStringField(long projectId, String key) {
        createCustomField(projectId,
                          """
                          {
                            "key": "%s",
                            "label": "%s",
                            "type": "STRING",
                            "required": false,
                            "stringMaxLength": 64
                          }
                          """.formatted(key, key));
    }

    private void createIntegerField(long projectId, String key) {
        createCustomField(projectId,
                          """
                          {
                            "key": "%s",
                            "label": "%s",
                            "type": "INTEGER",
                            "required": false,
                            "integerMin": 0,
                            "integerMax": 100
                          }
                          """.formatted(key, key));
    }

    private void createEnumField(long projectId, String key, String firstValue, String secondValue) {
        createCustomField(projectId,
                          """
                          {
                            "key": "%s",
                            "label": "%s",
                            "type": "ENUM",
                            "required": false,
                            "enumOptions": [
                              {"value": "%s", "label": "%s"},
                              {"value": "%s", "label": "%s"}
                            ]
                          }
                          """.formatted(key, key, firstValue, firstValue, secondValue, secondValue));
    }

    private void createCustomField(long projectId, String body) {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body(body)
               .post("/api/projects/{projectId}/custom-fields", projectId)
               .then()
               .statusCode(201);
    }
}

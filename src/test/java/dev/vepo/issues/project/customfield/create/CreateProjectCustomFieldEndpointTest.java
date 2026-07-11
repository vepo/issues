package dev.vepo.issues.project.customfield.create;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class CreateProjectCustomFieldEndpointTest {

    private ProjectResponse project;
    private Header pmHeader;
    private Header adminHeader;
    private String suffix;
    private Category category;

    @BeforeEach
    void setup() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        pmHeader = Given.authenticatedProjectManager();
        adminHeader = Given.authenticatedAdmin();
        WorkflowResponse workflow = Given.simpleWorkflow();
        project = given().header(pmHeader)
                         .contentType(ContentType.JSON)
                         .body("""
                               {
                                 "name": "CF Project %s",
                                 "description": "Custom fields test project.",
                                 "prefix": "CF%s",
                                 "workflowId": %d
                               }
                               """.formatted(suffix, suffix.substring(0, 4).toUpperCase(), workflow.id()))
                         .post("/api/projects")
                         .then()
                         .statusCode(201)
                         .extract()
                         .as(ProjectResponse.class);
        category = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                                .save(new Category("Bug" + suffix, "red")));
    }

    @Test
    @DisplayName("Should create a project string custom field")
    void shouldCreateStringCustomField() {
        var key = "env_" + suffix;
        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Environment",
                       "type": "STRING",
                       "required": false,
                       "enabled": true,
                       "stringMaxLength": 64
                     }
                     """.formatted(key))
               .post("/api/projects/%d/custom-fields".formatted(project.id()))
               .then()
               .statusCode(201)
               .body("key", equalTo(key))
               .body("label", equalTo("Environment"))
               .body("type", equalTo("STRING"))
               .body("stringMaxLength", equalTo(64))
               .body("projectId", equalTo((int) project.id()));
    }

    @Test
    @DisplayName("Should reject project field key that collides with workflow field")
    void shouldRejectKeyCollisionWithWorkflowField() {
        var key = "shared_" + suffix;
        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Workflow Field",
                       "type": "STRING",
                       "required": false,
                       "stringMaxLength": 32
                     }
                     """.formatted(key))
               .post("/api/workflows/%d/custom-fields".formatted(project.workflow().id()))
               .then()
               .statusCode(201);

        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Project Field",
                       "type": "STRING",
                       "required": false,
                       "stringMaxLength": 32
                     }
                     """.formatted(key))
               .post("/api/projects/%d/custom-fields".formatted(project.id()))
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should block delete when ticket values exist and allow soft-disable")
    void shouldBlockDeleteWhenValuesExistAndAllowDisable() {
        var key = "rel_" + suffix;
        var fieldId = given().header(pmHeader)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                     "key": "%s",
                                     "label": "Release",
                                     "type": "STRING",
                                     "required": false,
                                     "stringMaxLength": 32
                                   }
                                   """.formatted(key))
                             .post("/api/projects/%d/custom-fields".formatted(project.id()))
                             .then()
                             .statusCode(201)
                             .extract()
                             .path("id");

        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "title": "Ticket with CF %s",
                       "description": "Has a custom field value.",
                       "projectId": %d,
                       "categoryId": %d,
                       "customFields": [{"key": "%s", "value": "1.0"}]
                     }
                     """.formatted(suffix, project.id(), category.getId(), key))
               .post("/api/tickets")
               .then()
               .statusCode(201)
               .body("customFields.key", hasItem(key));

        given().header(pmHeader)
               .delete("/api/projects/%d/custom-fields/%d".formatted(project.id(), fieldId))
               .then()
               .statusCode(400);

        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Release",
                       "type": "STRING",
                       "required": false,
                       "enabled": false,
                       "stringMaxLength": 32
                     }
                     """.formatted(key))
               .put("/api/projects/%d/custom-fields/%d".formatted(project.id(), fieldId))
               .then()
               .statusCode(200)
               .body("enabled", equalTo(false));
    }

    @Test
    @DisplayName("Admin can create project custom fields")
    void adminCanCreateProjectCustomField() {
        var key = "adm_" + suffix;
        given().header(adminHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Admin Field",
                       "type": "INTEGER",
                       "required": true,
                       "integerMin": 1,
                       "integerMax": 10
                     }
                     """.formatted(key))
               .post("/api/projects/%d/custom-fields".formatted(project.id()))
               .then()
               .statusCode(201)
               .body("type", equalTo("INTEGER"))
               .body("required", equalTo(true));
    }
}

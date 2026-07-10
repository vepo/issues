package dev.vepo.issues.project.create;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.project.ProjectTestFixtures;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class CreateProjectEndpointTest {

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
    @DisplayName("No user should be allowed to create a project")
    void noUserShouldBeAllowedToCreateProjectTest() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project",
                         "description": "This is a test project.",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("Only project managers should be allowed to create projects")
    void onlyProjectManagerShouldBeAllowedToCreateProjectsTest() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 6);
        var prefix = "P" + suffix.substring(0, 4);
        var projectName = "Test Project " + suffix;
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "%s",
                         "description": "This is a test project.",
                         "prefix": "%s",
                         "workflowId": %d
                     }""".formatted(projectName, prefix, workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(201)
               .body("name", is(projectName))
               .body("description", is("This is a test project."))
               .body("workflow.id", is((int) workflow.id()))
               .body("workflow.name", is(workflow.name()))
               .body("ticketTemplate.enabled", is(false));
    }

    @Test
    @DisplayName("Project name is a required field")
    void projectNameIsRequiredTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "description": "This is a test project.",
                         "workflowId": 1,
                         "prefix": "PRJ"
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("create.request.name"))
               .body("violations[0].message", is("Project name cannot be empty"));
    }

    @Test
    @DisplayName("Workflow ID must be provided and must exist")
    void workflowIdMustExistTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project",
                         "description": "This is a test project.",
                         "prefix": "PRJJJJ"
                     }""")
               .post("/api/projects")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("create.request.workflowId"))
               .body("violations[0].message", is("Workflow ID must be provided"));

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project",
                         "description": "This is a test project.",
                         "workflowId": 9999,
                         "prefix": "PRJJJJ"
                     }""")
               .post("/api/projects")
               .then()
               .statusCode(404)
               .body("message", is("Workflow with ID 9999 does not exist"));
    }

    @Test
    @DisplayName("Project prefix validation")
    void projectPrefixValidationTest() {
        // Test empty prefix
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project with no prefix",
                         "description": "This is a test project.",
                         "prefix": "",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(400);

        // Test prefix that's too long
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project with prefix too long",
                         "description": "This is a test project.",
                         "prefix": "TOOLONGPREFIX",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Project description is a required field")
    void projectDescriptionIsRequiredTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Project Without Description",
                         "prefix": "PWD",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("create.request.description"))
               .body("violations[0].message", is("Project description cannot be empty"));
    }

    @Test
    @DisplayName("Ticket template disabled when omitted")
    void ticketTemplateDisabledWhenOmittedTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Project No Template %s",
                         "description": "Project without a ticket template.",
                         "prefix": "PNT",
                         "workflowId": %d
                     }""".formatted(java.util.UUID.randomUUID().toString().substring(0, 8), workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(201)
               .body("ticketTemplate.enabled", is(false));
    }

    @Test
    @DisplayName("Ticket template enabled with valid fields")
    void ticketTemplateEnabledWithValidFieldsTest() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 6);
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Template Project %s",
                         "description": "Project with template.",
                         "prefix": "TP%s",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": true,
                             "title": "Default ticket title",
                             "description": "Default ticket description for new tickets.",
                             "categoryId": %d,
                             "priority": "MEDIUM"
                         }
                     }""".formatted(suffix, suffix.substring(0, 2), workflow.id(), category.getId()))
               .post("/api/projects")
               .then()
               .statusCode(201)
               .body("ticketTemplate.enabled", is(true))
               .body("ticketTemplate.title", is("Default ticket title"))
               .body("ticketTemplate.description", is("Default ticket description for new tickets."))
               .body("ticketTemplate.categoryId", is(category.getId().intValue()))
               .body("ticketTemplate.priority", is("MEDIUM"));
    }

    @Test
    @DisplayName("Ticket template enabled with partial fields succeeds")
    void ticketTemplateEnabledWithPartialFieldsTest() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 6);
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Partial Template %s",
                         "description": "Project with partial template.",
                         "prefix": "PT%s",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": true,
                             "title": "Default ticket title"
                         }
                     }""".formatted(suffix, suffix.substring(0, 2), workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(201)
               .body("ticketTemplate.enabled", is(true))
               .body("ticketTemplate.title", is("Default ticket title"))
               .body("ticketTemplate.description", nullValue())
               .body("ticketTemplate.categoryId", nullValue());
    }

    @Test
    @DisplayName("Ticket template enabled with no configured fields returns 400")
    void ticketTemplateEnabledWithNoFieldsTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Empty Template %s",
                         "description": "Project with empty template.",
                         "prefix": "ETPL",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": true
                         }
                     }""".formatted(java.util.UUID.randomUUID().toString().substring(0, 8), workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(400)
               .body("message", is("Ticket template must configure at least one field"));
    }

    @Test
    @DisplayName("Ticket template enabled with invalid category returns 404")
    void ticketTemplateEnabledInvalidCategoryTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Invalid Category Template %s",
                         "description": "Project with invalid template category.",
                         "prefix": "ICTP",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": true,
                             "title": "Default ticket title",
                             "description": "Default ticket description for new tickets.",
                             "categoryId": 9999,
                             "priority": "MEDIUM"
                         }
                     }""".formatted(java.util.UUID.randomUUID().toString().substring(0, 8), workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(404)
               .body("message", is("Category with ID 9999 does not exist"));
    }

    @Test
    @DisplayName("Ticket template title and description length validation")
    void ticketTemplateLengthValidationTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Short Title Template %s",
                         "description": "Project with short template title.",
                         "prefix": "STT",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": true,
                             "title": "Bug",
                             "description": "Default ticket description for new tickets.",
                             "categoryId": %d,
                             "priority": "MEDIUM"
                         }
                     }""".formatted(java.util.UUID.randomUUID().toString().substring(0, 8), workflow.id(), category.getId()))
               .post("/api/projects")
               .then()
               .statusCode(400)
               .body("message", is("Ticket template title must be between 5 and 255 characters"));

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Short Desc Template %s",
                         "description": "Project with short template description.",
                         "prefix": "SDT",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": true,
                             "title": "Default ticket title",
                             "description": "tiny",
                             "categoryId": %d,
                             "priority": "MEDIUM"
                         }
                     }""".formatted(java.util.UUID.randomUUID().toString().substring(0, 8), workflow.id(), category.getId()))
               .post("/api/projects")
               .then()
               .statusCode(400)
               .body("message", is("Ticket template description must be between 5 and 1200 characters"));
    }
}

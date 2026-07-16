package dev.vepo.issues.project;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class ProjectSecurityLevelEndpointTest {

    private WorkflowResponse workflow;
    private Header userAuthenticatedHeader;
    private Header pmAuthenticatedHeader;
    private Header adminAuthenticatedHeader;
    private ProjectTestFixtures fixtures;

    @BeforeEach
    void setup() {
        this.fixtures = ProjectTestFixtures.create();
        this.workflow = fixtures.workflow();
        this.userAuthenticatedHeader = fixtures.userAuthenticatedHeader();
        this.pmAuthenticatedHeader = fixtures.pmAuthenticatedHeader();
        this.adminAuthenticatedHeader = Given.authenticatedAdmin();
    }

    @Test
    @DisplayName("Create project defaults securityLevel to INTERNAL")
    void shouldDefaultSecurityLevelToInternalOnCreate() {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Default Level %s",
                         "description": "Defaults to internal.",
                         "prefix": "DL%s",
                         "workflowId": %d
                     }""".formatted(suffix, suffix.substring(0, 2), workflow.id()))
               .when()
               .post("/api/projects")
               .then()
               .statusCode(201)
               .body("securityLevel", is("INTERNAL"));
    }

    @Test
    @DisplayName("Authenticated non-member can read INTERNAL project")
    void shouldAllowAuthenticatedNonMemberToReadInternalProject() {
        var projectId = createProject("INTERNAL", "Internal Read");

        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + projectId)
               .then()
               .statusCode(200)
               .body("securityLevel", is("INTERNAL"));
    }

    @Test
    @DisplayName("Authenticated non-member cannot read PRIVATE project; admin can")
    void shouldDenyNonMemberPrivateAndAllowAdmin() {
        var projectId = createProject("PRIVATE", "Private Read");

        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + projectId)
               .then()
               .statusCode(403);

        given().header(adminAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + projectId)
               .then()
               .statusCode(200)
               .body("securityLevel", is("PRIVATE"));
    }

    @Test
    @DisplayName("Anonymous can read PUBLIC project and list only Public projects")
    void shouldAllowAnonymousPublicReadAndList() {
        var publicId = createProject("PUBLIC", "Public Read");
        var privateId = createProject("PRIVATE", "Private Hidden");

        given().accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + publicId)
               .then()
               .statusCode(200)
               .body("securityLevel", is("PUBLIC"));

        given().accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + privateId)
               .then()
               .statusCode(403);

        given().accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.securityLevel".formatted(publicId), is("PUBLIC"))
               .body("find { it.id == %d }".formatted(privateId), nullValue());
    }

    @Test
    @DisplayName("Non-member cannot create ticket on PUBLIC project")
    void shouldDenyWriteForNonMemberOnPublicProject() {
        var projectId = createProject("PUBLIC", "Public Write Deny");
        var category = fixtures.category();

        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Should fail",
                         "description": "Non-member write denied.",
                         "projectId": %d,
                         "categoryId": %d
                     }""".formatted(projectId, category.getId()))
               .when()
               .post("/api/tickets")
               .then()
               .statusCode(403);
    }

    private long createProject(String securityLevel, String namePrefix) {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        return given().header(pmAuthenticatedHeader)
                      .accept(ContentType.JSON)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "name": "%s %s",
                                "description": "Security level test project.",
                                "prefix": "S%s",
                                "workflowId": %d,
                                "securityLevel": "%s"
                            }""".formatted(namePrefix, suffix, suffix.substring(0, 2), workflow.id(), securityLevel))
                      .when()
                      .post("/api/projects")
                      .then()
                      .statusCode(201)
                      .extract()
                      .jsonPath()
                      .getLong("id");
    }
}

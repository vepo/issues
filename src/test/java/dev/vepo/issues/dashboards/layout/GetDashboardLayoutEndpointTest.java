package dev.vepo.issues.dashboards.layout;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.project.ProjectResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class GetDashboardLayoutEndpointTest {

    private Header authenticatedHeader;
    private Header pmHeader;
    private ProjectResponse project;

    @BeforeEach
    void setUp() {
        authenticatedHeader = Given.authenticatedUser();
        pmHeader = Given.authenticatedProjectManager();
        project = Given.simpleProject();
    }

    @Test
    void shouldReturnDefaultLayoutWhenNoLayoutSaved() {
        given().header(authenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/dashboard/layout")
               .then()
               .statusCode(200)
               .body("widgetIds", contains("tickets-by-status",
                                           "tickets-by-priority",
                                           "performance-kpi",
                                           "recent-tickets"));
    }

    @Test
    void shouldRejectUnauthenticatedGetLayout() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/dashboard/layout")
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("Should reject dashboard layout for non-member")
    void shouldForbidNonMemberOnForeignProject() {
        var foreignProject = createForeignProject();

        given().header(authenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/dashboard/layout".formatted(foreignProject.id()))
               .then()
               .statusCode(403);
    }

    private ProjectResponse createForeignProject() {
        return given().header(pmHeader)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "name": "Foreign Dashboard %s",
                                "description": "No membership for user",
                                "prefix": "FD%s",
                                "workflowId": %d,
                                "securityLevel": "PRIVATE"
                            }
                            """.formatted(UUID.randomUUID(),
                                          UUID.randomUUID().toString().substring(0, 4).toUpperCase(),
                                          Given.simpleWorkflow().id()))
                      .post("/api/projects")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(ProjectResponse.class);
    }
}

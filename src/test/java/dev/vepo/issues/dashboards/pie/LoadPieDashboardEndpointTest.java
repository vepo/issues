package dev.vepo.issues.dashboards.pie;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

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
class LoadPieDashboardEndpointTest {

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
    void shouldLoadPieDashboardWhenAuthenticated() {
        given().header(authenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/dashboard/pie/tickets-by-status")
               .then()
               .statusCode(200)
               .body("datasets", notNullValue());
    }

    @Test
    void shouldRejectUnauthenticatedPieDashboard() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/dashboard/pie/tickets-by-status")
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("Should reject pie dashboard for non-member")
    void shouldForbidNonMemberOnForeignProject() {
        var foreignProject = given().header(pmHeader)
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Foreign Pie %s",
                                              "description": "No membership for user",
                                              "prefix": "FP%s",
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

        given().header(authenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/dashboard/pie/tickets-by-status".formatted(foreignProject.id()))
               .then()
               .statusCode(403);
    }
}

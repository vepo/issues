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
class SaveDashboardLayoutEndpointTest {

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
    void shouldReturnSavedLayoutOrderAfterPut() {
        var layoutPath = "/api/projects/" + project.id() + "/dashboard/layout";
        var savedOrder = """
                         {
                           "widgetIds": [
                             "recent-tickets",
                             "tickets-by-day",
                             "performance-kpi",
                             "tickets-by-status"
                           ]
                         }
                         """;

        given().header(authenticatedHeader)
               .contentType(ContentType.JSON)
               .body(savedOrder)
               .when()
               .put(layoutPath)
               .then()
               .statusCode(200)
               .body("widgetIds", contains("recent-tickets",
                                           "tickets-by-day",
                                           "performance-kpi",
                                           "tickets-by-status"));

        given().header(authenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get(layoutPath)
               .then()
               .statusCode(200)
               .body("widgetIds", contains("recent-tickets",
                                           "tickets-by-day",
                                           "performance-kpi",
                                           "tickets-by-status"));
    }

    @Test
    void shouldRejectUnknownWidgetId() {
        given().header(authenticatedHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "widgetIds": ["tickets-by-status", "not-a-real-widget"]
                     }
                     """)
               .when()
               .put("/api/projects/" + project.id() + "/dashboard/layout")
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should reject saving dashboard layout for non-member")
    void shouldForbidNonMemberSaveOnForeignProject() {
        var foreignProject = given().header(pmHeader)
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Foreign Save Layout %s",
                                              "description": "No membership for user",
                                              "prefix": "FS%s",
                                              "workflowId": %d
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
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "widgetIds": ["tickets-by-status"]
                     }
                     """)
               .when()
               .put("/api/projects/%d/dashboard/layout".formatted(foreignProject.id()))
               .then()
               .statusCode(403);
    }
}

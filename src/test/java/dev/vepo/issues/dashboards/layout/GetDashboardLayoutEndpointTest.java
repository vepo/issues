package dev.vepo.issues.dashboards.layout;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.project.ProjectResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class GetDashboardLayoutEndpointTest {

    private Header authenticatedHeader;
    private ProjectResponse project;

    @BeforeEach
    void setUp() {
        authenticatedHeader = Given.authenticatedUser();
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
               .statusCode(401);
    }
}

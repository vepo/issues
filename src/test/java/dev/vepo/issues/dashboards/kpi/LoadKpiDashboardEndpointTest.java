package dev.vepo.issues.dashboards.kpi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.project.ProjectResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class LoadKpiDashboardEndpointTest {

    private Header authenticatedHeader;
    private ProjectResponse project;

    @BeforeEach
    void setUp() {
        authenticatedHeader = Given.authenticatedUser();
        project = Given.simpleProject();
    }

    @Test
    void shouldLoadKpiDashboardWhenAuthenticated() {
        given().header(authenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/dashboard/kpi/performance-kpi")
               .then()
               .statusCode(200)
               .body("total", notNullValue());
    }

    @Test
    void shouldRejectUnauthenticatedKpiDashboard() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/dashboard/kpi/performance-kpi")
               .then()
               .statusCode(403);
    }
}

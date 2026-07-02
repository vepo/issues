package dev.vepo.issues.dashboards.pie;

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
class LoadPieDashboardEndpointTest {

    private Header authenticatedHeader;
    private ProjectResponse project;

    @BeforeEach
    void setUp() {
        authenticatedHeader = Given.authenticatedUser();
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
               .statusCode(401);
    }
}

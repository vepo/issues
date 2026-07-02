package dev.vepo.issues.project.list;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.project.ProjectTestFixtures;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class ListProjectsEndpointTest {

    private WorkflowResponse workflow;
    private Header userAuthenticatedHeader;
    private Header pmAuthenticatedHeader;

    @BeforeEach
    void setup() {
        var fixtures = ProjectTestFixtures.create();
        this.workflow = fixtures.workflow();
        this.userAuthenticatedHeader = fixtures.userAuthenticatedHeader();
        this.pmAuthenticatedHeader = fixtures.pmAuthenticatedHeader();
    }

    @Test
    @DisplayName("Non authenticated user should not be able to list projects")
    void nonAuthenticatedUserShouldNotListProjectsTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/projects")
               .then()
               .statusCode(401);
    }

    @Test
    @DisplayName("Only authenticated users should be able to list projects")
    void onlyAuthenticatedUsersShouldListProjectsTest() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Project listing should return the created project")
    void anyoneCanListProjects() {
        Stream.of(userAuthenticatedHeader, pmAuthenticatedHeader)
              .forEach(header -> given().header(header)
                                        .accept(ContentType.JSON)
                                        .when()
                                        .get("/api/projects")
                                        .then()
                                        .statusCode(200)
                                        .body("$.size()", greaterThanOrEqualTo(1))
                                        .body("find { it.name == 'Test Project' }.name", is("Test Project"))
                                        .body("find { it.name == 'Test Project' }.description", is("This is a test project."))
                                        .body("find { it.name == 'Test Project' }.workflow.id", is((int) workflow.id()))
                                        .body("find { it.name == 'Test Project' }.workflow.name", is(workflow.name())));
    }
}

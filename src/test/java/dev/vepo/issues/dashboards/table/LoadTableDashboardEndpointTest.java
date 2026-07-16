package dev.vepo.issues.dashboards.table;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.project.ProjectResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class LoadTableDashboardEndpointTest {

    private Header authenticatedHeader;
    private ProjectResponse project;

    @BeforeEach
    void setUp() {
        authenticatedHeader = Given.authenticatedUser();
        project = Given.simpleProject();
    }

    @Test
    void shouldLoadTableDashboardWhenAuthenticated() {
        given().header(authenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/dashboard/table/recent-tickets")
               .then()
               .statusCode(200)
               .body("columns", notNullValue());
    }

    @Test
    void shouldReturnAtMostTwentyRecentTicketsWhenMoreExist() {
        var isolatedProject = createIsolatedProject();
        var category = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                                    .save(new Category("RecentLimit" + UUID.randomUUID(), "blue")));
        var pmHeader = Given.authenticatedProjectManager();
        for (var i = 1; i <= 21; i++) {
            given().header(pmHeader)
                   .contentType(ContentType.JSON)
                   .body("""
                         {
                             "title": "Recent ticket %02d for limit",
                             "description": "Ticket used to assert recent-tickets row limit.",
                             "projectId": %d,
                             "categoryId": %d
                         }""".formatted(i, isolatedProject.id(), category.getId()))
                   .when()
                   .post("/api/tickets")
                   .then()
                   .statusCode(201);
        }

        given().header(authenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + isolatedProject.id() + "/dashboard/table/recent-tickets")
               .then()
               .statusCode(200)
               .body("rows", hasSize(20));
    }

    @Test
    void shouldRejectUnauthenticatedTableDashboard() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/dashboard/table/recent-tickets")
               .then()
               .statusCode(403);
    }

    private static ProjectResponse createIsolatedProject() {
        var workflow = Given.simpleWorkflow();
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var project = given().header(Given.authenticatedProjectManager())
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "Recent Tickets Limit %s",
                                       "description": "Isolated project for recent-tickets limit.",
                                       "prefix": "RL%s",
                                       "workflowId": %d
                                   }""".formatted(suffix, suffix, workflow.id()))
                             .when()
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(ProjectResponse.class);
        Given.addProjectMember(project.id(), "user@issues.vepo.dev");
        return project;
    }
}

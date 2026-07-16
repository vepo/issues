package dev.vepo.issues.dashboards.kpi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.project.ProjectResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class DashboardAggregateSmokeTest {

    private Header pmHeader;
    private Header userHeader;
    private ProjectResponse project;
    private Category category;

    @BeforeEach
    void setUp() {
        pmHeader = Given.authenticatedProjectManager();
        userHeader = Given.authenticatedUser();
        project = Given.simpleProject();
        category = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                                .save(new Category("DashSmoke" + UUID.randomUUID(), "blue")));
    }

    @Test
    @DisplayName("Soft-deleted tickets are excluded from KPI aggregate")
    void shouldExcludeSoftDeletedTicketsFromKpi() {
        var ticketId = given().header(pmHeader)
                              .contentType(ContentType.JSON)
                              .body("""
                                    {
                                        "title": "Dashboard soft-delete smoke",
                                        "description": "Counted then deleted for KPI smoke.",
                                        "projectId": %d,
                                        "categoryId": %d
                                    }""".formatted(project.id(), category.getId()))
                              .post("/api/tickets")
                              .then()
                              .statusCode(201)
                              .extract()
                              .jsonPath()
                              .getLong("id");

        var totalBefore = given().header(userHeader)
                                 .accept(ContentType.JSON)
                                 .when()
                                 .get("/api/projects/%d/dashboard/kpi/performance-kpi".formatted(project.id()))
                                 .then()
                                 .statusCode(200)
                                 .body("total", notNullValue())
                                 .extract()
                                 .jsonPath()
                                 .getInt("total");

        given().header(pmHeader)
               .when()
               .delete("/api/tickets/" + ticketId)
               .then()
               .statusCode(204);

        given().header(userHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/dashboard/kpi/PERFORMANCE_KPI".formatted(project.id()))
               .then()
               .statusCode(200)
               .body("total", equalTo(totalBefore - 1));
    }
}

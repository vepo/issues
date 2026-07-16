package dev.vepo.issues.project.tickets.list;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

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
class ListProjectTicketsEndpointTest {

    private Header authenticatedHeader;
    private ProjectResponse project;

    @BeforeEach
    void setUp() {
        authenticatedHeader = Given.authenticatedUser();
        project = Given.simpleProject();
        var category = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                                    .save(new Category("Bug" + UUID.randomUUID(), "red")));
        Given.simpleTicket(project.id(), category.getId());
    }

    @Test
    void shouldListProjectTicketsWhenAuthenticated() {
        given().header(authenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/tickets")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(1));
    }

    @Test
    void shouldRejectUnauthenticatedListProjectTickets() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/tickets")
               .then()
               .statusCode(403);
    }
}

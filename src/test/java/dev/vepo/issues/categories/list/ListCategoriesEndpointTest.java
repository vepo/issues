package dev.vepo.issues.categories.list;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ListCategoriesEndpointTest {

    @Test
    void shouldListCategoriesWhenAuthenticated() {
        given().header(Given.authenticatedUser())
               .accept(ContentType.JSON)
               .when()
               .get("/api/categories")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(0));
    }

    @Test
    void shouldRejectUnauthenticatedListCategories() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/categories")
               .then()
               .statusCode(401);
    }
}

package dev.vepo.issues.categories.update;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class UpdateCategoryEndpointTest {

    @Test
    void shouldUpdateCategoryWhenAdmin() {
        var header = Given.authenticatedAdmin();
        var createdId = given().header(header)
                               .contentType(ContentType.JSON)
                               .body("""
                                     {
                                       "name": "UpdateMe",
                                       "color": "#111111"
                                     }
                                     """)
                               .when()
                               .post("/api/categories")
                               .then()
                               .statusCode(201)
                               .extract()
                               .path("id");

        given().header(header)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "name": "Updated Category",
                       "color": "#00AA88"
                     }
                     """)
               .when()
               .put("/api/categories/" + createdId)
               .then()
               .statusCode(200)
               .body("name", equalTo("Updated Category"))
               .body("color", equalTo("#00AA88"));
    }

    @Test
    void regularUserShouldNotUpdateCategory() {
        given().header(Given.authenticatedUser())
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "name": "Denied",
                       "color": "#00635D"
                     }
                     """)
               .when()
               .put("/api/categories/1")
               .then()
               .statusCode(403);
    }
}

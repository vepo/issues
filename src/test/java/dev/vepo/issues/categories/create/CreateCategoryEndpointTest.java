package dev.vepo.issues.categories.create;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class CreateCategoryEndpointTest {

    @Test
    void shouldCreateCategoryWhenAdmin() {
        given().header(Given.authenticatedAdmin())
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "name": "Security",
                       "color": "#BF0603"
                     }
                     """)
               .when()
               .post("/api/categories")
               .then()
               .statusCode(201)
               .body("name", equalTo("Security"))
               .body("color", equalTo("#BF0603"));
    }

    @Test
    void shouldRejectDuplicateCategoryName() {
        var header = Given.authenticatedAdmin();
        var body = """
                   {
                     "name": "SecurityDup",
                     "color": "#BF0603"
                   }
                   """;
        given().header(header)
               .contentType(ContentType.JSON)
               .body(body)
               .when()
               .post("/api/categories")
               .then()
               .statusCode(201);

        given().header(header)
               .contentType(ContentType.JSON)
               .body(body)
               .when()
               .post("/api/categories")
               .then()
               .statusCode(400);
    }

    @Test
    void regularUserShouldNotCreateCategory() {
        given().header(Given.authenticatedUser())
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "name": "Ops",
                       "color": "#00635D"
                     }
                     """)
               .when()
               .post("/api/categories")
               .then()
               .statusCode(403);
    }
}

package dev.vepo.issues.auth.me;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class MeEndpointTest {

    @Test
    @Order(1)
    @DisplayName("me endpoint should return user information for authenticated user")
    void shouldReturnAuthenticatedUser() {
        given().header(Given.authenticatedUser())
               .accept(ContentType.JSON)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(200)
               .body("id", is(notNullValue()))
               .body("email", is("user@issues.vepo.dev"))
               .body("roles", is(notNullValue()))
               .body("roles.size()", is(1))
               .body("roles[0]", is("user"))
               .body("locale", is("pt"));
    }

    @Test
    @Order(2)
    @DisplayName("me endpoint should return 401 for unauthenticated user")
    void shouldRejectUnauthenticatedMe() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(401);
    }
}

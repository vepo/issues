package dev.vepo.issues.auth.login;

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
class LoginEndpointTest {

    @Test
    @Order(1)
    @DisplayName("Login request should have email and password")
    void shouldValidateLoginRequestFields() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                         "email": "email@test.com"
                     }
                     """)
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("login.request.password"))
               .body("violations[0].message", is("Password must not be empty!"));
        given().contentType(ContentType.JSON)
               .body("""
                      {
                          "password": "password"
                      }
                     """)
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("login.request.email"))
               .body("violations[0].message", is("Email must not be empty!"));
    }

    @Test
    @Order(2)
    @DisplayName("Login request should return 401 for invalid credentials")
    void shouldRejectInvalidCredentials() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                         "email": "not-found-user@test.com",
                         "password": "wrong-password"
                     }
                     """)
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(401)
               .body("message", is("Invalid credentials!"));
    }

    @Test
    @Order(3)
    @DisplayName("Login request should return JWT token for valid credentials")
    void shouldReturnTokenForValidCredentials() {
        var user = Given.randomUser();
        given().contentType(ContentType.JSON)
               .body("""
                     {
                         "email": "%s",
                         "password": "password"
                     }
                     """.formatted(user.getEmail()))
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(200)
               .body("token", is(notNullValue()));
    }
}

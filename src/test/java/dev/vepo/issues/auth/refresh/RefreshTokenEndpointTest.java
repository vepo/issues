package dev.vepo.issues.auth.refresh;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class RefreshTokenEndpointTest {

    @Test
    @DisplayName("Refresh should issue a new access token and rotate refresh token")
    void shouldRefreshAccessToken() {
        var user = Given.randomUser();
        var login = given().contentType(ContentType.JSON)
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
                           .body("token", is(notNullValue()))
                           .body("refreshToken", is(notNullValue()))
                           .body("expiresIn", is(900))
                           .extract()
                           .path("refreshToken");

        given().contentType(ContentType.JSON)
               .body("""
                     {
                         "refreshToken": "%s"
                     }
                     """.formatted(login))
               .when()
               .post("/api/auth/refresh")
               .then()
               .statusCode(200)
               .body("token", is(notNullValue()))
               .body("refreshToken", is(notNullValue()))
               .body("expiresIn", is(900));

        given().contentType(ContentType.JSON)
               .body("""
                     {
                         "refreshToken": "%s"
                     }
                     """.formatted(login))
               .when()
               .post("/api/auth/refresh")
               .then()
               .statusCode(401);
    }

    @Test
    @DisplayName("Refresh should reject empty refresh token")
    void shouldValidateRefreshTokenRequest() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                         "refreshToken": ""
                     }
                     """)
               .when()
               .post("/api/auth/refresh")
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Refreshed access token should authorize protected endpoints")
    void refreshedTokenShouldAuthorizeMeEndpoint() {
        var user = Given.randomUser();
        var tokens = given().contentType(ContentType.JSON)
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
                            .extract()
                            .response();

        var refreshToken = tokens.path("refreshToken");
        var accessToken = given().contentType(ContentType.JSON)
                                 .body("""
                                       {
                                           "refreshToken": "%s"
                                       }
                                       """.formatted(refreshToken))
                                 .when()
                                 .post("/api/auth/refresh")
                                 .then()
                                 .statusCode(200)
                                 .extract()
                                 .path("token");

        given().header("Authorization", "Bearer " + accessToken)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(200)
               .body("email", is(user.getEmail()));
    }
}

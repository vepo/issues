package dev.vepo.issues.auth.register;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.UserResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class RegisterUserEndpointTest {

    @Test
    void shouldRegisterUserWithUserRoleOnly() {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var username = "newuser" + suffix;
        var email = username + "@example.com";

        var response = given().contentType(ContentType.JSON)
                              .accept(ContentType.JSON)
                              .body("""
                                    {
                                        "username": "%s",
                                        "name": "New User",
                                        "email": "%s",
                                        "password": "Secret123"
                                    }
                                    """.formatted(username, email))
                              .when()
                              .post("/api/auth/register")
                              .then()
                              .statusCode(201)
                              .extract()
                              .as(UserResponse.class);

        assertThat(response.username()).isEqualTo(username);
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.roles()).containsExactly(Role.USER_ROLE);

        var token = given().contentType(ContentType.JSON)
                           .body("""
                                 {
                                     "email": "%s",
                                     "password": "Secret123"
                                 }
                                 """.formatted(email))
                           .when()
                           .post("/api/auth/login")
                           .then()
                           .statusCode(200)
                           .extract()
                           .path("token")
                           .toString();

        given().header("Authorization", "Bearer " + token)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(200)
               .body("locale", org.hamcrest.Matchers.is("pt"));
    }

    @Test
    void shouldSeedLocaleFromAcceptLanguageOnRegister() {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var username = "enuser" + suffix;
        var email = username + "@example.com";

        given().contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .header("Accept-Language", "en-US,en;q=0.9")
               .body("""
                     {
                         "username": "%s",
                         "name": "English User",
                         "email": "%s",
                         "password": "Secret123"
                     }
                     """.formatted(username, email))
               .when()
               .post("/api/auth/register")
               .then()
               .statusCode(201);

        var token = given().contentType(ContentType.JSON)
                           .body("""
                                 {
                                     "email": "%s",
                                     "password": "Secret123"
                                 }
                                 """.formatted(email))
                           .when()
                           .post("/api/auth/login")
                           .then()
                           .statusCode(200)
                           .extract()
                           .path("token")
                           .toString();

        given().header("Authorization", "Bearer " + token)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(200)
               .body("locale", org.hamcrest.Matchers.is("en"));
    }

    @Test
    void shouldPreferExplicitLocaleOnRegisterOverAcceptLanguage() {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var username = "ptuser" + suffix;
        var email = username + "@example.com";

        given().contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .header("Accept-Language", "en-US")
               .body("""
                     {
                         "username": "%s",
                         "name": "Explicit PT",
                         "email": "%s",
                         "password": "Secret123",
                         "locale": "pt"
                     }
                     """.formatted(username, email))
               .when()
               .post("/api/auth/register")
               .then()
               .statusCode(201);

        var token = given().contentType(ContentType.JSON)
                           .body("""
                                 {
                                     "email": "%s",
                                     "password": "Secret123"
                                 }
                                 """.formatted(email))
                           .when()
                           .post("/api/auth/login")
                           .then()
                           .statusCode(200)
                           .extract()
                           .path("token")
                           .toString();

        given().header("Authorization", "Bearer " + token)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(200)
               .body("locale", org.hamcrest.Matchers.is("pt"));
    }

    @Test
    void shouldRejectWeakPassword() {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var username = "wk" + suffix;

        given().contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "username": "%s",
                         "name": "Weak User",
                         "email": "%s@example.com",
                         "password": "secret12"
                     }
                     """.formatted(username, username))
               .when()
               .post("/api/auth/register")
               .then()
               .statusCode(400);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var email = "dup" + suffix + "@example.com";
        var firstUsername = "dup1" + suffix;
        var secondUsername = "dup2" + suffix;

        given().contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "username": "%s",
                         "name": "First User",
                         "email": "%s",
                         "password": "Secret123"
                     }
                     """.formatted(firstUsername, email))
               .when()
               .post("/api/auth/register")
               .then()
               .statusCode(201);

        given().contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "username": "%s",
                         "name": "Second User",
                         "email": "%s",
                         "password": "Secret123"
                     }
                     """.formatted(secondUsername, email))
               .when()
               .post("/api/auth/register")
               .then()
               .statusCode(400);
    }
}

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

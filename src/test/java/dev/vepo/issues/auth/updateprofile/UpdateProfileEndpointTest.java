package dev.vepo.issues.auth.updateprofile;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.auth.LoginResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class UpdateProfileEndpointTest {

    @Test
    @DisplayName("Should update own name and email")
    void shouldUpdateOwnProfile() {
        var user = Given.randomUser();
        var login = loginAs(user.getEmail());

        given().header("Authorization", "Bearer " + login.token())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Updated Name",
                         "email": "updated-%s"
                     }
                     """.formatted(user.getEmail()))
               .when()
               .post("/api/auth/profile")
               .then()
               .statusCode(200)
               .body("name", is("Updated Name"))
               .body("email", is("updated-" + user.getEmail()))
               .body("locale", is("pt"));

        given().header("Authorization", "Bearer " + login.token())
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(200)
               .body("name", is("Updated Name"))
               .body("email", is("updated-" + user.getEmail()))
               .body("locale", is("pt"));
    }

    @Test
    @DisplayName("Should update UI locale preference")
    void shouldUpdateUiLocalePreference() {
        var user = Given.randomUser();
        var login = loginAs(user.getEmail());

        given().header("Authorization", "Bearer " + login.token())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "%s",
                         "email": "%s",
                         "locale": "en"
                     }
                     """.formatted(user.getName(), user.getEmail()))
               .when()
               .post("/api/auth/profile")
               .then()
               .statusCode(200)
               .body("locale", is("en"));

        given().header("Authorization", "Bearer " + login.token())
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(200)
               .body("locale", is("en"));
    }

    @Test
    @DisplayName("Should reject invalid UI locale")
    void shouldRejectInvalidUiLocale() {
        var user = Given.randomUser();
        var login = loginAs(user.getEmail());

        given().header("Authorization", "Bearer " + login.token())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "%s",
                         "email": "%s",
                         "locale": "fr"
                     }
                     """.formatted(user.getName(), user.getEmail()))
               .when()
               .post("/api/auth/profile")
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should reject email already used by another user")
    void shouldRejectDuplicateEmail() {
        var user = Given.randomUser();
        var other = Given.randomUser();
        var login = loginAs(user.getEmail());

        given().header("Authorization", "Bearer " + login.token())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Updated Name",
                         "email": "%s"
                     }
                     """.formatted(other.getEmail()))
               .when()
               .post("/api/auth/profile")
               .then()
               .statusCode(400)
               .body("message", is("Email already in use"));
    }

    @Test
    @DisplayName("Should require authentication")
    void shouldRequireAuthentication() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Name",
                         "email": "test@example.com"
                     }
                     """)
               .when()
               .post("/api/auth/profile")
               .then()
               .statusCode(401);
    }

    private LoginResponse loginAs(String email) {
        return given().contentType(ContentType.JSON)
                      .body("""
                            {
                                "email": "%s",
                                "password": "password"
                            }
                            """.formatted(email))
                      .when()
                      .post("/api/auth/login")
                      .then()
                      .statusCode(200)
                      .extract()
                      .as(LoginResponse.class);
    }
}

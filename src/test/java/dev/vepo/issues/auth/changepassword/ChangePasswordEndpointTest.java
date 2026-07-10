package dev.vepo.issues.auth.changepassword;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.auth.LoginResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ChangePasswordEndpointTest {

    @Test
    @DisplayName("Should change password when current password is correct")
    void shouldChangePasswordWhenCurrentPasswordIsCorrect() {
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
                           .extract()
                           .as(LoginResponse.class);

        given().header("Authorization", "Bearer " + login.token())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "currentPassword": "password",
                         "newPassword": "newSecret99"
                     }""")
               .post("/api/auth/change-password")
               .then()
               .statusCode(200);

        given().contentType(ContentType.JSON)
               .body("""
                     {
                         "email": "%s",
                         "password": "newSecret99"
                     }
                     """.formatted(user.getEmail()))
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(200);
    }

    @Test
    @DisplayName("Should reject change password when current password is wrong")
    void shouldRejectChangePasswordWhenCurrentPasswordIsWrong() {
        given().header(Given.authenticatedUser())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "currentPassword": "wrong-password",
                         "newPassword": "Newpassword1"
                     }""")
               .post("/api/auth/change-password")
               .then()
               .statusCode(400)
               .body("message", is("Current password is incorrect"));
    }

    @Test
    @DisplayName("Should reject weak new password missing uppercase")
    void shouldRejectWeakNewPassword() {
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
                           .extract()
                           .as(LoginResponse.class);

        given().header("Authorization", "Bearer " + login.token())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "currentPassword": "password",
                         "newPassword": "weakpass1"
                     }""")
               .post("/api/auth/change-password")
               .then()
               .statusCode(400);
    }
}

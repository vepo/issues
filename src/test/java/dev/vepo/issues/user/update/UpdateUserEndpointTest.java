package dev.vepo.issues.user.update;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.user.CreateUserRequest;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.UserResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class UpdateUserEndpointTest {

    private Header authenticatedAdmin;

    @BeforeEach
    void setUp() {
        authenticatedAdmin = Given.authenticatedAdmin();
    }

    @Test
    void shouldUpdateUserWhenRequestIsValid() {
        var user = Given.randomUser();
        var updatedUser = given().when()
                                 .header(authenticatedAdmin)
                                 .accept(MediaType.APPLICATION_JSON)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(new CreateUserRequest(user.getUsername(), "New Name", user.getEmail(), List.of(Role.ADMIN_ROLE)))
                                 .post("/api/users/" + user.getId())
                                 .then()
                                 .statusCode(200)
                                 .extract()
                                 .as(UserResponse.class);

        assertThat(updatedUser.name()).isEqualTo("New Name");
        assertThat(updatedUser.roles()).contains(Role.ADMIN_ROLE);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingUnknownUser() {
        given().when()
               .header(authenticatedAdmin)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .body(new CreateUserRequest("not-found", "New Name", "not-found-user@email.com", List.of(Role.ADMIN_ROLE)))
               .post("/api/users/999")
               .then()
               .statusCode(404);
    }

    @Test
    void shouldRejectUpdateUserWithInvalidRole() {
        var user = Given.randomUser();
        given().when()
               .header(authenticatedAdmin)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .body(new CreateUserRequest(user.getUsername(), "New Name", user.getEmail(), List.of("INVALID_ROLE")))
               .post("/api/users/" + user.getId())
               .then()
               .statusCode(400);
    }
}

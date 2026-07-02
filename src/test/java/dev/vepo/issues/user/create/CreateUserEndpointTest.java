package dev.vepo.issues.user.create;

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
class CreateUserEndpointTest {

    private Header authenticatedAdmin;

    @BeforeEach
    void setUp() {
        authenticatedAdmin = Given.authenticatedAdmin();
    }

    @Test
    void shouldCreateUserWhenRequestIsValid() {
        var username = "nu" + java.util.UUID.randomUUID().toString().substring(0, 6);
        var response = given().when()
                              .header(authenticatedAdmin)
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON)
                              .body(new CreateUserRequest(username, "New User", username + "@user.com", List.of(Role.ADMIN_ROLE)))
                              .post("/api/users")
                              .then()
                              .statusCode(201)
                              .extract()
                              .as(UserResponse.class);
        assertThat(response.username()).isEqualTo(username);
    }

    @Test
    void shouldRejectCreateUserWithInvalidRole() {
        given().when()
               .header(authenticatedAdmin)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .body(new CreateUserRequest("user-invalid", "Name", "email@test.com", List.of("INVALID_ROLE")))
               .post("/api/users")
               .then()
               .statusCode(400);
    }
}

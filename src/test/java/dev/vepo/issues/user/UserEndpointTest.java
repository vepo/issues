package dev.vepo.issues.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class UserEndpointTest {

    String passwordDefault = "defaultPass";

    UserEndpoint userEndpoint;

    private Header authenticatedUser;
    private Header authenticatedAdmin;

    @BeforeEach
    void setUp() {
        this.authenticatedUser = Given.authenticatedUser();
        this.authenticatedAdmin = Given.authenticatedAdmin();
    }

    @Test
    void testFindUserById_found() {
        var user = Given.randomUser();
        var userResp = given().when()
                              .header(authenticatedUser)
                              .accept(MediaType.APPLICATION_JSON)
                              .get("/api/users/" + user.getId())
                              .then()
                              .statusCode(200)
                              .extract()
                              .as(UserResponse.class);
        assertThat(userResp.id()).isEqualTo(user.getId());
    }

    @Test
    void testFindUserById_notFound() {
        given().when()
               .header(authenticatedUser)
               .accept(MediaType.APPLICATION_JSON)
               .get("/api/users/9999")
               .then()
               .statusCode(404);
    }

    @Test
    void testCreate_success() {
        var response = given().when()
                              .header(authenticatedAdmin)
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON)
                              .body(new CreateUserRequest("newuser", "New User", "new@user.com", List.of(Role.ADMIN_ROLE)))
                              .post("/api/users")
                              .then()
                              .statusCode(201)
                              .extract()
                              .as(UserResponse.class);
        assertThat(response.username()).isEqualTo("newuser");
    }

    @Test
    void testCreate_invalidRole() {
        given().when()
               .header(authenticatedAdmin)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .body(new CreateUserRequest("user-invalid", "Name", "email@test.com", List.of("INVALID_ROLE")))
               .post("/api/users")
               .then()
               .statusCode(400);
    }

    @Test
    void testUpdate_success() {
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
    void testUpdate_userNotFound() {
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
    void testUpdate_invalidRole() {
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

    @Test
    void testSearch_success() {
        var user1 = Given.randomUser();
        var user2 = Given.randomUser();

        var response = given().when()
                              .header(authenticatedAdmin)
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON)
                              .queryParam("name", user1.getName())
                              .get("/api/users/search")
                              .then()
                              .statusCode(200)
                              .extract()
                              .as(UserResponse[].class);
        assertThat(response).hasSize(1);
        assertThat(response[0].username()).isEqualTo(user1.getUsername());

        response = given().when()
                          .header(authenticatedAdmin)
                          .accept(MediaType.APPLICATION_JSON)
                          .contentType(MediaType.APPLICATION_JSON)
                          .queryParam("email", user2.getEmail())
                          .get("/api/users/search")
                          .then()
                          .statusCode(200)
                          .extract()
                          .as(UserResponse[].class);
        assertThat(response).hasSize(1);
        assertThat(response[0].username()).isEqualTo(user2.getUsername());

        response = given().when()
                          .header(authenticatedAdmin)
                          .accept(MediaType.APPLICATION_JSON)
                          .contentType(MediaType.APPLICATION_JSON)
                          .queryParam("roles", List.of(Role.USER_ROLE))
                          .get("/api/users/search")
                          .then()
                          .statusCode(200)
                          .extract()
                          .as(UserResponse[].class);
        assertThat(response).hasSizeGreaterThan(2);
    }

    @Test
    void testSearch_invalidRole() {
        given().when()
               .header(authenticatedAdmin)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .queryParam("roles", List.of("INVALID-ROLE"))
               .get("/api/users/search")
               .then()
               .statusCode(400);
    }
}
package dev.vepo.issues.user.search;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.UserResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class SearchUsersEndpointTest {

    private Header authenticatedAdmin;

    @BeforeEach
    void setUp() {
        authenticatedAdmin = Given.authenticatedAdmin();
    }

    @Test
    void shouldSearchUsersByFilter() {
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

        Given.authenticatedProjectManager();

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
        assertThat(response).extracting(UserResponse::username)
                            .contains(user1.getUsername(), user2.getUsername());
        assertThat(response).extracting(UserResponse::email)
                            .doesNotContain("pm@issues.vepo.dev");
    }

    @Test
    void shouldRejectSearchUsersWithInvalidRole() {
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

package dev.vepo.issues.user.find;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.user.UserResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class FindUserByIdEndpointTest {

    private Header authenticatedUser;

    @BeforeEach
    void setUp() {
        authenticatedUser = Given.authenticatedUser();
    }

    @Test
    void shouldFindUserByIdWhenUserExists() {
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
    void shouldReturnNotFoundWhenUserDoesNotExist() {
        given().when()
               .header(authenticatedUser)
               .accept(MediaType.APPLICATION_JSON)
               .get("/api/users/9999")
               .then()
               .statusCode(404);
    }
}

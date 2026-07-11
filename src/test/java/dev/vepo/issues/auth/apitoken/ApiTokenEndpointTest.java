package dev.vepo.issues.auth.apitoken;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class ApiTokenEndpointTest {

    @Test
    @DisplayName("Should create a personal API token and return the secret once")
    void shouldCreatePersonalApiTokenAndReturnSecretOnce() {
        var auth = Given.authenticatedUser();

        given().header(auth)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Cursor agent"
                     }
                     """)
               .when()
               .post("/api/account/api-tokens")
               .then()
               .statusCode(201)
               .body("id", notNullValue())
               .body("name", is("Cursor agent"))
               .body("prefix", notNullValue())
               .body("createdAt", notNullValue())
               .body("token", startsWith("iss_pat_"));
    }

    @Test
    @DisplayName("Should list personal API tokens without the full secret")
    void shouldListPersonalApiTokensWithoutSecret() {
        var auth = Given.authenticatedUser();
        var secret = createPersonalApiToken(auth, "Listable token");

        given().header(auth)
               .accept(ContentType.JSON)
               .when()
               .get("/api/account/api-tokens")
               .then()
               .statusCode(200)
               .body("find { it.name == 'Listable token' }.id", notNullValue())
               .body("find { it.name == 'Listable token' }.name", is("Listable token"))
               .body("find { it.name == 'Listable token' }.tokenPrefix", notNullValue())
               .body("find { it.name == 'Listable token' }.createdAt", notNullValue())
               .body("find { it.name == 'Listable token' }", not(hasKey("token")))
               .body(not(containsString(secret)));
    }

    @Test
    @DisplayName("Should revoke a personal API token")
    void shouldRevokePersonalApiToken() {
        var auth = Given.authenticatedUser();
        var tokenId = createPersonalApiTokenId(auth, "Token to revoke");

        given().header(auth)
               .when()
               .delete("/api/account/api-tokens/{id}", tokenId)
               .then()
               .statusCode(204);

        given().header(auth)
               .accept(ContentType.JSON)
               .when()
               .get("/api/account/api-tokens")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.revokedAt".formatted(tokenId), notNullValue());
    }

    @Test
    @DisplayName("Should authenticate with a personal API token like the owning user")
    void shouldAuthenticateWithPersonalApiToken() {
        var auth = Given.authenticatedUser();
        var secret = createPersonalApiToken(auth, "Auth token");

        given().header(new Header("Authorization", "Bearer " + secret))
               .accept(ContentType.JSON)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(200)
               .body("username", is("user"))
               .body("email", is("user@issues.vepo.dev"));
    }

    @Test
    @DisplayName("Should reject a revoked personal API token with 401")
    void shouldRejectRevokedPersonalApiToken() {
        var auth = Given.authenticatedUser();
        var created = given().header(auth)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "Revoked auth token"
                                   }
                                   """)
                             .when()
                             .post("/api/account/api-tokens")
                             .then()
                             .statusCode(201)
                             .extract();
        var secret = (String) created.path("token");
        long tokenId = ((Number) created.path("id")).longValue();

        given().header(auth)
               .when()
               .delete("/api/account/api-tokens/{id}", tokenId)
               .then()
               .statusCode(204);

        given().header(new Header("Authorization", "Bearer " + secret))
               .accept(ContentType.JSON)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(401);
    }

    @Test
    @DisplayName("Should reject an invalid personal API token with 401")
    void shouldRejectInvalidPersonalApiToken() {
        given().header(new Header("Authorization", "Bearer iss_pat_notarealtoken"))
               .accept(ContentType.JSON)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(401);
    }

    private static String createPersonalApiToken(Header auth, String name) {
        return given().header(auth)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "name": "%s"
                            }
                            """.formatted(name))
                      .when()
                      .post("/api/account/api-tokens")
                      .then()
                      .statusCode(201)
                      .extract()
                      .path("token");
    }

    private static long createPersonalApiTokenId(Header auth, String name) {
        return ((Number) given().header(auth)
                                .contentType(ContentType.JSON)
                                .body("""
                                      {
                                          "name": "%s"
                                      }
                                      """.formatted(name))
                                .when()
                                .post("/api/account/api-tokens")
                                .then()
                                .statusCode(201)
                                .extract()
                                .path("id")).longValue();
    }
}

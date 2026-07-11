package dev.vepo.issues.agent.setup;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class AgentSetupConfigEndpointTest {

    @Test
    @DisplayName("Should return Cursor setup config using public base URLs")
    void shouldReturnCursorSetupConfigUsingPublicBaseUrls() {
        var auth = Given.authenticatedUser();

        given().header(auth)
               .accept(ContentType.JSON)
               .when()
               .get("/api/agent/setup-config?preset=cursor")
               .then()
               .statusCode(200)
               .body("preset", is("cursor"))
               .body("issuesPublicBaseUrl", is("http://localhost:8080"))
               .body("mcpPublicBaseUrl", is("http://localhost:8082"))
               .body("mcpUrl", is("http://localhost:8082/mcp"))
               .body("issuesApiBaseUrl", is("http://localhost:8080/api"))
               .body("snippet", notNullValue())
               .body("snippet", containsString("http://localhost:8082/mcp"))
               .body("snippet", containsString("http://localhost:8080/api"));
    }

    @Test
    @DisplayName("Should reject setup config without JWT")
    void shouldRejectSetupConfigWithoutJwt() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/agent/setup-config?preset=cursor")
               .then()
               .statusCode(401);
    }
}

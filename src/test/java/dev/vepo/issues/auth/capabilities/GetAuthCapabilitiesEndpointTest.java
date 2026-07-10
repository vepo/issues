package dev.vepo.issues.auth.capabilities;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GetAuthCapabilitiesEndpointTest {

    @Test
    @DisplayName("Should report local provider with password recovery and change password enabled")
    void shouldReportLocalCapabilitiesByDefault() {
        given().when()
               .get("/api/auth/capabilities")
               .then()
               .statusCode(200)
               .body("provider", is("local"))
               .body("passwordRecovery", is(true))
               .body("changePassword", is(true));
    }
}

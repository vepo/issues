package dev.vepo.issues.auth.capabilities;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.auth.EndpointAuthTestProfile;
import dev.vepo.issues.auth.JwtTokenIssuer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(EndpointAuthTestProfile.class)
class EndpointAuthCapabilitiesAndGuardsTest {

    @Test
    @DisplayName("Should disable password recovery and change password for endpoint provider")
    void shouldReportEndpointCapabilitiesWithoutPasswordOps() {
        given().when()
               .get("/api/auth/capabilities")
               .then()
               .statusCode(200)
               .body("provider", is("endpoint"))
               .body("passwordRecovery", is(false))
               .body("changePassword", is(false));
    }

    @Test
    @DisplayName("Should reject change password when provider is not local")
    void shouldRejectChangePasswordWhenProviderIsNotLocal() {
        var user = Given.randomUser();
        var token = Given.transaction(() -> Given.inject(JwtTokenIssuer.class).issueAccessToken(user));

        given().header("Authorization", "Bearer " + token)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "currentPassword": "password",
                         "newPassword": "newSecret99"
                     }
                     """)
               .when()
               .post("/api/auth/change-password")
               .then()
               .statusCode(400)
               .body("message", is("Password operations are only available with local authentication"));
    }

    @Test
    @DisplayName("Should reject password recovery when provider is not local")
    void shouldRejectPasswordRecoveryWhenProviderIsNotLocal() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                       "credential": "user@issues.vepo.dev"
                     }
                     """)
               .when()
               .post("/api/auth/recovery")
               .then()
               .statusCode(400)
               .body("message", is("Password operations are only available with local authentication"));
    }
}

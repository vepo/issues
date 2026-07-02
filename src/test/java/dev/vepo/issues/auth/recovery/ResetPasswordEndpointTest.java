package dev.vepo.issues.auth.recovery;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ResetPasswordEndpointTest {

    @Test
    void shouldAcceptRecoveryRequestForUnknownCredential() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                       "credential": "unknown@issues.vepo.dev"
                     }
                     """)
               .when()
               .post("/api/auth/recovery")
               .then()
               .statusCode(200);
    }
}

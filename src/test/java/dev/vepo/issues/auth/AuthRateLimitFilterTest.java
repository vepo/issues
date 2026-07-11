package dev.vepo.issues.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import java.util.Map;

@QuarkusTest
@TestProfile(AuthRateLimitFilterTest.RateLimitEnabledProfile.class)
class AuthRateLimitFilterTest {

    public static class RateLimitEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("auth.rate-limit.enabled", "true",
                          "auth.rate-limit.requests", "3",
                          "auth.rate-limit.window-seconds", "60");
        }
    }

    @Test
    void shouldRejectExcessiveLoginAttemptsWithTooManyRequests() {
        var body = """
                   {
                     "email": "rate-limit@issues.vepo.dev",
                     "password": "wrong-password"
                   }
                   """;
        for (var i = 0; i < 3; i++) {
            given().contentType(ContentType.JSON)
                   .body(body)
                   .when()
                   .post("/api/auth/login")
                   .then()
                   .statusCode(anyOf(is(401), is(400)));
        }
        given().contentType(ContentType.JSON)
               .body(body)
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(429);
    }
}

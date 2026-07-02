package dev.vepo.issues.notifications.register;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class RegisterNotificationsEndpointTest {

    @Test
    void shouldRejectUnauthenticatedNotificationRegistration() {
        given().when()
               .get("/api/notifications/register")
               .then()
               .statusCode(401);
    }
}

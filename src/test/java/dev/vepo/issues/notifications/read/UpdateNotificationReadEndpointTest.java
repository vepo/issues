package dev.vepo.issues.notifications.read;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class UpdateNotificationReadEndpointTest {

    @Test
    void shouldRejectUnauthenticatedUpdateNotificationRead() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                         "read": true
                     }
                     """)
               .when()
               .post("/api/notifications/1/read")
               .then()
               .statusCode(401);
    }

    @Test
    void shouldReturnNotFoundForUnknownNotification() {
        given().header(Given.authenticatedUser())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "read": true
                     }
                     """)
               .when()
               .post("/api/notifications/999999/read")
               .then()
               .statusCode(404);
    }
}

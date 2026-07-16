package dev.vepo.issues.ticket.attachments;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class TicketAttachmentEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should upload list download and delete attachment")
    void shouldUploadListDownloadAndDeleteAttachment() {
        var auth = fixtures.userAuthenticatedHeader();
        var ticketId = fixtures.ticket().id();
        var content = "hello attachment".getBytes(StandardCharsets.UTF_8);

        var attachmentId = given().header(auth)
                                  .multiPart("file", "notes.txt", content, "text/plain")
                                  .when()
                                  .post("/api/tickets/{id}/attachments", ticketId)
                                  .then()
                                  .statusCode(201)
                                  .body("id", notNullValue())
                                  .body("originalFilename", equalTo("notes.txt"))
                                  .body("contentType", equalTo("text/plain"))
                                  .body("sizeBytes", equalTo(content.length))
                                  .body("uploadedBy.username", notNullValue())
                                  .extract()
                                  .path("id");

        given().header(auth)
               .when()
               .get("/api/tickets/{id}/attachments", ticketId)
               .then()
               .statusCode(200)
               .body("size()", equalTo(1))
               .body("originalFilename", hasItem("notes.txt"));

        given().header(auth)
               .when()
               .get("/api/tickets/{id}/attachments/{attachmentId}", ticketId, attachmentId)
               .then()
               .statusCode(200)
               .header("Content-Disposition", org.hamcrest.Matchers.containsString("notes.txt"))
               .body(equalTo("hello attachment"));

        given().header(auth)
               .when()
               .get("/api/tickets/{id}/history", ticketId)
               .then()
               .statusCode(200)
               .body("action", hasItem("ATTACHMENT_ADDED"));

        given().header(auth)
               .when()
               .delete("/api/tickets/{id}/attachments/{attachmentId}", ticketId, attachmentId)
               .then()
               .statusCode(204);

        given().header(auth)
               .when()
               .get("/api/tickets/{id}/attachments", ticketId)
               .then()
               .statusCode(200)
               .body("size()", equalTo(0));

        given().header(auth)
               .when()
               .get("/api/tickets/{id}/history", ticketId)
               .then()
               .statusCode(200)
               .body("action", hasItem("ATTACHMENT_REMOVED"));
    }

    @Test
    @DisplayName("Should reject disallowed content type and oversize file")
    void shouldRejectDisallowedTypeAndOversize() {
        var auth = fixtures.userAuthenticatedHeader();
        var ticketId = fixtures.ticket().id();

        given().header(auth)
               .multiPart("file", "evil.exe", new byte[] { 1, 2, 3 }, "application/octet-stream")
               .when()
               .post("/api/tickets/{id}/attachments", ticketId)
               .then()
               .statusCode(400);

        given().header(auth)
               .multiPart("file", "fake.png", new byte[] { 1, 2, 3 }, "application/pdf")
               .when()
               .post("/api/tickets/{id}/attachments", ticketId)
               .then()
               .statusCode(400);

        var oversized = new byte[(int) AttachmentContentRules.MAX_FILE_BYTES + 1];
        given().header(auth)
               .multiPart("file", "big.txt", oversized, "text/plain")
               .when()
               .post("/api/tickets/{id}/attachments", ticketId)
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should reject upload and delete on soft-deleted ticket; PM may list and download")
    void shouldHandleSoftDeletedTicketAttachments() {
        var userAuth = fixtures.userAuthenticatedHeader();
        var pmAuth = fixtures.pmAuthenticatedHeader();
        var ticketId = fixtures.ticket().id();
        var content = "keep me".getBytes(StandardCharsets.UTF_8);

        var attachmentId = given().header(userAuth)
                                  .multiPart("file", "keep.txt", content, "text/plain")
                                  .when()
                                  .post("/api/tickets/{id}/attachments", ticketId)
                                  .then()
                                  .statusCode(201)
                                  .extract()
                                  .path("id");

        given().header(pmAuth)
               .when()
               .delete("/api/tickets/{id}", ticketId)
               .then()
               .statusCode(204);

        given().header(userAuth)
               .multiPart("file", "more.txt", content, "text/plain")
               .when()
               .post("/api/tickets/{id}/attachments", ticketId)
               .then()
               .statusCode(404);

        given().header(userAuth)
               .when()
               .delete("/api/tickets/{id}/attachments/{attachmentId}", ticketId, attachmentId)
               .then()
               .statusCode(404);

        given().header(userAuth)
               .when()
               .get("/api/tickets/{id}/attachments", ticketId)
               .then()
               .statusCode(404);

        given().header(pmAuth)
               .when()
               .get("/api/tickets/{id}/attachments", ticketId)
               .then()
               .statusCode(200)
               .body("size()", equalTo(1));

        given().header(pmAuth)
               .when()
               .get("/api/tickets/{id}/attachments/{attachmentId}", ticketId, attachmentId)
               .then()
               .statusCode(200)
               .body(equalTo("keep me"));
    }

    @Test
    @DisplayName("Should set via_agent on attachment history when using personal API token")
    void shouldSetViaAgentOnAttachmentHistoryWithPersonalApiToken() {
        var auth = fixtures.userAuthenticatedHeader();
        var secret = createPersonalApiToken(auth, "attachment-via-agent");
        var ticketId = fixtures.ticket().id();

        given().header(new Header("Authorization", "Bearer " + secret))
               .multiPart("file", "agent.txt", "via agent".getBytes(StandardCharsets.UTF_8), "text/plain")
               .when()
               .post("/api/tickets/{id}/attachments", ticketId)
               .then()
               .statusCode(201);

        given().header(auth)
               .when()
               .get("/api/tickets/{id}/history", ticketId)
               .then()
               .statusCode(200)
               .body("find { it.action == 'ATTACHMENT_ADDED' }.viaAgent", is(true))
               .body("find { it.action == 'ATTACHMENT_ADDED' }.newValue", equalTo("agent.txt"));
    }

    private static String createPersonalApiToken(Header auth, String name) {
        return given().header(auth)
                      .contentType(ContentType.JSON)
                      .body("{\"name\":\"%s\"}".formatted(name))
                      .when()
                      .post("/api/account/api-tokens")
                      .then()
                      .statusCode(201)
                      .body("token", notNullValue())
                      .extract()
                      .path("token");
    }
}

package dev.vepo.issues.ticket.csvimport;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ChunkedTicketImportUploadEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should reject init when totalBytes exceeds 5 MB")
    void shouldRejectInitWhenTotalBytesExceedsFiveMegabytes() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "fileName": "big.csv",
                       "totalBytes": %d,
                       "chunkCount": 6
                     }
                     """.formatted(CsvImportParser.MAX_FILE_BYTES + 1L))
               .when()
               .post("/api/projects/%d/tickets/import/upload/init".formatted(fixtures.project().id()))
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should reject part larger than 1 MB")
    void shouldRejectPartLargerThanOneMegabyte() {
        var csv = "Title,Description,Category\nImported ticket title,Imported ticket description,%s\n"
                                                                                                      .formatted(fixtures.bug().getName())
                                                                                                      .getBytes(StandardCharsets.UTF_8);
        var importId = initUpload(csv.length, 1, "tickets.csv");
        var oversized = new byte[CsvImportParser.MAX_CHUNK_BYTES + 1];
        Arrays.fill(oversized, (byte) 'a');

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType("application/octet-stream")
               .body(oversized)
               .when()
               .put("/api/projects/%d/tickets/import/%d/upload/parts/0".formatted(fixtures.project().id(), importId))
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should parse CSV after all parts are uploaded and completed")
    void shouldParseCsvAfterAllPartsAreUploadedAndCompleted() {
        var csv = """
                  Title,Description,Category
                  Imported ticket title,Imported ticket description,%s
                  """.formatted(fixtures.bug().getName())
                     .getBytes(StandardCharsets.UTF_8);
        var mid = csv.length / 2;
        var part0 = Arrays.copyOfRange(csv, 0, mid);
        var part1 = Arrays.copyOfRange(csv, mid, csv.length);

        var importId = initUpload(csv.length, 2, "tickets.csv");
        uploadPart(importId, 0, part0);
        uploadPart(importId, 1, part1);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .post("/api/projects/%d/tickets/import/%d/upload/complete".formatted(fixtures.project().id(), importId))
               .then()
               .statusCode(200)
               .body("id", equalTo((int) importId))
               .body("rowCount", equalTo(1))
               .body("headers", hasItem("Title"))
               .body("projectScoped", equalTo(true));
    }

    @Test
    @DisplayName("Should reject complete when a part is missing")
    void shouldRejectCompleteWhenPartIsMissing() {
        var csv = """
                  Title,Description,Category
                  Imported ticket title,Imported ticket description,%s
                  """.formatted(fixtures.bug().getName())
                     .getBytes(StandardCharsets.UTF_8);
        var mid = csv.length / 2;
        var part0 = Arrays.copyOfRange(csv, 0, mid);

        var importId = initUpload(csv.length, 2, "tickets.csv");
        uploadPart(importId, 0, part0);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .post("/api/projects/%d/tickets/import/%d/upload/complete".formatted(fixtures.project().id(), importId))
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should complete global chunked upload")
    void shouldCompleteGlobalChunkedUpload() {
        var csv = """
                  Title,Description,Category,Project
                  Global chunk title,Global chunk description,%s,%s
                  """.formatted(fixtures.bug().getName(), fixtures.project().name())
                     .getBytes(StandardCharsets.UTF_8);

        var importId = given().header(fixtures.pmAuthenticatedHeader())
                              .contentType(ContentType.JSON)
                              .body("""
                                    {
                                      "fileName": "global.csv",
                                      "totalBytes": %d,
                                      "chunkCount": 1
                                    }
                                    """.formatted(csv.length))
                              .when()
                              .post("/api/tickets/import/upload/init")
                              .then()
                              .statusCode(200)
                              .body("importId", greaterThan(0))
                              .extract()
                              .jsonPath()
                              .getLong("importId");

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType("application/octet-stream")
               .body(csv)
               .when()
               .put("/api/tickets/import/%d/upload/parts/0".formatted(importId))
               .then()
               .statusCode(204);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .post("/api/tickets/import/%d/upload/complete".formatted(importId))
               .then()
               .statusCode(200)
               .body("id", equalTo((int) importId))
               .body("rowCount", equalTo(1))
               .body("projectScoped", equalTo(false));
    }

    private long initUpload(long totalBytes, int chunkCount, String fileName) {
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                              "fileName": "%s",
                              "totalBytes": %d,
                              "chunkCount": %d
                            }
                            """.formatted(fileName, totalBytes, chunkCount))
                      .when()
                      .post("/api/projects/%d/tickets/import/upload/init".formatted(fixtures.project().id()))
                      .then()
                      .statusCode(200)
                      .extract()
                      .jsonPath()
                      .getLong("importId");
    }

    private void uploadPart(long importId, int partIndex, byte[] content) {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType("application/octet-stream")
               .body(content)
               .when()
               .put("/api/projects/%d/tickets/import/%d/upload/parts/%d".formatted(fixtures.project().id(), importId, partIndex))
               .then()
               .statusCode(204);
    }
}

package dev.vepo.issues.ticket.csvimport;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class PreviewTicketImportEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should return validation errors for unknown category on preview")
    void shouldReturnValidationErrorsForUnknownCategory() throws IOException {
        var importId = uploadCsv("""
                                 Title,Description,Category
                                 Valid title here,Valid description text,Missing%s
                                 """.formatted(UUID.randomUUID()));

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType("application/json")
               .body("""
                     {
                       "mapping": {
                         "titleColumn": "Title",
                         "descriptionColumn": "Description",
                         "categoryColumn": "Category"
                       }
                     }
                     """)
               .when()
               .put("/api/projects/%d/tickets/import/%d/mapping".formatted(fixtures.project().id(), importId))
               .then()
               .statusCode(204);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .post("/api/projects/%d/tickets/import/%d/preview".formatted(fixtures.project().id(), importId))
               .then()
               .statusCode(200)
               .body("invalidCount", equalTo(1))
               .body("validCount", equalTo(0))
               .body("rows[0].valid", equalTo(false));
    }

    @Test
    @DisplayName("Should reject preview without authentication")
    void shouldRejectPreviewWithoutAuthentication() {
        given().accept(ContentType.JSON)
               .when()
               .post("/api/projects/%d/tickets/import/1/preview".formatted(fixtures.project().id()))
               .then()
               .statusCode(401);
    }

    private long uploadCsv(String csv) throws IOException {
        var file = Files.createTempFile("import-", ".csv");
        Files.writeString(file, csv, StandardCharsets.UTF_8);
        return given().header(fixtures.pmAuthenticatedHeader())
                      .header("X-File-Name", "tickets.csv")
                      .contentType("application/octet-stream")
                      .body(Files.readAllBytes(file))
                      .when()
                      .post("/api/projects/%d/tickets/import/upload".formatted(fixtures.project().id()))
                      .then()
                      .statusCode(200)
                      .extract()
                      .jsonPath()
                      .getLong("id");
    }
}

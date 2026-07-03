package dev.vepo.issues.ticket.csvimport;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class GlobalTicketImportEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should upload global CSV import without project id")
    void shouldUploadGlobalCsvImport() throws IOException {
        given().header(fixtures.pmAuthenticatedHeader())
               .header("X-File-Name", "tickets.csv")
               .contentType("application/octet-stream")
               .body(Files.readAllBytes(csvFile("""
                                                Project,Title,Description,Category
                                                %s,Imported title,Imported description,%s
                                                """.formatted(fixtures.project().name(), fixtures.bug().getName()))))
               .when()
               .post("/api/tickets/import/upload")
               .then()
               .statusCode(200)
               .body("projectScoped", equalTo(false))
               .body("rowCount", equalTo(1));
    }

    @Test
    @DisplayName("Should import tickets using project column on global import")
    void shouldImportTicketsUsingProjectColumn() throws IOException {
        var importId = given().header(fixtures.pmAuthenticatedHeader())
                              .header("X-File-Name", "tickets.csv")
                              .contentType("application/octet-stream")
                              .body(Files.readAllBytes(csvFile("""
                                                               Project,Title,Description,Category
                                                               %s,Global endpoint title,Global endpoint description,%s
                                                               """.formatted(fixtures.project().name(), fixtures.bug().getName()))))
                              .when()
                              .post("/api/tickets/import/upload")
                              .then()
                              .statusCode(200)
                              .extract()
                              .jsonPath()
                              .getLong("id");

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "mapping": {
                         "titleColumn": "Title",
                         "descriptionColumn": "Description",
                         "categoryColumn": "Category",
                         "projectColumn": "Project"
                       }
                     }
                     """)
               .when()
               .put("/api/tickets/import/%d/mapping".formatted(importId))
               .then()
               .statusCode(204);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .post("/api/tickets/import/%d/execute".formatted(importId))
               .then()
               .statusCode(200)
               .body("created", hasSize(1))
               .body("errors", hasSize(0))
               .body("created[0].title", equalTo("Global endpoint title"));
    }

    private java.nio.file.Path csvFile(String csv) throws IOException {
        var file = Files.createTempFile("global-import-", ".csv");
        Files.writeString(file, csv, StandardCharsets.UTF_8);
        return file;
    }
}

package dev.vepo.issues.ticket.csvimport;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ImportTicketsEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should import valid tickets and report invalid rows")
    void shouldImportValidTicketsAndReportInvalidRows() throws IOException {
        var importId = uploadCsv("""
                                 Title,Description,Category
                                 Imported ticket title,Imported ticket description,%s
                                 Another imported title,Another imported description,Missing%s
                                 """.formatted(fixtures.bug().getName(), UUID.randomUUID()));

        applyDefaultMapping(importId);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .post("/api/projects/%d/tickets/import/%d/execute".formatted(fixtures.project().id(), importId))
               .then()
               .statusCode(200)
               .body("created", hasSize(1))
               .body("errors", hasSize(1))
               .body("created[0].title", equalTo("Imported ticket title"))
               .body("errors[0].rowNumber", equalTo(3))
               .body("summary.importedCount", equalTo(1))
               .body("summary.projectsImpacted", equalTo(1))
               .body("summary.ticketsByProject", hasSize(1))
               .body("summary.ticketsByStatus", hasSize(1));
    }

    @Test
    @DisplayName("Should import ticket with assignee and status when transition is valid")
    void shouldImportTicketWithAssigneeAndStatus() throws IOException {
        var importId = uploadCsv("""
                                 Title,Description,Category,Assignee,Status
                                 Assigned ticket title,Assigned ticket description,%s,user@issues.vepo.dev,In Progress
                                 """.formatted(fixtures.bug().getName()));

        applyDefaultMapping(importId);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .post("/api/projects/%d/tickets/import/%d/execute".formatted(fixtures.project().id(), importId))
               .then()
               .statusCode(200)
               .body("created", hasSize(1))
               .body("errors", hasSize(0))
               .body("created[0].assignee", equalTo((int) dev.vepo.issues.Given.userIdByEmail("user@issues.vepo.dev")));
    }

    @Test
    @DisplayName("Should report assignee who is not a project member as import error")
    void shouldRejectAssigneeWhoIsNotProjectMember() throws IOException {
        Given.user("outsider-import@issues.vepo.dev");
        var importId = uploadCsv("""
                                 Title,Description,Category,Assignee
                                 Outsider ticket title,Outsider ticket description,%s,outsider-import@issues.vepo.dev
                                 """.formatted(fixtures.bug().getName()));

        applyDefaultMapping(importId);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .post("/api/projects/%d/tickets/import/%d/execute".formatted(fixtures.project().id(), importId))
               .then()
               .statusCode(200)
               .body("created", hasSize(0))
               .body("errors", hasSize(1))
               .body("errors[0].message", equalTo("Assignee must be a member of the project"));
    }

    @Test
    @DisplayName("Should reject import without authentication")
    void shouldRejectImportWithoutAuthentication() {
        given().accept(ContentType.JSON)
               .when()
               .post("/api/projects/%d/tickets/import/1/execute".formatted(fixtures.project().id()))
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

    private void applyDefaultMapping(long importId) {
        var mapping = """
                      {
                        "mapping": {
                          "titleColumn": "Title",
                          "descriptionColumn": "Description",
                          "categoryColumn": "Category",
                          "priorityColumn": "Priority",
                          "assigneeEmailColumn": "Assignee",
                          "statusColumn": "Status"
                        }
                      }
                      """;
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body(mapping)
               .when()
               .put("/api/projects/%d/tickets/import/%d/mapping".formatted(fixtures.project().id(), importId))
               .then()
               .statusCode(204);
    }
}

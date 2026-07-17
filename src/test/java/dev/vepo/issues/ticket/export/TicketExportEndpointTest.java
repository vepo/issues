package dev.vepo.issues.ticket.export;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.TicketTestFixtures;
import dev.vepo.issues.ticket.search.saved.SavedQueryResponse;
import dev.vepo.issues.user.User;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class TicketExportEndpointTest {

    private static final String EXPORT_PATH = "/api/tickets/export";

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    void shouldAllowUserProjectManagerAndAdminToExportTicketsAndDenyUnauthenticatedCaller() {
        var request = simpleRequest(ExportFormat.JSON, fixtures.ticket().title());

        assertJsonExportAllowed(fixtures.userAuthenticatedHeader(), request);
        assertJsonExportAllowed(fixtures.pmAuthenticatedHeader(), request);
        assertJsonExportAllowed(Given.authenticatedAdmin(), request);

        given().contentType(ContentType.JSON)
               .body(request)
               .when()
               .post(EXPORT_PATH)
               .then()
               .statusCode(401);
    }

    @Test
    void shouldReturnCsvAttachmentWithStableBodyContentTypeAndDatedFilename() {
        var response = given().header(fixtures.userAuthenticatedHeader())
                              .contentType(ContentType.JSON)
                              .body(simpleRequest(ExportFormat.CSV, fixtures.ticket().title()))
                              .when()
                              .post(EXPORT_PATH)
                              .then()
                              .statusCode(200)
                              .contentType("text/csv")
                              .extract()
                              .response();

        assertThat(response.asString()).contains("identifier", "title", fixtures.ticket().identifier());
        assertAttachmentFilename(response.header("Content-Disposition"), "csv");
    }

    @Test
    void shouldReturnJsonAttachmentWithVersionedBodyContentTypeAndDatedFilename() {
        var response = given().header(fixtures.userAuthenticatedHeader())
                              .contentType(ContentType.JSON)
                              .body(simpleRequest(ExportFormat.JSON, fixtures.ticket().title()))
                              .when()
                              .post(EXPORT_PATH)
                              .then()
                              .statusCode(200)
                              .contentType(ContentType.JSON)
                              .body("schemaVersion", equalTo(1))
                              .body("source", equalTo("SIMPLE_SEARCH"))
                              .body("count", equalTo(1))
                              .body("tickets", hasSize(1))
                              .body("tickets[0].identifier", equalTo(fixtures.ticket().identifier()))
                              .extract()
                              .response();

        assertAttachmentFilename(response.header("Content-Disposition"), "json");
    }

    @Test
    void shouldExportCurrentSimpleAdvancedAndSavedQueryResults() {
        var queryToken = "export-source-%s".formatted(UUID.randomUUID());
        var expectedTicket = createTicket(fixtures.project().id(), queryToken);
        var savedQuery = createSavedQuery(queryToken);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body(simpleRequest(ExportFormat.JSON, queryToken))
               .when()
               .post(EXPORT_PATH)
               .then()
               .statusCode(200)
               .body("source", equalTo("SIMPLE_SEARCH"))
               .body("tickets.identifier", equalTo(java.util.List.of(expectedTicket.identifier())));

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body(advancedRequest(ExportFormat.JSON, "title ~ \"%s\"".formatted(queryToken)))
               .when()
               .post(EXPORT_PATH)
               .then()
               .statusCode(200)
               .body("source", equalTo("ADVANCED_QUERY"))
               .body("tickets.identifier", equalTo(java.util.List.of(expectedTicket.identifier())));

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body(savedRequest(ExportFormat.JSON, savedQuery.slug()))
               .when()
               .post(EXPORT_PATH)
               .then()
               .statusCode(200)
               .body("source", equalTo("SAVED_QUERY"))
               .body("tickets.identifier", equalTo(java.util.List.of(expectedTicket.identifier())));
    }

    @Test
    void shouldReturnValidEmptyCsvAndJsonFilesWhenNoTicketsMatch() {
        var absentTerm = "absent-%s".formatted(UUID.randomUUID());

        var csv = given().header(fixtures.userAuthenticatedHeader())
                         .contentType(ContentType.JSON)
                         .body(simpleRequest(ExportFormat.CSV, absentTerm))
                         .when()
                         .post(EXPORT_PATH)
                         .then()
                         .statusCode(200)
                         .contentType("text/csv")
                         .extract()
                         .asString();
        assertThat(csv.lines()).containsExactly("identifier,title,description,projectKey,projectName,statusCode,statusName,categoryId,categoryName,priority,type,authorEmail,authorName,assigneeEmail,assigneeName,phaseId,phaseName,observedVersionId,observedVersionName,targetVersionId,targetVersionName,storyPoints,dueDate,createdAt,updatedAt");

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body(simpleRequest(ExportFormat.JSON, absentTerm))
               .when()
               .post(EXPORT_PATH)
               .then()
               .statusCode(200)
               .contentType(ContentType.JSON)
               .body("count", equalTo(0))
               .body("tickets", hasSize(0));
    }

    @Test
    void shouldRejectMixedAndIncompleteExportRequests() {
        var mixed = new ExportTicketsRequest(ExportFormat.CSV,
                                             ExportSource.SIMPLE_SEARCH,
                                             "release",
                                             null,
                                             "priority = HIGH",
                                             null);
        var incomplete = new ExportTicketsRequest(ExportFormat.JSON,
                                                  ExportSource.SAVED_QUERY,
                                                  null,
                                                  null,
                                                  null,
                                                  null);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body(mixed)
               .when()
               .post(EXPORT_PATH)
               .then()
               .statusCode(400);
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body(incomplete)
               .when()
               .post(EXPORT_PATH)
               .then()
               .statusCode(400);
    }

    @Test
    void shouldReturnPayloadTooLargeWhenMoreThanTenThousandTicketsMatch() {
        QuarkusMock.installMockForType(new OverflowTicketExportService(), TicketExportService.class);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body(simpleRequest(ExportFormat.CSV, null))
               .when()
               .post(EXPORT_PATH)
               .then()
               .statusCode(413)
               .body(containsString("10,000"));
    }

    @Test
    void shouldExportOnlyTicketsFromProjectsReadableByCaller() {
        var queryToken = "export-visibility-%s".formatted(UUID.randomUUID());
        var readableProject = createProject("PRIVATE");
        Given.addProjectMember(readableProject.id(), "user@issues.vepo.dev");
        var readableTicket = createTicket(readableProject.id(), queryToken);
        var hiddenTicket = createTicket(createProject("PRIVATE").id(), queryToken);

        var responseBody = given().header(fixtures.userAuthenticatedHeader())
                                  .contentType(ContentType.JSON)
                                  .body(advancedRequest(ExportFormat.JSON, "title ~ \"%s\"".formatted(queryToken)))
                                  .when()
                                  .post(EXPORT_PATH)
                                  .then()
                                  .statusCode(200)
                                  .extract()
                                  .asString();

        assertThat(responseBody).contains(readableTicket.identifier()).doesNotContain(hiddenTicket.identifier());
    }

    private static void assertJsonExportAllowed(Header authentication, ExportTicketsRequest request) {
        given().header(authentication)
               .contentType(ContentType.JSON)
               .body(request)
               .when()
               .post(EXPORT_PATH)
               .then()
               .statusCode(200)
               .contentType(ContentType.JSON);
    }

    private static void assertAttachmentFilename(String contentDisposition, String extension) {
        assertThat(contentDisposition)
                                      .startsWith("attachment;")
                                      .containsPattern("filename=\"tickets-\\d{4}-\\d{2}-\\d{2}\\.%s\"".formatted(extension))
                                      .containsPattern("filename\\*=UTF-8''tickets-\\d{4}-\\d{2}-\\d{2}\\.%s".formatted(extension));
    }

    private SavedQueryResponse createSavedQuery(String queryToken) {
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "name": "Export source %s",
                                "query": "title ~ \\"%s\\"",
                                "showAtHome": false
                            }""".formatted(UUID.randomUUID(), queryToken))
                      .when()
                      .post("/api/saved-queries")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(SavedQueryResponse.class);
    }

    private ProjectResponse createProject(String securityLevel) {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "name": "Export visibility %s",
                                "description": "Ticket export visibility project.",
                                "prefix": "E%s",
                                "workflowId": %d,
                                "securityLevel": "%s"
                            }""".formatted(suffix,
                                           suffix.substring(0, 3).toUpperCase(),
                                           Given.simpleWorkflow().id(),
                                           securityLevel))
                      .when()
                      .post("/api/projects")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(ProjectResponse.class);
    }

    private TicketResponse createTicket(long projectId, String title) {
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "title": "%s",
                                "description": "Ticket export endpoint test.",
                                "projectId": %d,
                                "categoryId": %d
                            }""".formatted(title, projectId, fixtures.bug().getId()))
                      .when()
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }

    private static ExportTicketsRequest simpleRequest(ExportFormat format, String term) {
        return new ExportTicketsRequest(format, ExportSource.SIMPLE_SEARCH, term, null, null, null);
    }

    private static ExportTicketsRequest advancedRequest(ExportFormat format, String query) {
        return new ExportTicketsRequest(format, ExportSource.ADVANCED_QUERY, null, null, query, null);
    }

    private static ExportTicketsRequest savedRequest(ExportFormat format, String savedQuerySlug) {
        return new ExportTicketsRequest(format, ExportSource.SAVED_QUERY, null, null, null, savedQuerySlug);
    }

    private static final class OverflowTicketExportService extends TicketExportService {
        private OverflowTicketExportService() {
            super(null, null, null);
        }

        @Override
        public java.util.List<TicketExportRow> prepare(ExportTicketsRequest request, User requestingUser) {
            throw new TicketExportLimitExceededException();
        }
    }
}

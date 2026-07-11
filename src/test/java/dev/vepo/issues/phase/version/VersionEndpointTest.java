package dev.vepo.issues.phase.version;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class VersionEndpointTest {

    private ProjectResponse project;
    private Header userHeader;
    private Header pmHeader;
    private TicketTestFixtures ticketFixtures;

    @BeforeEach
    void setup() {
        project = Given.simpleProject();
        userHeader = Given.authenticatedUser();
        pmHeader = Given.authenticatedProjectManager();
        ticketFixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Project manager can create a SemVer version")
    void shouldCreateVersionWhenLabelIsValidSemVer() {
        given().header(pmHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "label": "1.0.0",
                         "description": "Initial release"
                     }""")
               .post("/api/projects/%d/versions".formatted(project.id()))
               .then()
               .statusCode(201)
               .body("label", is("1.0.0"))
               .body("description", is("Initial release"))
               .body("projectId", is((int) project.id()));
    }

    @Test
    @DisplayName("Regular user cannot create a version")
    void shouldRejectVersionCreateForRegularUser() {
        given().header(userHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "label": "1.0.0",
                         "description": "Initial release"
                     }""")
               .post("/api/projects/%d/versions".formatted(project.id()))
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("Invalid SemVer label is rejected")
    void shouldRejectInvalidSemVerLabel() {
        given().header(pmHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "label": "v1",
                         "description": "Bad label"
                     }""")
               .post("/api/projects/%d/versions".formatted(project.id()))
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Duplicate version label per project is rejected")
    void shouldRejectDuplicateVersionLabel() {
        var path = "/api/projects/%d/versions".formatted(project.id());
        var body = """
                   {
                       "label": "2.0.0",
                       "description": "Release"
                   }""";
        given().header(pmHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body(body)
               .post(path)
               .then()
               .statusCode(201);

        given().header(pmHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body(body)
               .post(path)
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Authenticated user can list project versions")
    void shouldListVersionsByProject() {
        given().header(pmHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "label": "3.0.0",
                         "description": "Third"
                     }""")
               .post("/api/projects/%d/versions".formatted(project.id()))
               .then()
               .statusCode(201);

        given().header(userHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/versions".formatted(project.id()))
               .then()
               .statusCode(200)
               .body("label", hasItem("3.0.0"));
    }

    @Test
    @DisplayName("Version changelog groups tickets by target and observed association")
    void shouldReturnGroupedChangelogExcludingCanceled() {
        var targetVersionId = given().header(pmHeader)
                                     .accept(ContentType.JSON)
                                     .contentType(ContentType.JSON)
                                     .body("""
                                           {
                                               "label": "4.0.0",
                                               "description": "Planned release"
                                           }""")
                                     .post("/api/projects/%d/versions".formatted(ticketFixtures.project().id()))
                                     .then()
                                     .statusCode(201)
                                     .extract()
                                     .path("id");

        var observedVersionId = given().header(pmHeader)
                                       .accept(ContentType.JSON)
                                       .contentType(ContentType.JSON)
                                       .body("""
                                             {
                                                 "label": "4.1.0",
                                                 "description": "Shipped release"
                                             }""")
                                       .post("/api/projects/%d/versions".formatted(ticketFixtures.project().id()))
                                       .then()
                                       .statusCode(201)
                                       .extract()
                                       .path("id");

        var ticketId = ticketFixtures.ticket().id();
        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Versioned ticket",
                         "description": "Ticket with version fields",
                         "categoryId": %d,
                         "priority": "MEDIUM",
                         "planningFields": {
                             "observedVersionId": %d,
                             "targetVersionId": %d
                         }
                     }""".formatted(ticketFixtures.feature().getId(), observedVersionId, targetVersionId))
               .post("/api/tickets/" + ticketId)
               .then()
               .statusCode(200)
               .body("targetVersionLabel", is("4.0.0"))
               .body("observedVersionLabel", is("4.1.0"));

        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/versions/%d/changelog".formatted(ticketFixtures.project().id(), targetVersionId))
               .then()
               .statusCode(200)
               .body("label", is("4.0.0"))
               .body("sections.find { it.name == 'Planejado' }.tickets", hasSize(1))
               .body("sections.find { it.name == 'Planejado' }.tickets[0].identifier", is(ticketFixtures.ticket().identifier()))
               .body("sections.find { it.name == 'Entregue' }.tickets", hasSize(0))
               .body("sections.find { it.name == 'Via fase' }.tickets", hasSize(0));

        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/versions/%d/changelog".formatted(ticketFixtures.project().id(), observedVersionId))
               .then()
               .statusCode(200)
               .body("sections.find { it.name == 'Entregue' }.tickets", hasSize(1))
               .body("sections.find { it.name == 'Planejado' }.tickets", hasSize(0));
    }

    @Test
    @DisplayName("Ticket update preserves versions when planningFields is omitted")
    void shouldPreserveVersionsWhenVersionFieldsOmitted() {
        var versionId = given().header(pmHeader)
                               .accept(ContentType.JSON)
                               .contentType(ContentType.JSON)
                               .body("""
                                     {
                                         "label": "5.0.0",
                                         "description": "Keep me"
                                     }""")
                               .post("/api/projects/%d/versions".formatted(ticketFixtures.project().id()))
                               .then()
                               .statusCode(201)
                               .extract()
                               .path("id");

        var ticketId = ticketFixtures.ticket().id();
        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "With target version",
                         "description": "Ticket with target version set",
                         "categoryId": %d,
                         "priority": "MEDIUM",
                         "planningFields": {
                             "targetVersionId": %d
                         }
                     }""".formatted(ticketFixtures.feature().getId(), versionId))
               .post("/api/tickets/" + ticketId)
               .then()
               .statusCode(200)
               .body("targetVersionLabel", is("5.0.0"));

        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Updated title only",
                         "description": "No version change",
                         "categoryId": %d,
                         "priority": "MEDIUM"
                     }""".formatted(ticketFixtures.feature().getId()))
               .post("/api/tickets/" + ticketId)
               .then()
               .statusCode(200)
               .body("title", equalTo("Updated title only"))
               .body("targetVersionLabel", is("5.0.0"));
    }

    @Test
    @DisplayName("Project manager can update version label and description")
    void shouldUpdateVersion() {
        var versionId = given().header(pmHeader)
                               .accept(ContentType.JSON)
                               .contentType(ContentType.JSON)
                               .body("""
                                     {
                                         "label": "6.0.0",
                                         "description": "Before"
                                     }""")
                               .post("/api/projects/%d/versions".formatted(project.id()))
                               .then()
                               .statusCode(201)
                               .extract()
                               .path("id");

        given().header(pmHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "label": "6.1.0",
                         "description": "After"
                     }""")
               .post("/api/projects/%d/versions/%d".formatted(project.id(), versionId))
               .then()
               .statusCode(200)
               .body("label", is("6.1.0"))
               .body("description", is("After"));

        given().header(userHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/versions/%d".formatted(project.id(), versionId))
               .then()
               .statusCode(200)
               .body("label", is("6.1.0"));
    }

    @Test
    @DisplayName("Clearing ticket version fields removes association")
    void shouldClearVersionFieldsOnTicketUpdate() {
        var versionId = given().header(pmHeader)
                               .accept(ContentType.JSON)
                               .contentType(ContentType.JSON)
                               .body("""
                                     {
                                         "label": "7.0.0",
                                         "description": "Temporary"
                                     }""")
                               .post("/api/projects/%d/versions".formatted(ticketFixtures.project().id()))
                               .then()
                               .statusCode(201)
                               .extract()
                               .path("id");

        var ticketId = ticketFixtures.ticket().id();
        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Clear versions",
                         "description": "Clear version fields",
                         "categoryId": %d,
                         "priority": "MEDIUM",
                         "planningFields": {
                             "targetVersionId": %d
                         }
                     }""".formatted(ticketFixtures.feature().getId(), versionId))
               .post("/api/tickets/" + ticketId)
               .then()
               .statusCode(200)
               .body("targetVersionLabel", is("7.0.0"));

        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Clear versions",
                         "description": "Clear version fields",
                         "categoryId": %d,
                         "priority": "MEDIUM",
                         "planningFields": {
                             "targetVersionId": null,
                             "observedVersionId": null
                         }
                     }""".formatted(ticketFixtures.feature().getId()))
               .post("/api/tickets/" + ticketId)
               .then()
               .statusCode(200)
               .body("targetVersionId", nullValue())
               .body("targetVersionLabel", nullValue());
    }

    @Test
    @DisplayName("Should reject version list for non-member")
    void shouldForbidNonMemberOnForeignProjectVersionList() {
        var foreignProject = given().header(pmHeader)
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Foreign Version %s",
                                              "description": "No membership for user",
                                              "prefix": "VR%s",
                                              "workflowId": %d
                                          }
                                          """.formatted(UUID.randomUUID(),
                                                        UUID.randomUUID().toString().substring(0, 4).toUpperCase(),
                                                        Given.simpleWorkflow().id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(ProjectResponse.class);

        given().header(userHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/versions".formatted(foreignProject.id()))
               .then()
               .statusCode(403);
    }
}

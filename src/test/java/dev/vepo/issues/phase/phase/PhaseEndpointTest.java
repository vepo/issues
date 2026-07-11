package dev.vepo.issues.phase.phase;

import static io.restassured.RestAssured.given;
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
class PhaseEndpointTest {

    private ProjectResponse project;
    private Header pmHeader;
    private TicketTestFixtures ticketFixtures;

    @BeforeEach
    void setup() {
        project = Given.simpleProject();
        pmHeader = Given.authenticatedProjectManager();
        ticketFixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("New phase copies objective and deliverables from project template when omitted")
    void shouldCopyPhaseTemplateOnCreate() {
        given().header(pmHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "%s",
                         "description": "%s",
                         "prefix": "%s",
                         "workflowId": %d,
                         "ticketTemplate": { "enabled": false },
                         "phaseTemplate": {
                             "objective": "Objetivo padrão da fase",
                             "deliverables": ["Entregável A", "Entregável B"]
                         }
                     }""".formatted(project.name(), project.description(), project.prefix(), project.workflow().id()))
               .post("/api/projects/" + project.id())
               .then()
               .statusCode(201)
               .body("phaseTemplate.objective", is("Objetivo padrão da fase"))
               .body("phaseTemplate.deliverables", org.hamcrest.Matchers.hasSize(2));

        given().header(pmHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Fase com template"
                     }""")
               .post("/api/projects/%d/phases".formatted(project.id()))
               .then()
               .statusCode(201)
               .body("objective", is("Objetivo padrão da fase"))
               .body("deliverables", org.hamcrest.Matchers.hasSize(2))
               .body("deliverables[0].text", is("Entregável A"));
    }

    @Test
    @DisplayName("Project manager can create and list phases")
    void shouldCreateAndListPhases() {
        given().header(pmHeader)
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Fase Alpha",
                         "objective": "Entregar MVP",
                         "deliverables": ["API REST", "UI Angular"]
                     }""")
               .post("/api/projects/%d/phases".formatted(project.id()))
               .then()
               .statusCode(201)
               .body("name", is("Fase Alpha"))
               .body("status", is("PLANNED"))
               .body("deliverables", notNullValue());

        given().header(Given.authenticatedUser())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/phases".formatted(project.id()))
               .then()
               .statusCode(200)
               .body("name", org.hamcrest.Matchers.hasItem("Fase Alpha"));
    }

    @Test
    @DisplayName("Activating a phase completes the previous active phase")
    void shouldCompletePreviousActivePhaseWhenActivatingNewOne() {
        var firstId = given().header(pmHeader)
                             .accept(ContentType.JSON)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "Fase 1",
                                       "objective": "Primeira"
                                   }""")
                             .post("/api/projects/%d/phases".formatted(project.id()))
                             .then()
                             .statusCode(201)
                             .extract()
                             .path("id");

        given().header(pmHeader)
               .accept(ContentType.JSON)
               .when()
               .post("/api/projects/%d/phases/%d/activate".formatted(project.id(), firstId))
               .then()
               .statusCode(200)
               .body("status", is("ACTIVE"));

        var secondId = given().header(pmHeader)
                              .accept(ContentType.JSON)
                              .contentType(ContentType.JSON)
                              .body("""
                                    {
                                        "name": "Fase 2",
                                        "objective": "Segunda"
                                    }""")
                              .post("/api/projects/%d/phases".formatted(project.id()))
                              .then()
                              .statusCode(201)
                              .extract()
                              .path("id");

        given().header(pmHeader)
               .accept(ContentType.JSON)
               .when()
               .post("/api/projects/%d/phases/%d/activate".formatted(project.id(), secondId))
               .then()
               .statusCode(200)
               .body("status", is("ACTIVE"));

        given().header(pmHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/phases/%d".formatted(project.id(), firstId))
               .then()
               .statusCode(200)
               .body("status", is("COMPLETED"))
               .body("completedAt", notNullValue());
    }

    @Test
    @DisplayName("Ticket can be assigned to a planned phase via planningFields")
    void shouldAssignTicketToPhase() {
        var phaseId = given().header(pmHeader)
                             .accept(ContentType.JSON)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "Fase planejada",
                                       "objective": "Objetivo"
                                   }""")
                             .post("/api/projects/%d/phases".formatted(ticketFixtures.project().id()))
                             .then()
                             .statusCode(201)
                             .extract()
                             .path("id");

        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Ticket na fase",
                         "description": "Ticket atribuído à fase",
                         "categoryId": %d,
                         "priority": "MEDIUM",
                         "planningFields": {
                             "phaseId": %d
                         }
                     }""".formatted(ticketFixtures.feature().getId(), phaseId))
               .post("/api/tickets/" + ticketFixtures.ticket().id())
               .then()
               .statusCode(200)
               .body("phaseId", is(phaseId))
               .body("phaseName", is("Fase planejada"));
    }

    @Test
    @DisplayName("Phase deliverable version appears in version changelog Via fase section")
    void shouldIncludePhaseDeliverableInChangelog() {
        var versionId = given().header(pmHeader)
                               .accept(ContentType.JSON)
                               .contentType(ContentType.JSON)
                               .body("""
                                     {
                                         "label": "8.0.0",
                                         "description": "Release da fase"
                                     }""")
                               .post("/api/projects/%d/versions".formatted(ticketFixtures.project().id()))
                               .then()
                               .statusCode(201)
                               .extract()
                               .path("id");

        var phaseId = given().header(pmHeader)
                             .accept(ContentType.JSON)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "Fase entrega",
                                       "objective": "Entregar versão",
                                       "deliverableVersionId": %d
                                   }""".formatted(versionId))
                             .post("/api/projects/%d/phases".formatted(ticketFixtures.project().id()))
                             .then()
                             .statusCode(201)
                             .extract()
                             .path("id");

        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Via fase ticket",
                         "description": "Ticket na fase com versão entregável",
                         "categoryId": %d,
                         "priority": "MEDIUM",
                         "planningFields": {
                             "phaseId": %d
                         }
                     }""".formatted(ticketFixtures.feature().getId(), phaseId))
               .post("/api/tickets/" + ticketFixtures.ticket().id())
               .then()
               .statusCode(200);

        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/versions/%d/changelog".formatted(ticketFixtures.project().id(), versionId))
               .then()
               .statusCode(200)
               .body("sections.find { it.name == 'Via fase' }.tickets", org.hamcrest.Matchers.hasSize(1))
               .body("sections.find { it.name == 'Via fase' }.tickets[0].identifier", is(ticketFixtures.ticket().identifier()));
    }

    @Test
    @DisplayName("Clearing phase on ticket update removes assignment")
    void shouldClearPhaseOnTicketUpdate() {
        var phaseId = given().header(pmHeader)
                             .accept(ContentType.JSON)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "Fase temporária",
                                       "objective": "Temp"
                                   }""")
                             .post("/api/projects/%d/phases".formatted(ticketFixtures.project().id()))
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
                         "title": "Com fase",
                         "description": "Ticket com fase",
                         "categoryId": %d,
                         "priority": "MEDIUM",
                         "planningFields": {
                             "phaseId": %d
                         }
                     }""".formatted(ticketFixtures.feature().getId(), phaseId))
               .post("/api/tickets/" + ticketId)
               .then()
               .statusCode(200)
               .body("phaseName", is("Fase temporária"));

        given().header(ticketFixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Com fase",
                         "description": "Ticket com fase",
                         "categoryId": %d,
                         "priority": "MEDIUM",
                         "planningFields": {
                             "phaseId": null
                         }
                     }""".formatted(ticketFixtures.feature().getId()))
               .post("/api/tickets/" + ticketId)
               .then()
               .statusCode(200)
               .body("phaseId", nullValue())
               .body("phaseName", nullValue());
    }

    @Test
    @DisplayName("Should reject phase list for non-member")
    void shouldForbidNonMemberOnForeignProjectPhaseList() {
        var foreignProject = given().header(pmHeader)
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Foreign Phase %s",
                                              "description": "No membership for user",
                                              "prefix": "PH%s",
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

        given().header(Given.authenticatedUser())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/%d/phases".formatted(foreignProject.id()))
               .then()
               .statusCode(403);
    }
}

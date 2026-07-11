package dev.vepo.issues.ticket.link;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class TicketLinkEndpointTest {

    private TicketTestFixtures fixtures;
    private Header auth;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
        auth = fixtures.pmAuthenticatedHeader();
    }

    @Test
    @DisplayName("Should create, list and delete a peer BLOCKS link")
    void shouldCreateListAndDeletePeerLink() {
        var source = createTicket("Source Blocks " + UUID.randomUUID(), null);
        var target = createTicket("Target Blocked " + UUID.randomUUID(), null);

        var linkId = given().header(auth)
                            .contentType(ContentType.JSON)
                            .body("""
                                  {
                                      "targetTicketId": %d,
                                      "linkType": "BLOCKS"
                                  }""".formatted(target.id()))
                            .post("/api/tickets/%d/links".formatted(source.id()))
                            .then()
                            .statusCode(201)
                            .body("linkType", equalTo("BLOCKS"))
                            .body("direction", equalTo("OUTBOUND"))
                            .body("displayLabel", equalTo("Bloqueia"))
                            .body("otherTicketId", equalTo((int) target.id()))
                            .body("otherIdentifier", equalTo(target.identifier()))
                            .extract()
                            .path("id");

        given().header(auth)
               .accept(ContentType.JSON)
               .get("/api/tickets/%d/links".formatted(source.id()))
               .then()
               .statusCode(200)
               .body("$", hasSize(1))
               .body("[0].id", equalTo(linkId))
               .body("[0].displayLabel", equalTo("Bloqueia"));

        given().header(auth)
               .accept(ContentType.JSON)
               .get("/api/tickets/%d/links".formatted(target.id()))
               .then()
               .statusCode(200)
               .body("$", hasSize(1))
               .body("[0].direction", equalTo("INBOUND"))
               .body("[0].displayLabel", equalTo("Bloqueado por"))
               .body("[0].otherTicketId", equalTo((int) source.id()));

        given().header(auth)
               .delete("/api/tickets/%d/links/%d".formatted(source.id(), (Integer) linkId))
               .then()
               .statusCode(204);

        given().header(auth)
               .accept(ContentType.JSON)
               .get("/api/tickets/%d/links".formatted(source.id()))
               .then()
               .statusCode(200)
               .body("$", hasSize(0));
    }

    @Test
    @DisplayName("Should allow cross-project peer links when both projects are viewable")
    void shouldAllowCrossProjectPeerLink() {
        var otherProject = createExtraProject();
        var source = createTicket("Cross source " + UUID.randomUUID(), fixtures.project().id());
        var target = createTicket("Cross target " + UUID.randomUUID(), otherProject.id());

        given().header(auth)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "targetTicketId": %d,
                         "linkType": "RELATES_TO"
                     }""".formatted(target.id()))
               .post("/api/tickets/%d/links".formatted(source.id()))
               .then()
               .statusCode(201)
               .body("otherProjectId", equalTo((int) otherProject.id()))
               .body("displayLabel", equalTo("Relacionado a"));
    }

    @Test
    @DisplayName("Should create CHILD_OF under Epic and reject non-Epic parent")
    void shouldRequireEpicParentForChildOf() {
        var epic = createTicket("Epic parent " + UUID.randomUUID(), null, "EPIC");
        var task = createTicket("Child task " + UUID.randomUUID(), null, "TASK");
        var nonEpic = createTicket("Not epic " + UUID.randomUUID(), null, "STORY");
        var orphan = createTicket("Orphan " + UUID.randomUUID(), null, "TASK");

        given().header(auth)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "targetTicketId": %d,
                         "linkType": "CHILD_OF"
                     }""".formatted(epic.id()))
               .post("/api/tickets/%d/links".formatted(task.id()))
               .then()
               .statusCode(201)
               .body("linkType", equalTo("CHILD_OF"))
               .body("otherTicketId", equalTo((int) epic.id()));

        given().header(auth)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "targetTicketId": %d,
                         "linkType": "CHILD_OF"
                     }""".formatted(nonEpic.id()))
               .post("/api/tickets/%d/links".formatted(orphan.id()))
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should reject second parent for CHILD_OF")
    void shouldRejectSecondParent() {
        var epic1 = createTicket("Epic one " + UUID.randomUUID(), null, "EPIC");
        var epic2 = createTicket("Epic two " + UUID.randomUUID(), null, "EPIC");
        var child = createTicket("Single parent child " + UUID.randomUUID(), null, "TASK");

        given().header(auth)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "targetTicketId": %d,
                         "linkType": "CHILD_OF"
                     }""".formatted(epic1.id()))
               .post("/api/tickets/%d/links".formatted(child.id()))
               .then()
               .statusCode(201);

        given().header(auth)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "targetTicketId": %d,
                         "linkType": "CHILD_OF"
                     }""".formatted(epic2.id()))
               .post("/api/tickets/%d/links".formatted(child.id()))
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should create child ticket under Epic via /children")
    void shouldCreateChildUnderEpic() {
        var epic = createTicket("Epic for children " + UUID.randomUUID(), null, "EPIC");

        var child = given().header(auth)
                           .contentType(ContentType.JSON)
                           .body("""
                                 {
                                     "title": "Nova subtarefa criada",
                                     "description": "Criada sob o épico"
                                 }""")
                           .post("/api/tickets/%d/children".formatted(epic.id()))
                           .then()
                           .statusCode(201)
                           .body("ticketType", equalTo("TASK"))
                           .body("project", equalTo((int) fixtures.project().id()))
                           .extract()
                           .as(TicketResponse.class);

        given().header(auth)
               .accept(ContentType.JSON)
               .get("/api/tickets/%d/links".formatted(child.id()))
               .then()
               .statusCode(200)
               .body("$", hasSize(1))
               .body("[0].linkType", equalTo("CHILD_OF"))
               .body("[0].otherTicketId", equalTo((int) epic.id()));

        given().header(auth)
               .accept(ContentType.JSON)
               .get("/api/tickets/%d/expanded".formatted(epic.id()))
               .then()
               .statusCode(200)
               .body("ticketType", equalTo("EPIC"))
               .body("childrenSummary.total", equalTo(1))
               .body("childrenSummary.done", equalTo(0))
               .body("links.linkType", hasItem("CHILD_OF"));
    }

    @Test
    @DisplayName("Should reject create-child when ticket is not an Epic")
    void shouldRejectCreateChildOnNonEpic() {
        var task = createTicket("Not epic for children " + UUID.randomUUID(), null, "TASK");

        given().header(auth)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Should fail child",
                         "description": "Parent is not epic"
                     }""")
               .post("/api/tickets/%d/children".formatted(task.id()))
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should record LINK_ADDED and LINK_REMOVED history")
    void shouldRecordLinkHistory() {
        var source = createTicket("History source " + UUID.randomUUID(), null);
        var target = createTicket("History target " + UUID.randomUUID(), null);

        var linkId = given().header(auth)
                            .contentType(ContentType.JSON)
                            .body("""
                                  {
                                      "targetTicketId": %d,
                                      "linkType": "DUPLICATES"
                                  }""".formatted(target.id()))
                            .post("/api/tickets/%d/links".formatted(source.id()))
                            .then()
                            .statusCode(201)
                            .extract()
                            .path("id");

        given().header(auth)
               .accept(ContentType.JSON)
               .get("/api/tickets/%d/history".formatted(source.id()))
               .then()
               .statusCode(200)
               .body("action", hasItem("LINK_ADDED"));

        given().header(auth)
               .delete("/api/tickets/%d/links/%d".formatted(source.id(), (Integer) linkId))
               .then()
               .statusCode(204);

        given().header(auth)
               .accept(ContentType.JSON)
               .get("/api/tickets/%d/history".formatted(source.id()))
               .then()
               .statusCode(200)
               .body("action", hasItems("LINK_ADDED", "LINK_REMOVED"));
    }

    @Test
    @DisplayName("Should omit soft-deleted other end from link list")
    void shouldOmitDeletedOtherEndFromList() {
        var source = createTicket("Keep source " + UUID.randomUUID(), null);
        var target = createTicket("Soft delete target " + UUID.randomUUID(), null);

        given().header(auth)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "targetTicketId": %d,
                         "linkType": "BLOCKS"
                     }""".formatted(target.id()))
               .post("/api/tickets/%d/links".formatted(source.id()))
               .then()
               .statusCode(201);

        given().header(auth)
               .delete("/api/tickets/" + target.id())
               .then()
               .statusCode(204);

        given().header(auth)
               .accept(ContentType.JSON)
               .get("/api/tickets/%d/links".formatted(source.id()))
               .then()
               .statusCode(200)
               .body("$", hasSize(0));
    }

    @Test
    @DisplayName("Should reject reverse RELATES_TO as duplicate")
    void shouldRejectReverseRelatesTo() {
        var a = createTicket("Relates A " + UUID.randomUUID(), null);
        var b = createTicket("Relates B " + UUID.randomUUID(), null);

        given().header(auth)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "targetTicketId": %d,
                         "linkType": "RELATES_TO"
                     }""".formatted(b.id()))
               .post("/api/tickets/%d/links".formatted(a.id()))
               .then()
               .statusCode(201);

        given().header(auth)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "targetTicketId": %d,
                         "linkType": "RELATES_TO"
                     }""".formatted(a.id()))
               .post("/api/tickets/%d/links".formatted(b.id()))
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should create and update ticket type")
    void shouldCreateAndUpdateTicketType() {
        var created = createTicket("Typed ticket " + UUID.randomUUID(), null, "STORY");
        given().header(auth)
               .accept(ContentType.JSON)
               .get("/api/tickets/" + created.id())
               .then()
               .statusCode(200)
               .body("ticketType", equalTo("STORY"));

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "%s",
                         "description": "%s",
                         "categoryId": %d,
                         "priority": "MEDIUM",
                         "ticketType": "EPIC"
                     }""".formatted(created.title(), created.description(), created.category()))
               .post("/api/tickets/" + created.id())
               .then()
               .statusCode(200)
               .body("ticketType", equalTo("EPIC"));
    }

    private TicketResponse createTicket(String title, Long projectId) {
        return createTicket(title, projectId, null);
    }

    private TicketResponse createTicket(String title, Long projectId, String ticketType) {
        var pid = projectId != null ? projectId : fixtures.project().id();
        var typeJson = ticketType == null ? "" : ",\n                         \"ticketType\": \"%s\"".formatted(ticketType);
        return given().header(auth)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "title": "%s",
                                "description": "Ticket used by ticket-links tests.",
                                "projectId": %d,
                                "categoryId": %d%s
                            }""".formatted(title, pid, fixtures.bug().getId(), typeJson))
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }

    private ProjectResponse createExtraProject() {
        var workflowId = fixtures.project().workflow().id();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var project = given().header(auth)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "Link Project %s",
                                       "description": "Second project for cross-project links.",
                                       "prefix": "L%s",
                                       "workflowId": %d
                                   }""".formatted(suffix, suffix, workflowId))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(ProjectResponse.class);
        Given.addProjectMember(project.id(), "user@issues.vepo.dev");
        return project;
    }
}

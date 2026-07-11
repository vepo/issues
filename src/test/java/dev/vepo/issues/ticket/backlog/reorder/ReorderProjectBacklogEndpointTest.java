package dev.vepo.issues.ticket.backlog.reorder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.ticket.TicketResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class ReorderProjectBacklogEndpointTest {

    private Header pmHeader;
    private Header userHeader;
    private Header adminHeader;
    private ProjectResponse project;
    private Category category;
    private TicketResponse ticketA;
    private TicketResponse ticketB;
    private TicketResponse ticketC;

    @BeforeEach
    void setUp() {
        pmHeader = Given.authenticatedProjectManager();
        userHeader = Given.authenticatedUser();
        adminHeader = Given.authenticatedAdmin();
        project = Given.simpleProject();
        category = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                                .save(new Category("ReorderCat" + UUID.randomUUID(), "green")));
        ticketA = createTicket("Reorder A");
        ticketB = createTicket("Reorder B");
        ticketC = createTicket("Reorder C");
    }

    @Test
    @DisplayName("PM can reorder ticket before another")
    void shouldReorderBeforeTicketAsProjectManager() {
        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "ticketId": %d,
                         "beforeTicketId": %d
                     }
                     """.formatted(ticketC.id(), ticketA.id()))
               .post("/api/projects/%d/backlog/reorder".formatted(project.id()))
               .then()
               .statusCode(200)
               .body("id", equalTo((int) ticketC.id()));

        var ids = backlogIds();
        assertEquals(List.of(ticketC.id(), ticketA.id(), ticketB.id()), ids);
    }

    @Test
    @DisplayName("Admin can move ticket to end of backlog")
    void shouldMoveToEndAsAdmin() {
        given().header(adminHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "ticketId": %d,
                         "beforeTicketId": null
                     }
                     """.formatted(ticketA.id()))
               .post("/api/projects/%d/backlog/reorder".formatted(project.id()))
               .then()
               .statusCode(200);

        var ids = backlogIds();
        assertEquals(ticketA.id(), ids.get(ids.size() - 1));
    }

    @Test
    @DisplayName("Regular user cannot reorder backlog")
    void shouldForbidUserReorder() {
        given().header(userHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "ticketId": %d,
                         "beforeTicketId": %d
                     }
                     """.formatted(ticketB.id(), ticketA.id()))
               .post("/api/projects/%d/backlog/reorder".formatted(project.id()))
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("Reorder writes FIELD_CHANGED history for backlogRank")
    void shouldWriteHistoryOnReorder() {
        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "ticketId": %d,
                         "beforeTicketId": null
                     }
                     """.formatted(ticketA.id()))
               .post("/api/projects/%d/backlog/reorder".formatted(project.id()))
               .then()
               .statusCode(200);

        List<?> history = given().header(pmHeader)
                                 .accept(ContentType.JSON)
                                 .when()
                                 .get("/api/tickets/%d/history".formatted(ticketA.id()))
                                 .then()
                                 .statusCode(200)
                                 .extract()
                                 .jsonPath()
                                 .getList("");

        @SuppressWarnings("unchecked")
        var fieldChange = history.stream()
                                 .map(entry -> (Map<String, Object>) entry)
                                 .filter(e -> "FIELD_CHANGED".equals(e.get("action")) && "backlogRank".equals(e.get("field")))
                                 .findFirst();
        assertTrue(fieldChange.isPresent());
    }

    @Test
    @DisplayName("Reorder rejects ticket not in backlog")
    void shouldRejectDoneTicketReorder() {
        var inProgress = Given.status("In Progress");
        var done = Given.status("Done");
        given().header(userHeader)
               .contentType(ContentType.JSON)
               .body("""
                     { "to": %d }
                     """.formatted(inProgress.getId()))
               .post("/api/tickets/%d/move".formatted(ticketB.id()))
               .then()
               .statusCode(200);
        given().header(userHeader)
               .contentType(ContentType.JSON)
               .body("""
                     { "to": %d }
                     """.formatted(done.getId()))
               .post("/api/tickets/%d/move".formatted(ticketB.id()))
               .then()
               .statusCode(200);

        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "ticketId": %d,
                         "beforeTicketId": null
                     }
                     """.formatted(ticketB.id()))
               .post("/api/projects/%d/backlog/reorder".formatted(project.id()))
               .then()
               .statusCode(404);
    }

    private List<Long> backlogIds() {
        List<Map<String, Object>> items = given().header(pmHeader)
                                                 .accept(ContentType.JSON)
                                                 .queryParam("size", 100)
                                                 .when()
                                                 .get("/api/projects/%d/backlog".formatted(project.id()))
                                                 .then()
                                                 .statusCode(200)
                                                 .body("items.id", hasItem((int) ticketA.id()))
                                                 .extract()
                                                 .jsonPath()
                                                 .getList("items");
        return items.stream()
                    .map(i -> ((Number) i.get("id")).longValue())
                    .filter(id -> id == ticketA.id() || id == ticketB.id() || id == ticketC.id())
                    .toList();
    }

    private TicketResponse createTicket(String title) {
        return given().header(pmHeader)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "title": "%s",
                                "description": "Reorder test",
                                "projectId": %d,
                                "categoryId": %d
                            }
                            """.formatted(title, project.id(), category.getId()))
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }
}

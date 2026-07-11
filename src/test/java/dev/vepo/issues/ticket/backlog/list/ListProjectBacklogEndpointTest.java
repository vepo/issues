package dev.vepo.issues.ticket.backlog.list;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
class ListProjectBacklogEndpointTest {

    private Header pmHeader;
    private Header userHeader;
    private ProjectResponse project;
    private Category category;

    @BeforeEach
    void setUp() {
        pmHeader = Given.authenticatedProjectManager();
        userHeader = Given.authenticatedUser();
        project = Given.simpleProject();
        category = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                                .save(new Category("BacklogCat" + UUID.randomUUID(), "blue")));
    }

    @Test
    @DisplayName("Should list backlog tickets ordered by backlog rank")
    void shouldListBacklogOrderedByRank() {
        var first = createTicket("First backlog ticket");
        var second = createTicket("Second backlog ticket");

        List<Map<String, Object>> items = given().header(userHeader)
                                                 .accept(ContentType.JSON)
                                                 .queryParam("size", 100)
                                                 .when()
                                                 .get("/api/projects/%d/backlog".formatted(project.id()))
                                                 .then()
                                                 .statusCode(200)
                                                 .body("page", equalTo(0))
                                                 .body("size", equalTo(100))
                                                 .body("total", greaterThanOrEqualTo(2))
                                                 .extract()
                                                 .jsonPath()
                                                 .getList("items");

        var ids = items.stream().map(i -> ((Number) i.get("id")).longValue()).toList();
        assertTrue(ids.contains(first.id()));
        assertTrue(ids.contains(second.id()));
        assertTrue(ids.indexOf(first.id()) < ids.indexOf(second.id()));

        var firstItem = items.stream().filter(i -> ((Number) i.get("id")).longValue() == first.id()).findFirst().orElseThrow();
        var secondItem = items.stream().filter(i -> ((Number) i.get("id")).longValue() == second.id()).findFirst().orElseThrow();
        assertTrue(((Number) firstItem.get("backlogRank")).intValue() < ((Number) secondItem.get("backlogRank")).intValue());
    }

    @Test
    @DisplayName("Should exclude done and deleted tickets from backlog")
    void shouldExcludeDoneAndDeletedTickets() {
        var open = createTicket("Open for backlog");
        var doneTicket = createTicket("Done for backlog");
        var deletedTicket = createTicket("Deleted for backlog");

        var done = Given.status("Done");
        var inProgress = Given.status("In Progress");
        given().header(userHeader)
               .contentType(ContentType.JSON)
               .body("""
                     { "to": %d }
                     """.formatted(inProgress.getId()))
               .post("/api/tickets/%d/move".formatted(doneTicket.id()))
               .then()
               .statusCode(200);
        given().header(userHeader)
               .contentType(ContentType.JSON)
               .body("""
                     { "to": %d }
                     """.formatted(done.getId()))
               .post("/api/tickets/%d/move".formatted(doneTicket.id()))
               .then()
               .statusCode(200);

        given().header(pmHeader)
               .when()
               .delete("/api/tickets/%d".formatted(deletedTicket.id()))
               .then()
               .statusCode(204);

        given().header(userHeader)
               .accept(ContentType.JSON)
               .queryParam("size", 100)
               .when()
               .get("/api/projects/%d/backlog".formatted(project.id()))
               .then()
               .statusCode(200)
               .body("items.id", hasItem((int) open.id()))
               .body("items.id", not(hasItem((int) doneTicket.id())))
               .body("items.id", not(hasItem((int) deletedTicket.id())));
    }

    @Test
    @DisplayName("Should paginate backlog with hasMore")
    void shouldPaginateBacklog() {
        for (var i = 0; i < 3; i++) {
            createTicket("Page ticket " + i + " " + UUID.randomUUID());
        }

        given().header(userHeader)
               .accept(ContentType.JSON)
               .queryParam("page", 0)
               .queryParam("size", 2)
               .when()
               .get("/api/projects/%d/backlog".formatted(project.id()))
               .then()
               .statusCode(200)
               .body("page", equalTo(0))
               .body("size", equalTo(2))
               .body("items.size()", equalTo(2))
               .body("hasMore", equalTo(true))
               .body("total", greaterThanOrEqualTo(3));

        given().header(userHeader)
               .accept(ContentType.JSON)
               .queryParam("page", 1)
               .queryParam("size", 2)
               .when()
               .get("/api/projects/%d/backlog".formatted(project.id()))
               .then()
               .statusCode(200)
               .body("page", equalTo(1))
               .body("size", equalTo(2))
               .body("items.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("Should reject backlog list for non-member")
    void shouldForbidNonMember() {
        var otherProject = given().header(pmHeader)
                                  .contentType(ContentType.JSON)
                                  .body("""
                                        {
                                            "name": "Other Project %s",
                                            "description": "No membership for user",
                                            "prefix": "OB%s",
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
               .get("/api/projects/%d/backlog".formatted(otherProject.id()))
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("New ticket should append at end of backlog rank")
    void shouldAssignEndRankOnCreate() {
        createTicket("Existing A");
        createTicket("Existing B");

        var created = createTicket("Newest ticket");

        List<Map<String, Object>> items = given().header(pmHeader)
                                                 .accept(ContentType.JSON)
                                                 .queryParam("size", 100)
                                                 .when()
                                                 .get("/api/projects/%d/backlog".formatted(project.id()))
                                                 .then()
                                                 .statusCode(200)
                                                 .extract()
                                                 .jsonPath()
                                                 .getList("items");

        var last = items.get(items.size() - 1);
        assertEquals(created.id(), ((Number) last.get("id")).longValue());
        var maxRank = items.stream().mapToInt(i -> ((Number) i.get("backlogRank")).intValue()).max().orElseThrow();
        assertEquals(maxRank, ((Number) last.get("backlogRank")).intValue());
    }

    private TicketResponse createTicket(String title) {
        return given().header(pmHeader)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "title": "%s",
                                "description": "Backlog test",
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

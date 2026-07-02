package dev.vepo.issues.ticket;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import dev.vepo.issues.Given;
import dev.vepo.issues.auth.AuthResponse;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.ticket.TicketEndpoint.SubscribeRequest;
import dev.vepo.issues.workflow.StatusEndpoint.StatusResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class TicketEndpointTest {
    private ProjectResponse project;
    private Header userAuthenticatedHeader;
    private Header pmAuthenticatedHeader;
    private TicketResponse ticket;
    private List<StatusResponse> allStatuses;
    private Category bug;
    private Category feature;

    @BeforeEach
    void setup() {
        this.bug = Given.transaction(() -> Given.inject(CategoryRepository.class).save(new Category("Bug" + UUID.randomUUID(), "red")));
        this.feature = Given.transaction(() -> Given.inject(CategoryRepository.class).save(new Category("Feature" + UUID.randomUUID(), "green")));
        this.project = Given.simpleProject();
        this.ticket = Given.simpleTicket(this.project.id(), bug.getId());
        this.userAuthenticatedHeader = Given.authenticatedUser();
        this.pmAuthenticatedHeader = Given.authenticatedProjectManager();
        this.allStatuses = Given.allStatuses();
    }

    @Test
    @Order(1)
    @DisplayName("No authenticated user should be able to list tickets")
    void noAuthenticatedUserShouldListTicketsTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/tickets")
               .then()
               .statusCode(401);
    }

    @Test
    @Order(2)
    @DisplayName("Only authenticated users should be able to list tickets")
    void onlyAuthenticatedUsersShouldListTicketsTest() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0));
    }

    @Test
    @Order(3)
    @DisplayName("It should be possible to list tickets by status")
    void shouldListTicketsByStatusTest() {
        var todo = allStatuses.stream()
                              .filter(status -> status.name().equals("TODO"))
                              .findFirst()
                              .orElseThrow(() -> new IllegalStateException("TODO status not found"));
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets?status=TODO")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].status", equalTo((int) todo.id()));
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets?status=" + todo.id())
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].status", equalTo((int) todo.id()));
    }

    @Test
    @Order(4)
    @DisplayName("It should be possible to search tickets by term on title or description")
    void shouldSearchTicketsByTermTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/search?term=" + ticket.title())
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].title", equalTo(ticket.title()));
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/search?term=" + ticket.description())
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].description", equalTo(ticket.description()));
    }

    @Test
    @Order(5)
    @DisplayName("It should be possible to retrieve a ticket by its ID")
    void shouldRetrieveTicketByIdTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/{id}", ticket.id())
               .then()
               .statusCode(200)
               .body("id", equalTo((int) ticket.id()))
               .body("title", equalTo(ticket.title()))
               .body("description", equalTo(ticket.description()))
               .body("status", equalTo((int) ticket.status()))
               .body("project", equalTo((int) ticket.project()))
               .body("category", equalTo((int) ticket.category()))
               .body("author", equalTo((int) ticket.author()));
    }

    @Test
    @Order(6)
    @DisplayName("It should be possible to create a new ticket")
    void shouldCreateNewTicketTest() {
        given().header(pmAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "New Ticket",
                         "description": "This is a new ticket.",
                         "projectId": %d,
                         "categoryId": %d
                     }""".formatted(project.id(), bug.getId()))
               .post("/api/tickets")
               .then()
               .statusCode(201)
               .body("title", equalTo("New Ticket"))
               .body("description", equalTo("This is a new ticket."))
               .body("project", equalTo((int) project.id()))
               .body("category", equalTo(bug.getId().intValue()))
               .body("author", equalTo((int) Given.userIdByEmail("pm@issues.vepo.dev")))
               .body("status", equalTo((int) allStatuses.stream()
                                                        .filter(status -> status.name().equals("TODO"))
                                                        .findFirst()
                                                        .orElseThrow(() -> new IllegalStateException("TODO status not found")).id()));
    }

    @Test
    @Order(7)
    @DisplayName("It should not be possible to create a ticket with an invalid project ID")
    void shouldNotCreateTicketWithInvalidProjectIdTest() {
        given().header(pmAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Invalid Project Ticket",
                         "description": "This ticket has an invalid project ID.",
                         "projectId": 9999,
                         "categoryId": %d
                     }""".formatted(bug.getId()))
               .post("/api/tickets")
               .then()
               .statusCode(404)
               .body("message", equalTo("Project does not found! projectId=9999"));
    }

    @Test
    @Order(8)
    @DisplayName("It should return a expanded entity with ")
    void shouldReturnExpandedTicketInformationTest() {
        var category = Given.category(ticket.category());
        given().header(userAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/{id}/expanded", ticket.id())
               .then()
               .statusCode(200)
               .body("id", equalTo((int) ticket.id()))
               .body("title", equalTo(ticket.title()))
               .body("description", equalTo(ticket.description()))
               .body("category", equalTo(category.getName()))
               .body("project.id", equalTo((int) project.id()))
               .body("project.name", equalTo(project.name()));
        given().header(userAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/{id}/expanded", ticket.identifier())
               .then()
               .statusCode(200)
               .body("id", equalTo((int) ticket.id()))
               .body("title", equalTo(ticket.title()))
               .body("description", equalTo(ticket.description()))
               .body("category", equalTo(category.getName()))
               .body("project.id", equalTo((int) project.id()))
               .body("project.name", equalTo(project.name()));
    }

    @Test
    @Order(9)
    @DisplayName("It should be possible to update a ticket")
    void updateTicketTest() {
        given().header(userAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "New Ticket Title",
                         "description": "New Ticket description",
                         "categoryId": %d
                     }""".formatted(feature.getId()))
               .post("/api/tickets/" + ticket.id())
               .then()
               .statusCode(200)
               .body("title", equalTo("New Ticket Title"))
               .body("description", equalTo("New Ticket description"))
               .body("category", is(feature.getId().intValue()));
    }

    @Test
    @Order(10)
    @DisplayName("It should be psosible to move ticket to a new status")
    void moveTicketTest() {
        var inProgress = Given.status("In Progress");
        given().header(userAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(inProgress.getId()))
               .post("/api/tickets/" + ticket.id() + "/move")
               .then()
               .statusCode(200)
               .body("status", is(inProgress.getId().intValue()));
    }

    @Test
    @Order(11)
    @DisplayName("It should be possible to update ticket assignee")
    void updateAssigneeTest() {
        var newAssignee = Given.user("user2@issues.vepo.dev");
        given().header(pmAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "assigneeId": %d
                     }""".formatted(newAssignee.getId()))
               .patch("/api/tickets/" + ticket.id() + "/assignee")
               .then()
               .statusCode(200)
               .body("assignee", equalTo(newAssignee.getId().intValue()));
    }

    @Test
    @Order(12)
    @DisplayName("It should not be possible to update assignee with invalid user ID")
    void shouldNotUpdateAssigneeWithInvalidUserIdTest() {
        given().header(pmAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "assigneeId": 9999
                     }""")
               .patch("/api/tickets/" + ticket.id() + "/assignee")
               .then()
               .statusCode(404)
               .body("message", equalTo("User does not found! userId=9999"));
    }

    @Test
    @Order(13)
    @DisplayName("Only admin or project manager should be able to delete a ticket")
    void shouldDeleteTicketTest() {
        // First create a ticket to delete
        var ticketToDelete = given().header(pmAuthenticatedHeader)
                                    .contentType(ContentType.JSON)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .body("""
                                          {
                                              "title": "Ticket to delete",
                                              "description": "This ticket will be deleted.",
                                              "projectId": %d,
                                              "categoryId": %d
                                          }""".formatted(project.id(), bug.getId()))
                                    .post("/api/tickets")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(TicketResponse.class);

        // Delete the ticket as PM
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .delete("/api/tickets/" + ticketToDelete.id())
               .then()
               .statusCode(204);

        // Verify ticket is deleted
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/" + ticketToDelete.id())
               .then()
               .statusCode(404);
    }

    @Test
    @Order(14)
    @DisplayName("Regular user should not be able to delete a ticket")
    void regularUserShouldNotDeleteTicketTest() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .delete("/api/tickets/" + ticket.id())
               .then()
               .statusCode(403);
    }

    @Test
    @Order(15)
    @DisplayName("It should be possible to list comments for a ticket")
    void shouldListCommentsTest() {
        // First add a comment
        given().header(userAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "content": "This is a test comment"
                     }""")
               .post("/api/tickets/" + ticket.id() + "/comments")
               .then()
               .statusCode(201);

        // Then list comments
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/" + ticket.id() + "/comments")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].content", equalTo("This is a test comment"));
    }

    @Test
    @Order(16)
    @DisplayName("It should be possible to add a comment to a ticket")
    void shouldAddCommentTest() {
        var loggedUser = given().header(userAuthenticatedHeader)
                                .accept(ContentType.JSON)
                                .when()
                                .get("/api/auth/me")
                                .then()
                                .statusCode(200)
                                .extract()
                                .as(AuthResponse.class);
        given().header(userAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "content": "Another test comment"
                     }""")
               .post("/api/tickets/" + ticket.id() + "/comments")
               .then()
               .statusCode(201)
               .body("content", equalTo("Another test comment"))
               .body("author.id", equalTo((int) loggedUser.id()))
               .body("author.name", equalTo(loggedUser.name()))
               .body("author.email", equalTo(loggedUser.email()));
    }

    @Test
    @Order(17)
    @DisplayName("It should not be possible to add a comment to a non-existent ticket")
    void shouldNotAddCommentToInvalidTicketTest() {
        given().header(userAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "content": "Invalid ticket comment"
                     }""")
               .post("/api/tickets/9999/comments")
               .then()
               .statusCode(404)
               .body("message", equalTo("Ticket does not found! ticketId=9999"));
    }

    @Test
    @Order(18)
    @DisplayName("It should be possible to get ticket history")
    void shouldGetTicketHistoryTest() {
        // First make some changes to generate history
        given().header(userAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Updated Title for History",
                         "description": "Updated description for history",
                         "categoryId": %d
                     }""".formatted(feature.getId()))
               .post("/api/tickets/" + ticket.id())
               .then()
               .statusCode(200);

        // Get history
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/" + ticket.id() + "/history")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0));
    }

    @Test
    @Order(19)
    @DisplayName("It should not be possible to move ticket to invalid status")
    void shouldNotMoveToInvalidStatusTest() {
        given().header(userAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": 9999
                     }""")
               .post("/api/tickets/" + ticket.id() + "/move")
               .then()
               .statusCode(400)
               .body("message", equalTo("Stage not defined in project! stageId=9999"));
    }

    @Test
    @Order(20)
    @DisplayName("It should not be possible to update ticket with invalid category ID")
    void shouldNotUpdateTicketWithInvalidCategoryIdTest() {
        given().header(userAuthenticatedHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Invalid Category Ticket",
                         "description": "This ticket has an invalid category ID.",
                         "categoryId": 9999
                     }""")
               .post("/api/tickets/" + ticket.id())
               .then()
               .statusCode(404)
               .body("message", equalTo("Category does not found! categoryId=9999"));
    }

    @Test
    @Order(21)
    @DisplayName("It should subscribe/unsubscribe to tickets")
    void subsrcibeTest() {
        var authUser = Given.me(userAuthenticatedHeader);
        // subscribe to ticket
        // execute the operation again to verify its idempotent
        IntStream.range(0, 10)
                 .forEach(count -> {
                     given().header(userAuthenticatedHeader)
                            .accept(ContentType.JSON)
                            .contentType(ContentType.JSON)
                            .when()
                            .body(new SubscribeRequest(authUser.id()))
                            .put("/api/tickets/" + ticket.id() + "/subscribe")
                            .then()
                            .statusCode(200);
                     // check if user is subscribed
                     given().header(userAuthenticatedHeader)
                            .contentType(ContentType.JSON)
                            .accept(ContentType.JSON)
                            .when()
                            .get("/api/tickets/{id}/expanded", ticket.id())
                            .then()
                            .statusCode(200)
                            .body("id", equalTo((int) ticket.id()))
                            .body("subscribers.size()", Matchers.equalTo(1))
                            .body("subscribers[0].username", equalTo(authUser.username()));
                 });
        // unsubscribe
        // execute the operation again to verify its idempotent
        IntStream.range(0, 10)
                 .forEach(count -> {
                     given().header(userAuthenticatedHeader)
                            .accept(ContentType.JSON)
                            .contentType(ContentType.JSON)
                            .when()
                            .delete("/api/tickets/" + ticket.id() + "/subscribe/" + authUser.id())
                            .then()
                            .statusCode(200);
                     given().header(userAuthenticatedHeader)
                            .contentType(ContentType.JSON)
                            .accept(ContentType.JSON)
                            .when()
                            .get("/api/tickets/{id}/expanded", ticket.id())
                            .then()
                            .statusCode(200)
                            .body("id", equalTo((int) ticket.id()))
                            .body("subscribers.size()", Matchers.equalTo(0));
                 });
    }
}

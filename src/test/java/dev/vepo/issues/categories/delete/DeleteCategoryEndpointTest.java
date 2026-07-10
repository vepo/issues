package dev.vepo.issues.categories.delete;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class DeleteCategoryEndpointTest {

    private static final String TICKET_REFERENCE_MESSAGE = "Category cannot be deleted while tickets reference it";
    private static final String TEMPLATE_REFERENCE_MESSAGE = "Category cannot be deleted while project ticket templates reference it";

    @Test
    void shouldReturnNoContentWhenAdminDeletesUnusedCategory() {
        var admin = Given.authenticatedAdmin();
        var categoryId = createCategory(admin, "Unused");

        given().header(admin)
               .when()
               .delete("/api/categories/" + categoryId)
               .then()
               .statusCode(204);
    }

    @Test
    void shouldRejectDeleteWhenTicketReferencesCategory() {
        var admin = Given.authenticatedAdmin();
        var categoryId = createCategory(admin, "TicketRef");
        createTicketWithCategory(categoryId);

        given().header(admin)
               .when()
               .delete("/api/categories/" + categoryId)
               .then()
               .statusCode(400)
               .body("message", equalTo(TICKET_REFERENCE_MESSAGE));
    }

    @Test
    void shouldRejectDeleteWhenSoftDeletedTicketReferencesCategory() {
        var admin = Given.authenticatedAdmin();
        var categoryId = createCategory(admin, "SoftDeletedRef");
        var ticketId = createTicketWithCategory(categoryId);

        given().header(Given.authenticatedProjectManager())
               .when()
               .delete("/api/tickets/" + ticketId)
               .then()
               .statusCode(204);

        given().header(admin)
               .when()
               .delete("/api/categories/" + categoryId)
               .then()
               .statusCode(400)
               .body("message", equalTo(TICKET_REFERENCE_MESSAGE));
    }

    @Test
    void shouldRejectDeleteWhenProjectTicketTemplateReferencesCategory() {
        var admin = Given.authenticatedAdmin();
        var categoryId = createCategory(admin, "TemplateRef");
        createProjectWithTicketTemplateCategory(categoryId);

        given().header(admin)
               .when()
               .delete("/api/categories/" + categoryId)
               .then()
               .statusCode(400)
               .body("message", equalTo(TEMPLATE_REFERENCE_MESSAGE));
    }

    @Test
    void shouldReturnNotFoundWhenCategoryIdUnknown() {
        given().header(Given.authenticatedAdmin())
               .when()
               .delete("/api/categories/999999999")
               .then()
               .statusCode(404);
    }

    @Test
    void regularUserShouldNotDeleteCategory() {
        var admin = Given.authenticatedAdmin();
        var categoryId = createCategory(admin, "Forbidden");

        given().header(Given.authenticatedUser())
               .when()
               .delete("/api/categories/" + categoryId)
               .then()
               .statusCode(403);
    }

    private static int createCategory(Header admin, String namePrefix) {
        var name = namePrefix + "-" + UUID.randomUUID();
        return given().header(admin)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                              "name": "%s",
                              "color": "#BF0603"
                            }
                            """.formatted(name))
                      .when()
                      .post("/api/categories")
                      .then()
                      .statusCode(201)
                      .extract()
                      .path("id");
    }

    private static int createTicketWithCategory(int categoryId) {
        var project = Given.simpleProject();
        return given().header(Given.authenticatedProjectManager())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "title": "Category delete guard %s",
                                "description": "Ticket referencing category under delete guard test.",
                                "projectId": %d,
                                "categoryId": %d
                            }""".formatted(UUID.randomUUID(), project.id(), categoryId))
                      .when()
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .path("id");
    }

    private static void createProjectWithTicketTemplateCategory(int categoryId) {
        var workflow = Given.simpleWorkflow();
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        given().header(Given.authenticatedProjectManager())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Template Ref Project %s",
                         "description": "Project whose ticket template references a category.",
                         "prefix": "TR%s",
                         "workflowId": %d,
                         "ticketTemplate": {
                             "enabled": true,
                             "title": "Default ticket title",
                             "description": "Default ticket description for new tickets.",
                             "categoryId": %d,
                             "priority": "MEDIUM"
                         }
                     }""".formatted(suffix, suffix.substring(0, 2), workflow.id(), categoryId))
               .when()
               .post("/api/projects")
               .then()
               .statusCode(201)
               .body("ticketTemplate.categoryId", is(categoryId));
    }
}

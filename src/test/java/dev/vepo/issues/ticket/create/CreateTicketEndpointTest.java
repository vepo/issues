package dev.vepo.issues.ticket.create;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class CreateTicketEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be possible to create a new ticket")
    void shouldCreateNewTicketTest() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "New Ticket",
                         "description": "This is a new ticket.",
                         "projectId": %d,
                         "categoryId": %d
                     }""".formatted(fixtures.project().id(), fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(201)
               .body("title", equalTo("New Ticket"))
               .body("description", equalTo("This is a new ticket."))
               .body("project", equalTo((int) fixtures.project().id()))
               .body("category", equalTo(fixtures.bug().getId().intValue()))
               .body("author", equalTo((int) Given.userIdByEmail("pm@issues.vepo.dev")))
               .body("status", equalTo((int) fixtures.allStatuses().stream()
                                                     .filter(status -> status.name().equals("TODO"))
                                                     .findFirst()
                                                     .orElseThrow(() -> new IllegalStateException("TODO status not found")).id()));
    }

    @Test
    @DisplayName("Should create ticket with optional due date")
    void shouldCreateTicketWithDueDate() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Ticket With Due Date",
                         "description": "This ticket has a planned due date.",
                         "projectId": %d,
                         "categoryId": %d,
                         "dueDate": "2026-08-15"
                     }""".formatted(fixtures.project().id(), fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(201)
               .body("dueDate", equalTo("2026-08-15"));
    }

    @Test
    @DisplayName("Should create ticket with story points")
    void shouldCreateTicketWithStoryPoints() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Ticket With Story Points",
                         "description": "This ticket has story points assigned.",
                         "projectId": %d,
                         "categoryId": %d,
                         "storyPoints": 5
                     }""".formatted(fixtures.project().id(), fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(201)
               .body("storyPoints", equalTo(5));
    }

    @Test
    @DisplayName("Should reject negative story points on create")
    void shouldRejectNegativeStoryPointsOnCreate() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Invalid Story Points",
                         "description": "This ticket has invalid story points.",
                         "projectId": %d,
                         "categoryId": %d,
                         "storyPoints": -1
                     }""".formatted(fixtures.project().id(), fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("It should be possible to create a ticket assigned to a phase")
    void shouldCreateTicketWithPhaseAssignment() {
        var phaseId = given().header(fixtures.pmAuthenticatedHeader())
                             .accept(ContentType.JSON)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "Fase para create ticket"
                                   }""")
                             .post("/api/projects/%d/phases".formatted(fixtures.project().id()))
                             .then()
                             .statusCode(201)
                             .extract()
                             .path("id");

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Ticket in phase",
                         "description": "Created with phase assignment.",
                         "projectId": %d,
                         "categoryId": %d,
                         "phaseId": %d
                     }""".formatted(fixtures.project().id(), fixtures.bug().getId(), phaseId))
               .post("/api/tickets")
               .then()
               .statusCode(201)
               .body("phaseId", equalTo(phaseId))
               .body("phaseName", equalTo("Fase para create ticket"));
    }

    @Test
    @DisplayName("It should not be possible to create a ticket with an invalid project ID")
    void shouldNotCreateTicketWithInvalidProjectIdTest() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Invalid Project Ticket",
                         "description": "This ticket has an invalid project ID.",
                         "projectId": 9999,
                         "categoryId": %d
                     }""".formatted(fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(404)
               .body("message", equalTo("Project does not found! projectId=9999"));
    }

    @Test
    @DisplayName("Should create ticket with custom field values and reject missing required")
    void shouldCreateTicketWithCustomFieldsAndRejectMissingRequired() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        var workflow = Given.simpleWorkflow();
        var project = given().header(fixtures.pmAuthenticatedHeader())
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                     "name": "CF Ticket Project %s",
                                     "description": "Isolated project for CF ticket tests.",
                                     "prefix": "CT%s",
                                     "workflowId": %d
                                   }
                                   """.formatted(suffix, suffix.substring(0, 4).toUpperCase(), workflow.id()))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(dev.vepo.issues.project.ProjectResponse.class);

        var key = "cf_" + suffix;
        var fieldId = given().header(fixtures.pmAuthenticatedHeader())
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                     "key": "%s",
                                     "label": "Sprint",
                                     "type": "INTEGER",
                                     "required": true,
                                     "integerMin": 1,
                                     "integerMax": 99
                                   }
                                   """.formatted(key))
                             .post("/api/projects/%d/custom-fields".formatted(project.id()))
                             .then()
                             .statusCode(201)
                             .extract()
                             .path("id");

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Missing required CF",
                         "description": "Should fail without custom field.",
                         "projectId": %d,
                         "categoryId": %d
                     }""".formatted(project.id(), fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(400);

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Ticket with sprint",
                         "description": "Has required custom field.",
                         "projectId": %d,
                         "categoryId": %d,
                         "customFields": [{"key": "%s", "value": 12}]
                     }""".formatted(project.id(), fixtures.bug().getId(), key))
               .post("/api/tickets")
               .then()
               .statusCode(201)
               .body("customFields.key", org.hamcrest.Matchers.hasItem(key))
               .body("customFields.value", org.hamcrest.Matchers.hasItem(12));

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Sprint",
                       "type": "INTEGER",
                       "required": true,
                       "enabled": false,
                       "integerMin": 1,
                       "integerMax": 99
                     }
                     """.formatted(key))
               .put("/api/projects/%d/custom-fields/%d".formatted(project.id(), fieldId))
               .then()
               .statusCode(200);
    }

    @Test
    @DisplayName("Should accept ticket description when plain text is within 1200 even if HTML is longer")
    void shouldAcceptTicketDescriptionWhenPlainTextIsWithinLimitButHtmlExceedsRawSize() {
        var plain = "a".repeat(1200);
        var htmlDescription = "<p>%s</p>".formatted(plain);
        assert htmlDescription.length() > 1200;

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Rich description ticket",
                         "description": "%s",
                         "projectId": %d,
                         "categoryId": %d
                     }""".formatted(htmlDescription, fixtures.project().id(), fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(201)
               .body("description", equalTo(htmlDescription));
    }

    @Test
    @DisplayName("Should reject ticket description when plain text exceeds 1200 even when wrapped in HTML")
    void shouldRejectTicketDescriptionWhenPlainTextExceedsLimit() {
        var plain = "a".repeat(1201);
        var htmlDescription = "<b>%s</b>".formatted(plain);

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Too long plain description",
                         "description": "%s",
                         "projectId": %d,
                         "categoryId": %d
                     }""".formatted(htmlDescription, fixtures.project().id(), fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should reject ticket description when plain text is below minimum even if HTML is longer")
    void shouldRejectTicketDescriptionWhenPlainTextIsBelowMinimum() {
        var htmlDescription = "<p>hi</p>";

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Short plain description",
                         "description": "%s",
                         "projectId": %d,
                         "categoryId": %d
                     }""".formatted(htmlDescription, fixtures.project().id(), fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should accept TEXT custom field when plain text is within 1200 even if HTML is longer")
    void shouldAcceptTextCustomFieldWhenPlainTextIsWithinLimitButHtmlExceedsRawSize() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        var workflow = Given.simpleWorkflow();
        var project = given().header(fixtures.pmAuthenticatedHeader())
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                     "name": "TEXT CF Project %s",
                                     "description": "Isolated project for TEXT CF plain-text tests.",
                                     "prefix": "TX%s",
                                     "workflowId": %d
                                   }
                                   """.formatted(suffix, suffix.substring(0, 4).toUpperCase(), workflow.id()))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(dev.vepo.issues.project.ProjectResponse.class);

        var key = "notes_" + suffix;
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Notes",
                       "type": "TEXT",
                       "required": false
                     }
                     """.formatted(key))
               .post("/api/projects/%d/custom-fields".formatted(project.id()))
               .then()
               .statusCode(201);

        var plain = "b".repeat(1200);
        var htmlValue = "<p>%s</p>".formatted(plain);
        assert htmlValue.length() > 1200;

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Ticket with long HTML TEXT CF",
                         "description": "Description within limits.",
                         "projectId": %d,
                         "categoryId": %d,
                         "customFields": [{"key": "%s", "value": "%s"}]
                     }""".formatted(project.id(), fixtures.bug().getId(), key, htmlValue))
               .post("/api/tickets")
               .then()
               .statusCode(201)
               .body("customFields.key", org.hamcrest.Matchers.hasItem(key))
               .body("customFields.value", org.hamcrest.Matchers.hasItem(htmlValue));
    }

    @Test
    @DisplayName("Should reject TEXT custom field when plain text exceeds 1200")
    void shouldRejectTextCustomFieldWhenPlainTextExceedsLimit() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        var workflow = Given.simpleWorkflow();
        var project = given().header(fixtures.pmAuthenticatedHeader())
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                     "name": "TEXT CF Over Project %s",
                                     "description": "Isolated project for TEXT CF over-limit tests.",
                                     "prefix": "TO%s",
                                     "workflowId": %d
                                   }
                                   """.formatted(suffix, suffix.substring(0, 4).toUpperCase(), workflow.id()))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(dev.vepo.issues.project.ProjectResponse.class);

        var key = "notes_" + suffix;
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Notes",
                       "type": "TEXT",
                       "required": false
                     }
                     """.formatted(key))
               .post("/api/projects/%d/custom-fields".formatted(project.id()))
               .then()
               .statusCode(201);

        var plain = "c".repeat(1201);
        var htmlValue = "<em>%s</em>".formatted(plain);

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Ticket with overlong TEXT CF",
                         "description": "Description within limits.",
                         "projectId": %d,
                         "categoryId": %d,
                         "customFields": [{"key": "%s", "value": "%s"}]
                     }""".formatted(project.id(), fixtures.bug().getId(), key, htmlValue))
               .post("/api/tickets")
               .then()
               .statusCode(400);
    }
}

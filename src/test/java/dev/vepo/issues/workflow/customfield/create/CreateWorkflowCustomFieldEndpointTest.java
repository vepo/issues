package dev.vepo.issues.workflow.customfield.create;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class CreateWorkflowCustomFieldEndpointTest {

    private WorkflowResponse workflow;
    private Header pmHeader;
    private String suffix;

    @BeforeEach
    void setup() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        pmHeader = Given.authenticatedProjectManager();
        workflow = given().header(pmHeader)
                          .contentType(ContentType.JSON)
                          .body("""
                                {
                                  "name": "CF Workflow %s",
                                  "statuses": ["TODO", "In Progress", "Done"],
                                  "start": "TODO",
                                  "transitions": [
                                    {"from": "TODO", "to": "In Progress"},
                                    {"from": "In Progress", "to": "Done"}
                                  ],
                                  "finishStatuses": [{"status": "Done", "outcome": "DONE"}]
                                }
                                """.formatted(suffix))
                          .post("/api/workflows")
                          .then()
                          .statusCode(201)
                          .extract()
                          .as(WorkflowResponse.class);
    }

    @Test
    @DisplayName("Should create workflow custom field with status-required")
    void shouldCreateWorkflowFieldWithStatusRequired() {
        var key = "res_" + suffix;
        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Resolution notes",
                       "type": "TEXT",
                       "required": false,
                       "statusRequired": ["Done"]
                     }
                     """.formatted(key))
               .post("/api/workflows/%d/custom-fields".formatted(workflow.id()))
               .then()
               .statusCode(201)
               .body("key", equalTo(key))
               .body("type", equalTo("TEXT"))
               .body("workflowId", equalTo((int) workflow.id()))
               .body("statusRequired", hasItem("Done"));
    }

    @Test
    @DisplayName("Should reject unknown status name for status-required")
    void shouldRejectUnknownStatusName() {
        var key = "bad_" + suffix;
        given().header(pmHeader)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Bad",
                       "type": "STRING",
                       "required": false,
                       "stringMaxLength": 20,
                       "statusRequired": ["DoesNotExist"]
                     }
                     """.formatted(key))
               .post("/api/workflows/%d/custom-fields".formatted(workflow.id()))
               .then()
               .statusCode(400);
    }
}

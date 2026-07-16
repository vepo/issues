package dev.vepo.issues.project.list;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.project.ProjectTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ListWritableProjectsEndpointTest {

    private ProjectTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = ProjectTestFixtures.create();
    }

    @Test
    void shouldListOnlyProjectsWritableByMemberOwnerOrAdmin() {
        var memberProjectId = createInternalProject("Writable member");
        var merelyReadableProjectId = createInternalProject("Merely readable");
        Given.addProjectMember(memberProjectId, "user@issues.vepo.dev");

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/writable")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }".formatted(memberProjectId), notNullValue())
               .body("find { it.id == %d }".formatted(merelyReadableProjectId), nullValue());

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/writable")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.id".formatted(memberProjectId), is((int) memberProjectId))
               .body("find { it.id == %d }.id".formatted(merelyReadableProjectId), is((int) merelyReadableProjectId));

        given().header(Given.authenticatedAdmin())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/writable")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.id".formatted(memberProjectId), is((int) memberProjectId))
               .body("find { it.id == %d }.id".formatted(merelyReadableProjectId), is((int) merelyReadableProjectId));
    }

    private long createInternalProject(String name) {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                              "name": "%s %s",
                              "description": "Writable project scope test.",
                              "prefix": "W%s",
                              "workflowId": %d,
                              "securityLevel": "INTERNAL"
                            }
                            """.formatted(name, suffix, suffix.substring(0, 3).toUpperCase(), fixtures.workflow().id()))
                      .post("/api/projects")
                      .then()
                      .statusCode(201)
                      .extract()
                      .jsonPath()
                      .getLong("id");
    }
}

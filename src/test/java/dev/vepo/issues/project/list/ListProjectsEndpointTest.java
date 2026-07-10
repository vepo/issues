package dev.vepo.issues.project.list;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.auth.PasswordEncoder;
import dev.vepo.issues.project.ProjectTestFixtures;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class ListProjectsEndpointTest {

    private WorkflowResponse workflow;
    private Header userAuthenticatedHeader;
    private Header pmAuthenticatedHeader;
    private ProjectTestFixtures fixtures;

    @BeforeEach
    void setup() {
        this.fixtures = ProjectTestFixtures.create();
        this.workflow = fixtures.workflow();
        this.userAuthenticatedHeader = fixtures.userAuthenticatedHeader();
        this.pmAuthenticatedHeader = fixtures.pmAuthenticatedHeader();
    }

    @Test
    @DisplayName("Non authenticated user should not be able to list projects")
    void nonAuthenticatedUserShouldNotListProjectsTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/projects")
               .then()
               .statusCode(401);
    }

    @Test
    @DisplayName("Only authenticated users should be able to list projects")
    void onlyAuthenticatedUsersShouldListProjectsTest() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Project listing is viewable scope: members see memberships; non-members do not")
    void projectListingRespectsViewableMembershipScope() {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        var projectName = "Scoped Project " + suffix;
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "%s",
                                              "description": "This is a test project.",
                                              "prefix": "SP%s",
                                              "workflowId": %d
                                          }""".formatted(projectName, suffix.substring(0, 2), workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .jsonPath();

        var projectId = createdProject.getLong("id");

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.name".formatted(projectId), is(projectName));

        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }".formatted(projectId), nullValue());

        Given.addProjectMember(projectId, "user@issues.vepo.dev");

        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.name".formatted(projectId), is(projectName));
    }

    @Test
    @DisplayName("Project manager who is only a member sees that project in the list")
    void shouldListProjectWhenProjectManagerIsMemberButNotOwner() {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        var projectName = "Member PM Project " + suffix;
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "%s",
                                              "description": "Owned by default PM.",
                                              "prefix": "MP%s",
                                              "workflowId": %d
                                          }""".formatted(projectName, suffix.substring(0, 2), workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .jsonPath();

        var projectId = createdProject.getLong("id");
        var memberPmEmail = "mpm-" + suffix + "@issues.vepo.dev";
        Given.transaction(() -> {
            Given.inject(UserRepository.class)
                 .save(new User("mpm-" + suffix,
                                "Member PM",
                                memberPmEmail,
                                Given.inject(PasswordEncoder.class).hashPassword("password"),
                                Set.of(Role.PROJECT_MANAGER)));
        });
        Given.addProjectMember(projectId, memberPmEmail);

        var memberPmHeader = loginHeader(memberPmEmail);

        given().header(memberPmHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.name".formatted(projectId), is(projectName));
    }

    @Test
    @DisplayName("List projects should expose prefixLocked false without tickets and true with tickets")
    void shouldExposePrefixLockedOnListProjects() {
        var emptySuffix = UUID.randomUUID().toString().substring(0, 6);
        var emptyProjectId = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "List Prefix Empty %s",
                                              "description": "No tickets yet.",
                                              "prefix": "LE%s",
                                              "workflowId": %d
                                          }""".formatted(emptySuffix, emptySuffix.substring(0, 2), workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .jsonPath()
                                    .getLong("id");

        var lockedSuffix = UUID.randomUUID().toString().substring(0, 6);
        var lockedProjectId = given().header(pmAuthenticatedHeader)
                                     .accept(ContentType.JSON)
                                     .when()
                                     .contentType(ContentType.JSON)
                                     .body("""
                                           {
                                               "name": "List Prefix Locked %s",
                                               "description": "Has a ticket.",
                                               "prefix": "LL%s",
                                               "workflowId": %d
                                           }""".formatted(lockedSuffix, lockedSuffix.substring(0, 2), workflow.id()))
                                     .post("/api/projects")
                                     .then()
                                     .statusCode(201)
                                     .extract()
                                     .jsonPath()
                                     .getLong("id");

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Locks list prefixLocked",
                         "description": "Creates prefix lock for list response.",
                         "projectId": %d,
                         "categoryId": %d
                     }""".formatted(lockedProjectId, fixtures.category().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(201);

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.prefixLocked".formatted(emptyProjectId), is(false))
               .body("find { it.id == %d }.prefixLocked".formatted(lockedProjectId), is(true));
    }

    private static Header loginHeader(String email) {
        var response = given().when()
                              .contentType("application/json")
                              .body("""
                                    {
                                        "email": "%s",
                                        "password": "password"
                                    }
                                    """.formatted(email))
                              .post("/api/auth/login")
                              .then()
                              .statusCode(200)
                              .extract()
                              .jsonPath();
        return new Header("Authorization", "Bearer " + response.getString("token"));
    }
}

package dev.vepo.issues.project.serviceaccount;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.auth.LoginResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class ServiceAccountEndpointTest {

    @Test
    @DisplayName("Project manager can create a service account for the project")
    void shouldCreateServiceAccountWhenProjectManager() {
        var project = Given.simpleProject();
        var projectManager = Given.authenticatedProjectManager();

        given().header(projectManager)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "bot-ci"
                     }
                     """)
               .when()
               .post("/api/projects/{projectId}/service-accounts", project.id())
               .then()
               .statusCode(201)
               .body("id", notNullValue())
               .body("name", is("bot-ci"))
               .body("createdAt", notNullValue())
               .body("active", is(true));
    }

    @Test
    @DisplayName("Project manager can list service accounts for the project")
    void shouldListServiceAccountsForProject() {
        var project = Given.simpleProject();
        var projectManager = Given.authenticatedProjectManager();
        createServiceAccount(projectManager, project.id(), "listable-bot");

        given().header(projectManager)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/{projectId}/service-accounts", project.id())
               .then()
               .statusCode(200)
               .body("find { it.name == 'listable-bot' }.id", notNullValue())
               .body("find { it.name == 'listable-bot' }.name", is("listable-bot"))
               .body("find { it.name == 'listable-bot' }.active", is(true))
               .body("find { it.name == 'listable-bot' }.createdAt", notNullValue());
    }

    @Test
    @DisplayName("Generating a service account token returns the secret once with iss_sat_ prefix")
    void shouldGenerateServiceAccountTokenAndReturnSecretOnce() {
        var project = Given.simpleProject();
        var projectManager = Given.authenticatedProjectManager();
        var serviceAccountId = createServiceAccount(projectManager, project.id(), "token-bot");

        given().header(projectManager)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "ci"
                     }
                     """)
               .when()
               .post("/api/projects/{projectId}/service-accounts/{serviceAccountId}/tokens",
                     project.id(),
                     serviceAccountId)
               .then()
               .statusCode(201)
               .body("id", notNullValue())
               .body("name", is("ci"))
               .body("prefix", notNullValue())
               .body("createdAt", notNullValue())
               .body("token", startsWith("iss_sat_"));
    }

    @Test
    @DisplayName("Bearer service account token authenticates as the SA-linked user")
    void shouldAuthenticateWithServiceAccountToken() {
        var project = Given.simpleProject();
        var projectManager = Given.authenticatedProjectManager();
        var serviceAccountId = createServiceAccount(projectManager, project.id(), "auth-bot");
        var secret = createServiceAccountToken(projectManager, project.id(), serviceAccountId, "auth");

        given().header(new Header("Authorization", "Bearer " + secret))
               .accept(ContentType.JSON)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(200)
               .body("username", notNullValue())
               .body("name", is("auth-bot"));
    }

    @Test
    @DisplayName("Revoked service account token is rejected with 401")
    void shouldRejectRevokedServiceAccountToken() {
        var project = Given.simpleProject();
        var projectManager = Given.authenticatedProjectManager();
        var serviceAccountId = createServiceAccount(projectManager, project.id(), "revoke-bot");
        var created = given().header(projectManager)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "revoke-me"
                                   }
                                   """)
                             .when()
                             .post("/api/projects/{projectId}/service-accounts/{serviceAccountId}/tokens",
                                   project.id(),
                                   serviceAccountId)
                             .then()
                             .statusCode(201)
                             .extract();
        var secret = (String) created.path("token");
        long tokenId = ((Number) created.path("id")).longValue();

        given().header(projectManager)
               .when()
               .delete("/api/projects/{projectId}/service-accounts/{serviceAccountId}/tokens/{tokenId}",
                       project.id(),
                       serviceAccountId,
                       tokenId)
               .then()
               .statusCode(204);

        given().header(new Header("Authorization", "Bearer " + secret))
               .accept(ContentType.JSON)
               .when()
               .get("/api/auth/me")
               .then()
               .statusCode(401);
    }

    @Test
    @DisplayName("Non-member cannot manage project service accounts")
    void shouldRejectServiceAccountManagementByNonMember() {
        var project = Given.simpleProject();
        var nonMember = authenticatedNonMember();

        given().header(nonMember)
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "forbidden-bot"
                     }
                     """)
               .when()
               .post("/api/projects/{projectId}/service-accounts", project.id())
               .then()
               .statusCode(403);
    }

    private static long createServiceAccount(Header projectManager, long projectId, String name) {
        return ((Number) given().header(projectManager)
                                .contentType(ContentType.JSON)
                                .body("""
                                      {
                                          "name": "%s"
                                      }
                                      """.formatted(name))
                                .when()
                                .post("/api/projects/{projectId}/service-accounts", projectId)
                                .then()
                                .statusCode(201)
                                .extract()
                                .path("id")).longValue();
    }

    private static String createServiceAccountToken(Header projectManager,
                                                    long projectId,
                                                    long serviceAccountId,
                                                    String name) {
        return given().header(projectManager)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "name": "%s"
                            }
                            """.formatted(name))
                      .when()
                      .post("/api/projects/{projectId}/service-accounts/{serviceAccountId}/tokens",
                            projectId,
                            serviceAccountId)
                      .then()
                      .statusCode(201)
                      .extract()
                      .path("token");
    }

    private static Header authenticatedNonMember() {
        var outsider = Given.randomUser();
        var response = given().when()
                              .contentType(ContentType.JSON)
                              .body("""
                                    {
                                        "email": "%s",
                                        "password": "password"
                                    }
                                    """.formatted(outsider.getEmail()))
                              .post("/api/auth/login")
                              .then()
                              .statusCode(200)
                              .extract()
                              .as(LoginResponse.class);
        return new Header("Authorization", "Bearer " + response.token());
    }
}

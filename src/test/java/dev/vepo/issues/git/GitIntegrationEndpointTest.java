package dev.vepo.issues.git;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.auth.LoginResponse;
import dev.vepo.issues.auth.PasswordEncoder;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.project.ProjectTestFixtures;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class GitIntegrationEndpointTest {

    @Test
    @DisplayName("Owner can associate git repository and regenerate webhook secret")
    void shouldAssociateGitRepositoryAndRegenerateSecret() {
        var project = Given.simpleProject();
        var pm = Given.authenticatedProjectManager();

        var created = given().header(pm)
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                     "remoteUrl": "https://github.com/org/repo",
                                     "provider": "GITHUB",
                                     "defaultBranch": "main"
                                   }
                                   """)
                             .when()
                             .put("/api/projects/{id}/git", project.id())
                             .then()
                             .statusCode(200)
                             .body("remoteUrl", is("https://github.com/org/repo"))
                             .body("provider", is("GITHUB"))
                             .body("defaultBranch", is("main"))
                             .body("hasSecret", is(true))
                             .body("webhookSecret", notNullValue())
                             .body("webhookUrl", notNullValue())
                             .extract()
                             .jsonPath();

        var firstSecret = created.getString("webhookSecret");

        given().header(pm)
               .when()
               .get("/api/projects/{id}/git", project.id())
               .then()
               .statusCode(200)
               .body("hasSecret", is(true))
               .body("webhookSecret", nullValue());

        given().header(pm)
               .when()
               .post("/api/projects/{id}/git/regenerate-secret", project.id())
               .then()
               .statusCode(200)
               .body("webhookSecret", notNullValue())
               .body("webhookSecret", org.hamcrest.Matchers.not(equalTo(firstSecret)));
    }

    @Test
    @DisplayName("Inbound API links commits mentioning ticket identifiers idempotently")
    void shouldIngestCommitsViaApiIdempotently() {
        var fixtures = ProjectTestFixtures.create();
        var project = createProject(fixtures);
        var pm = fixtures.pmAuthenticatedHeader();
        associateGit(pm, project);
        var ticket = createTicket(pm, project.id(), fixtures.category().getId());

        var body = """
                   {
                     "commits": [
                       {
                         "sha": "abc123def456",
                         "message": "fix: handle null (%s)",
                         "authorName": "Alice",
                         "authorEmail": "user@issues.vepo.dev",
                         "committedAt": "2026-07-16T12:00:00Z",
                         "commitUrl": "https://github.com/org/repo/commit/abc123def456"
                       }
                     ]
                   }
                   """.formatted(ticket.identifier());

        given().header(pm)
               .contentType(ContentType.JSON)
               .body(body)
               .when()
               .post("/api/projects/{id}/git/commits", project.id())
               .then()
               .statusCode(200)
               .body("linked", is(1))
               .body("skippedDuplicates", is(0));

        given().header(pm)
               .contentType(ContentType.JSON)
               .body(body)
               .when()
               .post("/api/projects/{id}/git/commits", project.id())
               .then()
               .statusCode(200)
               .body("linked", is(0))
               .body("skippedDuplicates", greaterThanOrEqualTo(1));

        given().header(pm)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/{id}/expanded", ticket.id())
               .then()
               .statusCode(200)
               .body("linkedCommits.size()", is(1))
               .body("linkedCommits[0].sha", is("abc123def456"))
               .body("linkedCommits[0].commitUrl", notNullValue());
    }

    @Test
    @DisplayName("Webhook accepts valid HMAC and rejects invalid signature")
    void shouldAcceptValidWebhookHmac() {
        var fixtures = ProjectTestFixtures.create();
        var project = createProject(fixtures);
        var pm = fixtures.pmAuthenticatedHeader();
        var secret = associateGit(pm, project);
        var ticket = createTicket(pm, project.id(), fixtures.category().getId());

        var payload = """
                      {
                        "commits": [
                          {
                            "id": "deadbeef01",
                            "message": "chore: %s",
                            "timestamp": "2026-07-16T13:00:00Z",
                            "url": "https://github.com/org/repo/commit/deadbeef01",
                            "author": { "name": "Bob", "email": "bob@example.com" }
                          }
                        ]
                      }
                      """.formatted(ticket.identifier());
        var bodyBytes = payload.getBytes(StandardCharsets.UTF_8);
        var signature = "sha256=" + hmacSha256Hex(secret, bodyBytes);

        given().contentType(ContentType.JSON)
               .header("X-Hub-Signature-256", signature)
               .body(payload)
               .when()
               .post("/api/projects/{id}/git/webhook", project.id())
               .then()
               .statusCode(204);

        given().contentType(ContentType.JSON)
               .header("X-Hub-Signature-256", "sha256=deadbeef")
               .body(payload)
               .when()
               .post("/api/projects/{id}/git/webhook", project.id())
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("Non-owner project manager cannot configure git association")
    void shouldForbidNonOwnerFromConfiguringGit() {
        var project = Given.simpleProject();
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        var email = "git-opm-" + suffix + "@issues.vepo.dev";
        Given.transaction(() -> {
            Given.inject(UserRepository.class)
                 .save(new User("gopm-" + suffix,
                                "Other",
                                email,
                                Given.inject(PasswordEncoder.class).hashPassword("password"),
                                Set.of(Role.PROJECT_MANAGER)));
            return null;
        });
        var login = given().contentType(ContentType.JSON)
                           .body("""
                                 {"email":"%s","password":"password"}
                                 """.formatted(email))
                           .post("/api/auth/login")
                           .then()
                           .statusCode(200)
                           .extract()
                           .as(LoginResponse.class);
        var header = new Header("Authorization", "Bearer " + login.token());

        given().header(header)
               .contentType(ContentType.JSON)
               .body("""
                     {"remoteUrl":"https://github.com/org/other","provider":"GITHUB"}
                     """)
               .when()
               .put("/api/projects/{id}/git", project.id())
               .then()
               .statusCode(403);
    }

    private static ProjectResponse createProject(ProjectTestFixtures fixtures) {
        var suffix = UUID.randomUUID().toString().substring(0, 6);
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                              "name": "Git Project %s",
                              "description": "Git integration test project.",
                              "prefix": "G%s",
                              "workflowId": %d
                            }
                            """.formatted(suffix, suffix.substring(0, 2).toUpperCase(), fixtures.workflow().id()))
                      .post("/api/projects")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(ProjectResponse.class);
    }

    private static TicketResponse createTicket(Header pm, long projectId, long categoryId) {
        var title = "Git ticket " + UUID.randomUUID();
        return given().header(pm)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                              "title": "%s",
                              "description": "Git integration test ticket.",
                              "projectId": %d,
                              "categoryId": %d
                            }
                            """.formatted(title, projectId, categoryId))
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }

    private static String associateGit(Header pm, ProjectResponse project) {
        return given().header(pm)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                              "remoteUrl": "https://github.com/org/repo",
                              "provider": "GITHUB",
                              "defaultBranch": "main"
                            }
                            """)
                      .when()
                      .put("/api/projects/{id}/git", project.id())
                      .then()
                      .statusCode(200)
                      .extract()
                      .jsonPath()
                      .getString("webhookSecret");
    }

    private static String hmacSha256Hex(String secret, byte[] body) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}

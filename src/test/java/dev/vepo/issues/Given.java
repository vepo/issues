package dev.vepo.issues;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.auth.AuthResponse;
import dev.vepo.issues.auth.LoginResponse;
import dev.vepo.issues.auth.PasswordEncoder;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.project.Project;
import dev.vepo.issues.project.ProjectMemberRepository;
import dev.vepo.issues.project.ProjectRepository;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import dev.vepo.issues.workflow.StatusResponse;
import dev.vepo.issues.workflow.WorkflowRepository;
import dev.vepo.issues.workflow.WorkflowResponse;
import dev.vepo.issues.workflow.WorkflowStatus;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import jakarta.enterprise.inject.spi.CDI;

public class Given {
    private static final Logger logger = LoggerFactory.getLogger(Given.class);

    public static Header authenticatedUser() {
        ensureUser("user", "User", "user@issues.vepo.dev", Set.of(Role.USER));
        var response = given().when()
                              .contentType("application/json")
                              .body("""
                                    {
                                        "email": "user@issues.vepo.dev",
                                        "password": "password"
                                    }
                                    """)
                              .post("/api/auth/login")
                              .then()
                              .statusCode(200)
                              .extract()
                              .as(LoginResponse.class);
        return new Header("Authorization", "Bearer " + response.token());
    }

    public static AuthResponse me(Header authentication) {
        return given().header(authentication)
                      .when()
                      .accept(ContentType.JSON)
                      .get("/api/auth/me")
                      .then()
                      .statusCode(200)
                      .extract()
                      .as(AuthResponse.class);
    }

    public static Header authenticatedAdmin() {
        ensureUser("admin", "Admin", "sysadmin@issues.ui", Set.of(Role.ADMIN));
        var response = given().when()
                              .contentType("application/json")
                              .body("""
                                    {
                                        "email": "sysadmin@issues.ui",
                                        "password": "qwas1234"
                                    }
                                    """)
                              .post("/api/auth/login")
                              .then()
                              .statusCode(200)
                              .extract()
                              .as(LoginResponse.class);
        return new Header("Authorization", "Bearer " + response.token());
    }

    public static User randomUser() {
        var id = SEQUENCE.incrementAndGet();
        String email = "user" + id + "@issues.vepo.dev";
        ensureUser("user-" + id, "Random User " + id, email, Set.of(Role.USER));
        return inject(UserRepository.class).findByEmail(email)
                                           .orElseThrow(() -> new IllegalStateException("User created!"));
    }

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    public static User randomUser(String email) {
        var id = SEQUENCE.incrementAndGet();
        ensureUser("user-" + id, "Random User " + id, email, Set.of(Role.USER));
        return inject(UserRepository.class).findByEmail(email)
                                           .orElseThrow(() -> new IllegalStateException("User created!"));
    }

    public static Header authenticatedProjectManager() {
        ensureUser("project-manager", "PM", "pm@issues.vepo.dev", Set.of(Role.PROJECT_MANAGER));
        var response = given().when()
                              .contentType("application/json")
                              .body("""
                                    {
                                        "email": "pm@issues.vepo.dev",
                                        "password": "password"
                                    }
                                    """)
                              .post("/api/auth/login")
                              .then()
                              .statusCode(200)
                              .extract()
                              .as(LoginResponse.class);
        return new Header("Authorization", "Bearer " + response.token());
    }

    public static TicketResponse simpleTicket(Long projectId, long categoryId) {
        var existingTickets = given().when()
                                     .header(authenticatedProjectManager())
                                     .get("/api/tickets")
                                     .then()
                                     .statusCode(200)
                                     .extract()
                                     .as(TicketResponse[].class);
        if (existingTickets.length > 0 && Stream.of(existingTickets)
                                                .anyMatch(t -> "Test Ticket".equals(t.title()))) {
            return Stream.of(existingTickets)
                         .filter(t -> "Test Ticket".equals(t.title()))
                         .findFirst()
                         .orElseThrow();
        }
        return given().when()
                      .contentType("application/json")
                      .header(authenticatedProjectManager())
                      .body("""
                            {
                                "title": "Test Ticket",
                                "description": "This is a test ticket.",
                                "projectId": %d,
                                "categoryId": %d
                            }""".formatted(projectId, categoryId))
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }

    public static ProjectResponse simpleProject() {
        var workflow = simpleWorkflow();
        var existingProjects = given().when()
                                      .header(authenticatedProjectManager())
                                      .get("/api/projects")
                                      .then()
                                      .statusCode(200)
                                      .extract()
                                      .as(ProjectResponse[].class);
        if (existingProjects.length > 0 && Stream.of(existingProjects)
                                                 .anyMatch(p -> "Test Project".equals(p.name()))) {
            var project = Stream.of(existingProjects)
                                .filter(p -> "Test Project".equals(p.name()))
                                .findFirst()
                                .orElseThrow();
            addProjectMember(project.id(), "user@issues.vepo.dev");
            return project;
        }
        var project = given().when()
                             .contentType("application/json")
                             .header(authenticatedProjectManager())
                             .body("""
                                   {
                                       "name": "Test Project",
                                       "description": "This is a test project.",
                                       "prefix": "PRJ%d",
                                       "workflowId": %d
                                   }""".formatted(SEQUENCE.incrementAndGet(), workflow.id()))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(ProjectResponse.class);
        addProjectMember(project.id(), "user@issues.vepo.dev");
        return project;
    }

    public static void addProjectMember(long projectId, String email) {
        addProjectMember(projectId, userIdByEmail(email));
    }

    public static void addProjectMember(long projectId, long userId) {
        transaction(() -> {
            var memberRepository = inject(ProjectMemberRepository.class);
            if (memberRepository.isMember(projectId, userId)) {
                return;
            }
            var project = inject(ProjectRepository.class).findById(projectId).orElseThrow();
            var user = inject(UserRepository.class).findById(userId).orElseThrow();
            memberRepository.addMember(project, user);
        });
    }

    public static WorkflowResponse simpleWorkflow() {
        // try get the value before creating it
        var existingWorkflow = given().header(authenticatedUser())
                                      .when()
                                      .get("/api/workflows")
                                      .then()
                                      .statusCode(200)
                                      .extract()
                                      .as(WorkflowResponse[].class);
        if (existingWorkflow.length > 0 && Stream.of(existingWorkflow)
                                                 .anyMatch(w -> "Project".equals(w.name()))) {
            return Stream.of(existingWorkflow)
                         .filter(w -> "Project".equals(w.name()))
                         .findFirst()
                         .orElseThrow();
        }
        return given().header(authenticatedProjectManager())
                      .when()
                      .contentType("application/json")
                      .body("""
                            {
                                "name": "Project",
                                "statuses": ["TODO", "In Progress", "Blocked", "Done"],
                                "start": "TODO",
                                "transitions": [
                                    {"from": "TODO", "to": "In Progress"},
                                    {"from": "In Progress", "to": "Blocked"},
                                    {"from": "Blocked", "to": "In Progress"},
                                    {"from": "In Progress", "to": "Done"},
                                    {"from": "Done", "to": "In Progress"}
                                ],
                                "finishStatuses": [
                                    {"status": "Done", "outcome": "DONE"}
                                ]
                            }""")
                      .post("/api/workflows")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(WorkflowResponse.class);

    }

    public static int userIdByEmail(String email) {
        return inject(UserRepository.class).findByEmail(email)
                                           .map(User::getId)
                                           .orElseThrow(() -> new IllegalStateException("User not found with email: " + email))
                                           .intValue();
    }

    public static List<StatusResponse> allStatuses() {
        return Arrays.asList(given().header(authenticatedProjectManager())
                                    .when()
                                    .get("/api/status")
                                    .then()
                                    .statusCode(200)
                                    .extract()
                                    .as(StatusResponse[].class));
    }

    private static void ensureUser(String username, String name, String email, Set<Role> roles) {
        logger.info("Creating user: username: {} name: {} email: {} roles: {}", username, name, email, roles);
        if (!inject(UserRepository.class).findByEmail(email).isPresent()) {
            transaction(() -> inject(UserRepository.class)
                                                          .save(new User(username,
                                                                         name,
                                                                         email,
                                                                         inject(PasswordEncoder.class).hashPassword("password"),
                                                                         roles)));
        }
    }

    public static <T> T inject(Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }

    public static void transaction(Runnable code) {
        try {
            QuarkusTransaction.begin();
            code.run();
            QuarkusTransaction.commit();
        } catch (Exception e) {
            QuarkusTransaction.rollback();
            fail("Fail to create transaction!", e);
        }
    }

    public static <T> T transaction(Supplier<T> code) {
        try {
            QuarkusTransaction.begin();
            T value = code.get();
            QuarkusTransaction.commit();
            return value;
        } catch (Exception e) {
            QuarkusTransaction.rollback();
            fail("Fail to create transaction!", e);
            return null;
        }
    }

    public static Category category(long categoryId) {
        return inject(CategoryRepository.class).findById(categoryId)
                                               .orElseThrow();
    }

    public static Project project(long projectId) {
        return inject(ProjectRepository.class).findById(projectId)
                                              .orElseThrow();
    }

    public static User user(String email) {
        return inject(UserRepository.class).findByEmail(email)
                                           .orElseGet(() -> randomUser(email));
    }

    public static WorkflowStatus status(String status) {
        return inject(WorkflowRepository.class).findStatusByName(status)
                                               .orElseThrow();
    }

    public static void withoutTicket(long id) {
        transaction(() -> inject(TicketRepository.class).delete(id));
    }
}

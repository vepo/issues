package dev.vepo.issues.ticket.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketTestFixtures;
import dev.vepo.issues.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
class TicketQueryLanguageServiceTest {

    @Inject
    TicketQueryLanguageService queryLanguageService;

    @Inject
    UserRepository userRepository;

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should reject invalid query syntax")
    void shouldRejectInvalidQuerySyntax() {
        assertThatThrownBy(() -> queryLanguageService.parse("project ="))
                                                                         .isInstanceOf(InvalidQueryException.class);
    }

    @Test
    @DisplayName("Should search tickets by project name")
    void shouldSearchTicketsByProjectName() {
        var project = Given.simpleProject();
        var user = userRepository.findByEmail("user@issues.vepo.dev").orElseThrow();
        var results = queryLanguageService.execute("project = \"%s\"".formatted(project.name()), user);
        assertThat(results).isNotEmpty();
    }

    @Test
    @DisplayName("Should search tickets by due date")
    void shouldSearchTicketsByDueDate() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "title": "Due Date Query Ticket",
                         "description": "Ticket for due date query test.",
                         "projectId": %d,
                         "categoryId": %d,
                         "dueDate": "2026-08-20"
                     }""".formatted(fixtures.project().id(), fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(201);

        var user = userRepository.findByEmail("user@issues.vepo.dev").orElseThrow();
        var results = queryLanguageService.execute("dueDate = \"2026-08-20\"", user);
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(ticket -> java.time.LocalDate.parse("2026-08-20").equals(ticket.getDueDate()));
    }
}

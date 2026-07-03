package dev.vepo.issues.ticket.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class TicketQueryLanguageServiceTest {

    @Inject
    TicketQueryLanguageService queryLanguageService;

    @Inject
    UserRepository userRepository;

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
}

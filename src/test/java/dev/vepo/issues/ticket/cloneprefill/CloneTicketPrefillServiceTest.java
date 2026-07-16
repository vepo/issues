package dev.vepo.issues.ticket.cloneprefill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class CloneTicketPrefillServiceTest {

    @Inject
    CloneTicketPrefillService cloneTicketPrefillService;

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    void shouldCopyOnlyIntrinsicFieldsIntoTargetAwarePrefill() {
        var prefill = cloneTicketPrefillService.getPrefill(fixtures.ticket().id(),
                                                           fixtures.project().id(),
                                                           "project-manager");

        assertEquals(fixtures.ticket().identifier(), prefill.sourceIdentifier());
        assertEquals(fixtures.project().id(), prefill.targetProjectId());
        assertEquals(fixtures.ticket().title(), prefill.title());
        assertEquals(fixtures.ticket().description(), prefill.description());
        assertEquals(fixtures.ticket().category(), prefill.categoryId());
        assertEquals(fixtures.ticket().priority(), prefill.priority());
        assertEquals(fixtures.ticket().ticketType(), prefill.ticketType());
        assertTrue(prefill.customFields().isEmpty());
        assertTrue(prefill.warnings().isEmpty());
    }
}

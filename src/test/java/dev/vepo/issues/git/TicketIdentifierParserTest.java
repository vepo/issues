package dev.vepo.issues.git;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TicketIdentifierParserTest {

    @Test
    void shouldFindCaseInsensitivePrefixMatches() {
        var ids = TicketIdentifierParser.findIdentifiers("ISS", "fix(auth): ISS-003 and iss-004 in body");
        assertThat(ids).containsExactly("ISS-003", "ISS-004");
    }

    @Test
    void shouldIgnoreUnrelatedWords() {
        var ids = TicketIdentifierParser.findIdentifiers("ISS", "MISSION-1 is not a ticket");
        assertThat(ids).isEmpty();
    }
}

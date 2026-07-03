package dev.vepo.issues.ticket.history;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HistoryDisplayTest {

    @Test
    void shouldFormatUnderscoreStatusNames() {
        assertEquals("In Progress", HistoryDisplay.formatStatus("IN_PROGRESS"));
        assertEquals("Todo", HistoryDisplay.formatStatus("TODO"));
        assertEquals("Request Information", HistoryDisplay.formatStatus("REQUEST_INFORMATION"));
    }
}

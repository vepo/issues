package dev.vepo.issues.ticket.export;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.ticket.TicketType;

class TicketJsonWriterTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-07-17T11:33:45Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(GENERATED_AT, ZoneOffset.UTC);
    private static final List<String> STABLE_TICKET_FIELDS = List.of("identifier",
                                                                     "title",
                                                                     "description",
                                                                     "projectKey",
                                                                     "projectName",
                                                                     "statusCode",
                                                                     "statusName",
                                                                     "categoryId",
                                                                     "categoryName",
                                                                     "priority",
                                                                     "type",
                                                                     "authorEmail",
                                                                     "authorName",
                                                                     "assigneeEmail",
                                                                     "assigneeName",
                                                                     "phaseId",
                                                                     "phaseName",
                                                                     "observedVersionId",
                                                                     "observedVersionName",
                                                                     "targetVersionId",
                                                                     "targetVersionName",
                                                                     "storyPoints",
                                                                     "dueDate",
                                                                     "createdAt",
                                                                     "updatedAt",
                                                                     "customFields");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldWriteSchemaVersionOneEnvelopeWithGeneratedAtSourceAndCount() throws Exception {
        var tickets = List.of(row("ISS-1", "First", "Description", Map.of()),
                              row("ISS-2", "Second", "Description", Map.of()));

        var document = write(ExportSource.SIMPLE_SEARCH, tickets);

        assertThat(TicketExportDocument.class).isRecord();
        assertThat(document.fieldNames()).toIterable()
                                         .containsExactly("schemaVersion", "generatedAt", "source", "count", "tickets");
        assertThat(document.path("schemaVersion").intValue()).isEqualTo(1);
        assertThat(document.path("generatedAt").textValue()).isEqualTo("2026-07-17T11:33:45Z");
        assertThat(document.path("source").textValue()).isEqualTo("SIMPLE_SEARCH");
        assertThat(document.path("count").intValue()).isEqualTo(2);
        assertThat(document.path("tickets")).hasSize(2);
    }

    @Test
    void shouldWriteStableTicketFieldsNullsTypedCustomFieldsAndPlainTextRichFields() throws Exception {
        var customFields = new LinkedHashMap<String, Object>();
        customFields.put("richText", "<p>First <strong>line</strong></p><p>Second &amp; final</p>");
        customFields.put("enabled", true);
        customFields.put("estimate", 13);
        customFields.put("releaseDate", LocalDate.of(2026, Month.AUGUST, 1));
        customFields.put("reviewedAt", LocalDateTime.of(2026, Month.JULY, 17, 8, 45, 30));
        customFields.put("unset", null);

        var ticket = row("ISS-7",
                         "JSON contract",
                         "<h2>Overview</h2><p>Ship &lt;safely&gt;<br>today</p>",
                         customFields);

        var exportedTicket = write(ExportSource.ADVANCED_QUERY, List.of(ticket)).path("tickets").get(0);

        assertThat(exportedTicket.fieldNames()).toIterable().containsExactlyElementsOf(STABLE_TICKET_FIELDS);
        assertThat(exportedTicket.path("identifier").textValue()).isEqualTo("ISS-7");
        assertThat(exportedTicket.path("description").textValue()).isEqualTo("Overview\nShip <safely>\ntoday");
        assertThat(exportedTicket.path("priority").textValue()).isEqualTo("HIGH");
        assertThat(exportedTicket.path("type").textValue()).isEqualTo("STORY");
        assertThat(exportedTicket.path("dueDate").textValue()).isEqualTo("2026-07-31");
        assertThat(exportedTicket.path("createdAt").textValue()).isEqualTo("2026-07-16T10:20:30");
        assertThat(exportedTicket.path("updatedAt").textValue()).isEqualTo("2026-07-17T11:21:31");
        assertThat(exportedTicket.path("categoryId").isNull()).isTrue();
        assertThat(exportedTicket.path("assigneeEmail").isNull()).isTrue();
        assertThat(exportedTicket.path("phaseId").isNull()).isTrue();

        var exportedCustomFields = exportedTicket.path("customFields");
        assertThat(exportedCustomFields.path("richText").textValue()).isEqualTo("First line\nSecond & final");
        assertThat(exportedCustomFields.path("enabled").booleanValue()).isTrue();
        assertThat(exportedCustomFields.path("estimate").intValue()).isEqualTo(13);
        assertThat(exportedCustomFields.path("releaseDate").textValue()).isEqualTo("2026-08-01");
        assertThat(exportedCustomFields.path("reviewedAt").textValue()).isEqualTo("2026-07-17T08:45:30");
        assertThat(exportedCustomFields.path("unset").isNull()).isTrue();
    }

    @Test
    void shouldWriteTicketsInGivenOrderAndCustomFieldKeysInLexicalOrder() throws Exception {
        var firstCustomFields = new LinkedHashMap<String, Object>();
        firstCustomFields.put("zeta", "last");
        firstCustomFields.put("alpha", "first");
        firstCustomFields.put("middle", "between");
        var secondCustomFields = new LinkedHashMap<String, Object>();
        secondCustomFields.put("beta", 2);
        secondCustomFields.put("alpha", 1);

        var document = write(ExportSource.SAVED_QUERY,
                             List.of(row("ISS-20", "First", null, firstCustomFields),
                                     row("ISS-3", "Second", null, secondCustomFields)));
        var tickets = document.path("tickets");

        assertThat(tickets.get(0).path("identifier").textValue()).isEqualTo("ISS-20");
        assertThat(tickets.get(1).path("identifier").textValue()).isEqualTo("ISS-3");
        assertThat(tickets.get(0).path("customFields").fieldNames()).toIterable()
                                                                    .containsExactly("alpha", "middle", "zeta");
        assertThat(tickets.get(1).path("customFields").fieldNames()).toIterable()
                                                                    .containsExactly("alpha", "beta");
    }

    private JsonNode write(ExportSource source, List<TicketExportRow> tickets) throws Exception {
        var output = new ByteArrayOutputStream();
        new TicketJsonWriter(objectMapper, FIXED_CLOCK).write(source, tickets, output);
        return objectMapper.readTree(output.toString(UTF_8));
    }

    private static TicketExportRow row(String identifier,
                                       String title,
                                       String description,
                                       Map<String, Object> customFields) {
        return new TicketExportRow(identifier,
                                   title,
                                   description,
                                   "ISS",
                                   "Issues",
                                   "IN_PROGRESS",
                                   "In Progress",
                                   null,
                                   null,
                                   TicketPriority.HIGH,
                                   TicketType.STORY,
                                   "author@example.test",
                                   "Ticket Author",
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   8,
                                   LocalDate.of(2026, Month.JULY, 31),
                                   LocalDateTime.of(2026, Month.JULY, 16, 10, 20, 30),
                                   LocalDateTime.of(2026, Month.JULY, 17, 11, 21, 31),
                                   customFields);
    }
}

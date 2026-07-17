package dev.vepo.issues.ticket.export;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.opencsv.CSVReader;

import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.ticket.TicketType;

class TicketCsvWriterTest {

    private static final List<String> STABLE_HEADERS = List.of("identifier",
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
                                                               "updatedAt");

    @Test
    void shouldWriteUtf8Rfc4180CsvWithStableHeadersFollowedBySortedCustomFieldUnion() throws Exception {
        var first = row("ISS-1", "Ação pronta", "Descrição", Map.of("zeta", "last", "alpha", "first"));
        var second = row("ISS-2", "Revisão", null, Map.of("middle", "value"));

        var csvBytes = write(first, second);
        var csv = new String(csvBytes, UTF_8);
        var records = read(csv);

        assertThat(csvBytes).isEqualTo(csv.getBytes(UTF_8));
        assertThat(csv).contains("Ação pronta", "Descrição", "Revisão");
        assertThat(csv.replace("\r\n", "")).doesNotContain("\r", "\n");
        assertThat(records.getFirst()).containsExactlyElementsOf(headers("alpha", "middle", "zeta"));
        assertThat(records.get(1)).endsWith("first", "", "last");
        assertThat(records.get(2)).endsWith("", "value", "");
    }

    @Test
    void shouldWriteNullAsEmptyAndEscapeCommasQuotesAndPlainTextNewlines() throws Exception {
        var ticket = row("ISS-3",
                         "Ship, \"today\"",
                         "<p>First <strong>line</strong></p><p>Second &amp; final</p>",
                         Map.of("notes", "<p>Use \"safe\" mode</p><p>Then ship</p>"));

        var csv = new String(write(ticket), UTF_8);
        var ticketRecord = read(csv).get(1);

        assertThat(csv).contains("\"Ship, \"\"today\"\"\"",
                                 "\"First line\nSecond & final\"",
                                 "\"Use \"\"safe\"\" mode\nThen ship\"");
        assertThat(ticketRecord[1]).isEqualTo("Ship, \"today\"");
        assertThat(ticketRecord[2]).isEqualTo("First line\nSecond & final");
        assertThat(ticketRecord[8]).isEmpty();
        assertThat(ticketRecord[25]).isEqualTo("Use \"safe\" mode\nThen ship");
    }

    @Test
    void shouldNeutralizeFormulaStringsWithoutCorruptingTypedCustomValues() throws Exception {
        var ticket = row("ISS-4",
                         "=HYPERLINK(\"https://example.test\")",
                         "+cmd",
                         Map.of("atText",
                                "@lookup",
                                "boolean",
                                true,
                                "date",
                                LocalDate.of(2026, Month.JULY, 17),
                                "dateTime",
                                LocalDateTime.of(2026, Month.JULY, 17, 8, 25, 30),
                                "formula",
                                "=1+1",
                                "negativeNumber",
                                -42,
                                "negativeText",
                                "-42",
                                "positiveText",
                                "+42"));

        var ticketRecord = read(new String(write(ticket), UTF_8)).get(1);

        assertThat(ticketRecord[1]).isEqualTo("'=HYPERLINK(\"https://example.test\")");
        assertThat(ticketRecord[2]).isEqualTo("'+cmd");
        assertThat(ticketRecord[25]).isEqualTo("'@lookup");
        assertThat(ticketRecord[26]).isEqualTo("true");
        assertThat(ticketRecord[27]).isEqualTo("2026-07-17");
        assertThat(ticketRecord[28]).isEqualTo("2026-07-17T08:25:30");
        assertThat(ticketRecord[29]).isEqualTo("'=1+1");
        assertThat(ticketRecord[30]).isEqualTo("-42");
        assertThat(ticketRecord[31]).isEqualTo("'-42");
        assertThat(ticketRecord[32]).isEqualTo("'+42");
    }

    private static byte[] write(TicketExportRow... rows) throws Exception {
        var output = new ByteArrayOutputStream();
        new TicketCsvWriter().write(List.of(rows), output);
        return output.toByteArray();
    }

    private static List<String[]> read(String csv) throws Exception {
        try (var reader = new CSVReader(new StringReader(csv))) {
            return reader.readAll();
        }
    }

    private static List<String> headers(String... customKeys) {
        var headers = new ArrayList<>(STABLE_HEADERS);
        for (var customKey : customKeys) {
            headers.add("custom.%s".formatted(customKey));
        }
        return headers;
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

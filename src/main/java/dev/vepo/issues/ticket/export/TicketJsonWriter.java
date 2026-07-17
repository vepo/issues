package dev.vepo.issues.ticket.export;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TicketJsonWriter {

    private static final int SCHEMA_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Inject
    public TicketJsonWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void write(ExportSource source, List<TicketExportRow> rows, OutputStream output) throws IOException {
        var tickets = rows.stream()
                          .map(TicketJsonWriter::toTicket)
                          .toList();
        var document = new TicketExportDocument(SCHEMA_VERSION,
                                                clock.instant(),
                                                source,
                                                tickets.size(),
                                                tickets);
        objectMapper.writer()
                    .without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .writeValue(new CallerOwnedOutputStream(output), document);
    }

    private static TicketExportDocument.Ticket toTicket(TicketExportRow row) {
        return new TicketExportDocument.Ticket(row.identifier(),
                                               row.title(),
                                               TicketExportValueConverter.toJsonPlainText(row.description()),
                                               row.projectKey(),
                                               row.projectName(),
                                               row.statusCode(),
                                               row.statusName(),
                                               row.categoryId(),
                                               row.categoryName(),
                                               row.priority(),
                                               row.type(),
                                               row.authorEmail(),
                                               row.authorName(),
                                               row.assigneeEmail(),
                                               row.assigneeName(),
                                               row.phaseId(),
                                               row.phaseName(),
                                               row.observedVersionId(),
                                               row.observedVersionName(),
                                               row.targetVersionId(),
                                               row.targetVersionName(),
                                               row.storyPoints(),
                                               row.dueDate(),
                                               row.createdAt(),
                                               row.updatedAt(),
                                               sortedCustomFields(row.customFields()));
    }

    private static Map<String, Object> sortedCustomFields(Map<String, Object> customFields) {
        var sorted = new TreeMap<String, Object>();
        for (var entry : customFields.entrySet()) {
            var value = entry.getValue();
            sorted.put(entry.getKey(),
                       value instanceof String text ? TicketExportValueConverter.toJsonPlainText(text) : value);
        }
        return new LinkedHashMap<>(sorted);
    }
}

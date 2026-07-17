package dev.vepo.issues.ticket.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TicketCsvWriter {

    private static final List<String> BUILT_IN_HEADERS = List.of("identifier",
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

    public void write(List<TicketExportRow> rows, OutputStream output) throws IOException {
        var customKeys = collectSortedCustomKeys(rows);
        var outputWriter = new OutputStreamWriter(new CallerOwnedOutputStream(output), StandardCharsets.UTF_8);
        try (var csvWriter = new CSVWriter(outputWriter,
                                           ICSVWriter.DEFAULT_SEPARATOR,
                                           ICSVWriter.DEFAULT_QUOTE_CHARACTER,
                                           ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                                           ICSVWriter.RFC4180_LINE_END)) {
            csvWriter.writeNext(headers(customKeys), false);
            for (var row : rows) {
                csvWriter.writeNext(values(row, customKeys), false);
            }
        }
    }

    private static SortedSet<String> collectSortedCustomKeys(List<TicketExportRow> rows) {
        var customKeys = new TreeSet<String>();
        for (var row : rows) {
            customKeys.addAll(row.customFields().keySet());
        }
        return customKeys;
    }

    private static String[] headers(SortedSet<String> customKeys) {
        var headers = new ArrayList<>(BUILT_IN_HEADERS);
        for (var customKey : customKeys) {
            headers.add("custom.%s".formatted(customKey));
        }
        return headers.toArray(String[]::new);
    }

    private static String[] values(TicketExportRow row, SortedSet<String> customKeys) {
        var values = new ArrayList<String>(BUILT_IN_HEADERS.size() + customKeys.size());
        values.add(textValue(row.identifier()));
        values.add(textValue(row.title()));
        values.add(richTextValue(row.description()));
        values.add(textValue(row.projectKey()));
        values.add(textValue(row.projectName()));
        values.add(textValue(row.statusCode()));
        values.add(textValue(row.statusName()));
        values.add(typedValue(row.categoryId()));
        values.add(textValue(row.categoryName()));
        values.add(typedValue(row.priority()));
        values.add(typedValue(row.type()));
        values.add(textValue(row.authorEmail()));
        values.add(textValue(row.authorName()));
        values.add(textValue(row.assigneeEmail()));
        values.add(textValue(row.assigneeName()));
        values.add(typedValue(row.phaseId()));
        values.add(textValue(row.phaseName()));
        values.add(typedValue(row.observedVersionId()));
        values.add(textValue(row.observedVersionName()));
        values.add(typedValue(row.targetVersionId()));
        values.add(textValue(row.targetVersionName()));
        values.add(typedValue(row.storyPoints()));
        values.add(typedValue(row.dueDate()));
        values.add(typedValue(row.createdAt()));
        values.add(typedValue(row.updatedAt()));
        for (var customKey : customKeys) {
            values.add(customValue(row.customFields().get(customKey)));
        }
        return values.toArray(String[]::new);
    }

    private static String customValue(Object value) {
        return value instanceof String stringValue ? richTextValue(stringValue) : typedValue(value);
    }

    private static String typedValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof TemporalAccessor || value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return textValue(value.toString());
    }

    private static String textValue(String value) {
        if (value == null) {
            return "";
        }
        return isFormula(value) ? "'%s".formatted(value) : value;
    }

    private static String richTextValue(String value) {
        if (value == null) {
            return "";
        }
        return textValue(TicketExportValueConverter.toCsvPlainText(value));
    }

    private static boolean isFormula(String value) {
        return !value.isEmpty() && (value.charAt(0) == '='
                || value.charAt(0) == '+'
                || value.charAt(0) == '-'
                || value.charAt(0) == '@');
    }

}

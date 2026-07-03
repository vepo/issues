package dev.vepo.issues.ticket.csvimport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class CsvImportParser {

    public static final int MAX_ROWS = 500;
    public static final int MAX_FILE_BYTES = 5 * 1024 * 1024;

    public ParsedCsv parse(InputStream inputStream) {
        try {
            var bytes = inputStream.readAllBytes();
            if (bytes.length == 0) {
                throw new BadRequestException("CSV file is empty");
            }
            if (bytes.length > MAX_FILE_BYTES) {
                throw new BadRequestException("CSV file exceeds maximum size of 5 MB");
            }
            if (bytes[0] == (byte) 0xEF && bytes.length > 2 && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                bytes = java.util.Arrays.copyOfRange(bytes, 3, bytes.length);
            }
            var text = new String(bytes, StandardCharsets.UTF_8);
            var firstLine = text.lines().findFirst().orElse("");
            var delimiter = detectDelimiter(firstLine);

            try (var reader = buildReader(bytes, delimiter)) {
                var headerLine = reader.readNext();
                if (headerLine == null || headerLine.length == 0) {
                    throw new BadRequestException("CSV header row is invalid");
                }
                var headers = normalizeHeaders(headerLine);
                if (headers.isEmpty()) {
                    throw new BadRequestException("CSV header row is invalid");
                }

                var rows = new ArrayList<ParsedCsvRow>();
                var truncated = false;
                String[] line;
                var rowNumber = 2;
                while ((line = reader.readNext()) != null) {
                    if (isBlankLine(line)) {
                        continue;
                    }
                    if (rows.size() >= MAX_ROWS) {
                        truncated = true;
                        break;
                    }
                    rows.add(new ParsedCsvRow(rowNumber++, toRowMap(headers, line)));
                }
                return new ParsedCsv(headers, rows, truncated);
            }
        } catch (IOException | CsvValidationException ex) {
            throw new BadRequestException("Unable to parse CSV file");
        }
    }

    private CSVReader buildReader(byte[] bytes, char delimiter) {
        return new CSVReaderBuilder(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
                                                                                                                   .withCSVParser(new RFC4180ParserBuilder().withSeparator(delimiter)
                                                                                                                                                            .build())
                                                                                                                   .build();
    }

    private char detectDelimiter(String headerLine) {
        var commaCount = headerLine.chars().filter(ch -> ch == ',').count();
        var semicolonCount = headerLine.chars().filter(ch -> ch == ';').count();
        return semicolonCount > commaCount ? ';' : ',';
    }

    private List<String> normalizeHeaders(String[] headerLine) {
        var headers = new ArrayList<String>();
        for (var header : headerLine) {
            var trimmed = header == null ? "" : header.trim();
            if (!trimmed.isBlank()) {
                headers.add(trimmed);
            }
        }
        return headers;
    }

    private Map<String, String> toRowMap(List<String> headers, String[] line) {
        var values = new LinkedHashMap<String, String>();
        for (var i = 0; i < headers.size(); i++) {
            var cell = i < line.length && line[i] != null ? line[i].trim() : "";
            values.put(headers.get(i), cell);
        }
        return values;
    }

    private boolean isBlankLine(String[] line) {
        if (line.length == 0) {
            return true;
        }
        for (var value : line) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    public record ParsedCsv(List<String> headers, List<ParsedCsvRow> rows, boolean truncated) {}

    public record ParsedCsvRow(int rowNumber, Map<String, String> values) {}
}

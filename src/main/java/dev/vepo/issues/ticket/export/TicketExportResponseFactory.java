package dev.vepo.issues.ticket.export;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@ApplicationScoped
class TicketExportResponseFactory {

    private static final MediaType CSV_MEDIA_TYPE = new MediaType("text", "csv");

    private final TicketCsvWriter csvWriter;
    private final TicketJsonWriter jsonWriter;
    private final Clock clock;

    @Inject
    TicketExportResponseFactory(TicketCsvWriter csvWriter, TicketJsonWriter jsonWriter, Clock clock) {
        this.csvWriter = csvWriter;
        this.jsonWriter = jsonWriter;
        this.clock = clock;
    }

    Response create(ExportTicketsRequest request, List<TicketExportRow> rows) {
        var format = request.format();
        StreamingOutput stream = output -> write(format, request.source(), rows, output);
        var filename = "tickets-%s.%s".formatted(LocalDate.now(clock), extension(format));
        return Response.ok(stream, mediaType(format))
                       .header(HttpHeaders.CONTENT_DISPOSITION, attachmentHeader(filename))
                       .build();
    }

    private void write(ExportFormat format,
                       ExportSource source,
                       List<TicketExportRow> rows,
                       OutputStream output)
            throws IOException {
        switch (format) {
            case CSV -> csvWriter.write(rows, output);
            case JSON -> jsonWriter.write(source, rows, output);
        }
    }

    private static String extension(ExportFormat format) {
        return switch (format) {
            case CSV -> "csv";
            case JSON -> "json";
        };
    }

    private static MediaType mediaType(ExportFormat format) {
        return switch (format) {
            case CSV -> CSV_MEDIA_TYPE;
            case JSON -> MediaType.APPLICATION_JSON_TYPE;
        };
    }

    private static String attachmentHeader(String filename) {
        var encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"%s\"; filename*=UTF-8''%s".formatted(filename, encodedFilename);
    }
}

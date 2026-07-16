package dev.vepo.issues.ticket.attachments;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class AttachmentContentRules {

    static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    static final int MAX_FILES_PER_TICKET = 20;
    static final long MAX_TOTAL_BYTES_PER_TICKET = 50L * 1024 * 1024;

    private static final Map<String, Set<String>> ALLOWED_BY_EXTENSION = Map.ofEntries(
                                                                                       Map.entry("png", Set.of("image/png")),
                                                                                       Map.entry("jpg", Set.of("image/jpeg")),
                                                                                       Map.entry("jpeg", Set.of("image/jpeg")),
                                                                                       Map.entry("gif", Set.of("image/gif")),
                                                                                       Map.entry("webp", Set.of("image/webp")),
                                                                                       Map.entry("pdf", Set.of("application/pdf")),
                                                                                       Map.entry("txt", Set.of("text/plain")),
                                                                                       Map.entry("md", Set.of("text/markdown", "text/plain")),
                                                                                       Map.entry("csv", Set.of("text/csv", "text/plain", "application/csv")),
                                                                                       Map.entry("doc", Set.of("application/msword")),
                                                                                       Map.entry("docx",
                                                                                                 Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
                                                                                       Map.entry("xls", Set.of("application/vnd.ms-excel")),
                                                                                       Map.entry("xlsx",
                                                                                                 Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                                                                                       Map.entry("odt", Set.of("application/vnd.oasis.opendocument.text")),
                                                                                       Map.entry("ods",
                                                                                                 Set.of("application/vnd.oasis.opendocument.spreadsheet")),
                                                                                       Map.entry("zip",
                                                                                                 Set.of("application/zip", "application/x-zip-compressed")));

    private AttachmentContentRules() {}

    static Optional<String> extensionOf(String filename) {
        if (filename == null || filename.isBlank()) {
            return Optional.empty();
        }
        var name = filename;
        var slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        var dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    static boolean isAllowed(String filename, String contentType) {
        var extension = extensionOf(filename);
        if (extension.isEmpty()) {
            return false;
        }
        var allowedTypes = ALLOWED_BY_EXTENSION.get(extension.get());
        if (allowedTypes == null) {
            return false;
        }
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        var mime = contentType.strip().toLowerCase(Locale.ROOT);
        var semicolon = mime.indexOf(';');
        if (semicolon >= 0) {
            mime = mime.substring(0, semicolon).strip();
        }
        return allowedTypes.contains(mime);
    }

    static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "attachment";
        }
        var name = filename.replace('\\', '/');
        var slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        var cleaned = name.replaceAll("[\\r\\n\"\\\\]", "_").strip();
        return cleaned.isEmpty() ? "attachment" : cleaned;
    }
}

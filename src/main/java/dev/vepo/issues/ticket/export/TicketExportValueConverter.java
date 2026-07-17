package dev.vepo.issues.ticket.export;

import java.util.regex.Pattern;

final class TicketExportValueConverter {

    private static final Pattern BLOCK_END = Pattern.compile("(?i)</(?:blockquote|div|h[1-6]|li|ol|p|pre|ul)\\s*>");
    private static final Pattern LINE_BREAK = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern EXTRA_NEWLINES = Pattern.compile("\\n{3,}");

    private TicketExportValueConverter() {}

    static String toCsvPlainText(String value) {
        if (value == null) {
            return null;
        }
        var plainText = LINE_BREAK.matcher(value).replaceAll("\n");
        plainText = BLOCK_END.matcher(plainText).replaceAll("\n");
        plainText = HTML_TAG.matcher(plainText).replaceAll("");
        plainText = decodeCsvHtmlEntities(plainText);
        return EXTRA_NEWLINES.matcher(plainText).replaceAll("\n\n").strip();
    }

    static String toJsonPlainText(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("(?i)<br\\s*/?>", "\n")
                    .replaceAll("(?i)</(?:p|div|li|h[1-6])\\s*>", "\n")
                    .replaceAll("<[^>]+>", "")
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                    .replace(" \n", "\n")
                    .replace("\n ", "\n")
                    .replaceAll("\\n{3,}", "\n\n")
                    .trim();
    }

    private static String decodeCsvHtmlEntities(String value) {
        var decoded = value.replace("&amp;", "&")
                           .replace("&lt;", "<")
                           .replace("&gt;", ">")
                           .replace("&quot;", "\"")
                           .replace("&#39;", "'")
                           .replace("&apos;", "'");
        var result = new StringBuilder();
        var index = 0;
        while (index < decoded.length()) {
            if (decoded.charAt(index) == '&') {
                var semicolon = decoded.indexOf(';', index + 1);
                if (semicolon > index) {
                    var entity = decoded.substring(index + 1, semicolon);
                    var codePoint = numericEntity(entity);
                    if (codePoint >= 0) {
                        result.appendCodePoint(codePoint);
                        index = semicolon + 1;
                        continue;
                    }
                }
            }
            result.append(decoded.charAt(index));
            index++;
        }
        return result.toString();
    }

    private static int numericEntity(String entity) {
        try {
            if (entity.startsWith("#x") || entity.startsWith("#X")) {
                return Integer.parseInt(entity.substring(2), 16);
            }
            if (entity.startsWith("#")) {
                return Integer.parseInt(entity.substring(1));
            }
        } catch (NumberFormatException ignored) {
            return -1;
        }
        return -1;
    }
}

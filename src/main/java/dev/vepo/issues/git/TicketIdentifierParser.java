package dev.vepo.issues.git;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

final class TicketIdentifierParser {

    private TicketIdentifierParser() {}

    static Set<String> findIdentifiers(String prefix, String text) {
        var found = new LinkedHashSet<String>();
        if (prefix == null || prefix.isBlank() || text == null || text.isBlank()) {
            return found;
        }
        var escaped = Pattern.quote(prefix.trim());
        var pattern = Pattern.compile("\\b(" + escaped + ")-(\\d+)\\b", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(text);
        while (matcher.find()) {
            found.add(prefix.trim() + "-" + matcher.group(2));
        }
        return found;
    }
}

package dev.vepo.issues.user;

import java.util.Locale;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

/**
 * Allowed UI locales for chrome and system labels ({@code pt}, {@code en}).
 */
public final class UiLocale {

    public static final String DEFAULT = "pt";
    public static final Set<String> ALLOWED = Set.of("pt", "en");

    private UiLocale() {}

    public static String requireAllowed(String locale) {
        if (locale == null || locale.isBlank()) {
            throw new BadRequestException("Invalid locale");
        }
        var normalized = normalize(locale);
        if (!ALLOWED.contains(normalized)) {
            throw new BadRequestException("Invalid locale");
        }
        return normalized;
    }

    public static String normalizeOrDefault(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT;
        }
        var normalized = normalize(locale);
        return ALLOWED.contains(normalized) ? normalized : DEFAULT;
    }

    /**
     * Maps an Accept-Language header (or BCP-47 tag) to {@code pt} or {@code en}.
     */
    public static String fromAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return DEFAULT;
        }
        for (var range : acceptLanguage.split(",")) {
            var tag = range.trim().split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (tag.isEmpty()) {
                continue;
            }
            if (tag.equals("en") || tag.startsWith("en-")) {
                return "en";
            }
            if (tag.equals("pt") || tag.startsWith("pt-")) {
                return "pt";
            }
        }
        return DEFAULT;
    }

    private static String normalize(String locale) {
        return locale.trim().toLowerCase(Locale.ROOT);
    }
}

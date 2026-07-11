package dev.vepo.issues.infra;

/**
 * Counts characters after stripping HTML tags from rich-text fields.
 */
public final class PlainTextLength {

    private PlainTextLength() {}

    /**
     * Returns the length of {@code htmlOrText} after HTML tags are removed. Null or
     * blank (including blank after tag strip) yields {@code 0}.
     */
    public static int of(String htmlOrText) {
        if (htmlOrText == null) {
            return 0;
        }
        var plain = htmlOrText.replaceAll("<[^>]+>", "");
        if (plain.isBlank()) {
            return 0;
        }
        return plain.length();
    }
}

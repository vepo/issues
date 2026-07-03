package dev.vepo.issues.ticket.history;

public final class HistoryDisplay {

    private HistoryDisplay() {}

    public static String formatStatus(String statusName) {
        if (statusName == null || statusName.isBlank()) {
            return statusName;
        }
        return java.util.Arrays.stream(statusName.split("_"))
                               .filter(part -> !part.isEmpty())
                               .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                               .reduce((a, b) -> a + " " + b)
                               .orElse(statusName);
    }
}

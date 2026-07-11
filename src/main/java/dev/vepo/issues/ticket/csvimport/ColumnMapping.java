package dev.vepo.issues.ticket.csvimport;

import java.util.Map;

public record ColumnMapping(String titleColumn,
                            String descriptionColumn,
                            String categoryColumn,
                            String priorityColumn,
                            String storyPointsColumn,
                            String assigneeEmailColumn,
                            String statusColumn,
                            String projectColumn,
                            Map<String, String> customFieldColumns) {

    public ColumnMapping {
        customFieldColumns = customFieldColumns == null ? Map.of() : Map.copyOf(customFieldColumns);
    }
}

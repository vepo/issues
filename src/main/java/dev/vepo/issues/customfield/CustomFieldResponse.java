package dev.vepo.issues.customfield;

import java.util.List;

public record CustomFieldResponse(long id,
                                  String key,
                                  String label,
                                  CustomFieldType type,
                                  boolean required,
                                  boolean enabled,
                                  Long projectId,
                                  Long workflowId,
                                  Integer stringMaxLength,
                                  Integer integerMin,
                                  Integer integerMax,
                                  int sortOrder,
                                  List<EnumOptionResponse> enumOptions,
                                  List<String> statusRequired) {

    public static CustomFieldResponse load(CustomField field, List<String> statusNames) {
        return new CustomFieldResponse(field.getId(),
                                       field.getKey(),
                                       field.getLabel(),
                                       field.getType(),
                                       field.isRequired(),
                                       field.isEnabled(),
                                       field.getProjectId(),
                                       field.getWorkflowId(),
                                       field.getStringMaxLength(),
                                       field.getIntegerMin(),
                                       field.getIntegerMax(),
                                       field.getSortOrder(),
                                       field.getEnumOptions()
                                            .stream()
                                            .map(EnumOptionResponse::load)
                                            .toList(),
                                       statusNames == null ? List.of() : List.copyOf(statusNames));
    }

    public static CustomFieldResponse load(CustomField field) {
        return load(field, List.of());
    }
}

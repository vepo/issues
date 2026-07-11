package dev.vepo.issues.customfield;

public record CustomFieldValueResponse(String key,
                                       CustomFieldType type,
                                       Object value,
                                       boolean orphan,
                                       boolean readOnly) {

    public static CustomFieldValueResponse load(CustomField field, Object value, boolean inScope) {
        return new CustomFieldValueResponse(field.getKey(),
                                            field.getType(),
                                            value,
                                            !inScope,
                                            !inScope || !field.isEnabled());
    }
}

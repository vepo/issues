package dev.vepo.issues.customfield;

public record EnumOptionResponse(long id, String value, String label, int sortOrder) {

    public static EnumOptionResponse load(CustomFieldEnumOption option) {
        return new EnumOptionResponse(option.getId(), option.getValue(), option.getLabel(), option.getSortOrder());
    }
}

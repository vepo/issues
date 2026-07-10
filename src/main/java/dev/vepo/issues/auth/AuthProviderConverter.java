package dev.vepo.issues.auth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuthProviderConverter implements AttributeConverter<AuthProvider, String> {

    @Override
    public String convertToDatabaseColumn(AuthProvider attribute) {
        return attribute == null ? AuthProvider.LOCAL.configValue() : attribute.configValue();
    }

    @Override
    public AuthProvider convertToEntityAttribute(String dbData) {
        return AuthProvider.fromStored(dbData);
    }
}

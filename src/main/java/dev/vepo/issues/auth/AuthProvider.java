package dev.vepo.issues.auth;

import java.util.Locale;

/**
 * Active credential provider for the deployment ({@code auth.provider} /
 * {@code AUTH_PROVIDER}).
 */
public enum AuthProvider {
    LOCAL("local"),
    LDAP("ldap"),
    ENDPOINT("endpoint");

    private final String configValue;

    AuthProvider(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static AuthProvider fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        for (var provider : values()) {
            if (provider.configValue.equals(normalized)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown auth provider: %s".formatted(value));
    }

    public static AuthProvider fromStored(String value) {
        return fromConfig(value);
    }

    public boolean isLocal() {
        return this == LOCAL;
    }

    @Override
    public String toString() {
        return configValue;
    }
}

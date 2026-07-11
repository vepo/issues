package dev.vepo.issues.auth.apitoken;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import dev.vepo.issues.infra.IssuesException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApiTokenHasher {

    public static final String PAT_PREFIX = "iss_pat_";
    public static final String SAT_PREFIX = "iss_sat_";

    private static final int SECRET_BYTES = 32;
    private static final int DISPLAY_PREFIX_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generatePersonalApiTokenSecret() {
        return PAT_PREFIX + randomSecret();
    }

    public String generateServiceAccountTokenSecret() {
        return SAT_PREFIX + randomSecret();
    }

    public String hash(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IssuesException("SHA-256 unavailable", e);
        }
    }

    public String displayPrefix(String rawToken) {
        if (rawToken.length() <= DISPLAY_PREFIX_LENGTH) {
            return rawToken;
        }
        return rawToken.substring(0, DISPLAY_PREFIX_LENGTH);
    }

    public boolean isPersonalApiToken(String rawToken) {
        return rawToken != null && rawToken.startsWith(PAT_PREFIX);
    }

    public boolean isServiceAccountToken(String rawToken) {
        return rawToken != null && rawToken.startsWith(SAT_PREFIX);
    }

    public boolean isApiToken(String rawToken) {
        return isPersonalApiToken(rawToken) || isServiceAccountToken(rawToken);
    }

    private String randomSecret() {
        var bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

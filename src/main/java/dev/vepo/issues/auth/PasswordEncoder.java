package dev.vepo.issues.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.issues.infra.IssuesException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PasswordEncoder {

    static final String FORMAT_VERSION = "v1";
    private static final int SALT_BYTES = 16;
    private static final int FORMAT_PARTS = 4;

    private final int passwordIterations;
    private final int passwordKeyLength;
    private final String algorithm;
    private final String legacySalt;
    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    public PasswordEncoder(@ConfigProperty(name = "password.iterations") int passwordIterations,
                           @ConfigProperty(name = "password.key.length") int passwordKeyLength,
                           @ConfigProperty(name = "password.algorithm") String algorithm,
                           @ConfigProperty(name = "password.salt") String legacySalt) {
        this.passwordIterations = passwordIterations;
        this.passwordKeyLength = passwordKeyLength;
        this.algorithm = algorithm;
        this.legacySalt = legacySalt;
    }

    public String hashPassword(String password) {
        var salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        var hash = pbkdf2(password, salt, passwordIterations);
        return "%s$%d$%s$%s".formatted(FORMAT_VERSION,
                                       passwordIterations,
                                       Base64.getEncoder().encodeToString(salt),
                                       Base64.getEncoder().encodeToString(hash));
    }

    public boolean matches(String plainPassword, String hashedPassword) {
        Objects.requireNonNull(hashedPassword, "hashedPassword cannot be null!");
        Objects.requireNonNull(plainPassword, "plainPassword cannot be null!");
        if (isV1Format(hashedPassword)) {
            return matchesV1(plainPassword, hashedPassword);
        }
        return matchesLegacy(plainPassword, hashedPassword);
    }

    public boolean needsRehash(String hashedPassword) {
        return hashedPassword == null || !isV1Format(hashedPassword);
    }

    static boolean isV1Format(String hashedPassword) {
        if (hashedPassword == null) {
            return false;
        }
        var parts = hashedPassword.split("\\$", FORMAT_PARTS);
        return parts.length == FORMAT_PARTS && FORMAT_VERSION.equals(parts[0]);
    }

    private boolean matchesV1(String plainPassword, String hashedPassword) {
        var parts = hashedPassword.split("\\$", FORMAT_PARTS);
        if (parts.length != FORMAT_PARTS) {
            return false;
        }
        try {
            var iterations = Integer.parseInt(parts[1]);
            var salt = Base64.getDecoder().decode(parts[2]);
            var expectedHash = Base64.getDecoder().decode(parts[3]);
            var actualHash = pbkdf2(plainPassword, salt, iterations);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            return false;
        }
    }

    private boolean matchesLegacy(String plainPassword, String hashedPassword) {
        var actual = hashWithSalt(plainPassword, legacySalt.getBytes(StandardCharsets.UTF_8), passwordIterations);
        try {
            var expected = Base64.getDecoder().decode(hashedPassword);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] pbkdf2(String password, byte[] salt, int iterations) {
        return hashWithSalt(password, salt, iterations);
    }

    private byte[] hashWithSalt(String password, byte[] salt, int iterations) {
        var chars = password.toCharArray();
        var spec = new PBEKeySpec(chars, salt, iterations, passwordKeyLength);
        Arrays.fill(chars, Character.MIN_VALUE);
        try {
            var factory = SecretKeyFactory.getInstance(algorithm);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IssuesException("Error encoding password", ex);
        } finally {
            spec.clearPassword();
        }
    }
}

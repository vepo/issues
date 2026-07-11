package dev.vepo.issues.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordEncoderTest {

    private static final String LEGACY_SALT = "test-legacy-salt-value";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int ITERATIONS = 1024;
    private static final int KEY_LENGTH = 256;

    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new PasswordEncoder(ITERATIONS, KEY_LENGTH, ALGORITHM, LEGACY_SALT);
    }

    @Test
    @DisplayName("Two users with the same password get different hashes")
    void shouldProduceDifferentHashesForSamePassword() {
        var first = encoder.hashPassword("same-password");
        var second = encoder.hashPassword("same-password");

        assertNotEquals(first, second);
        assertTrue(PasswordEncoder.isV1Format(first));
        assertTrue(PasswordEncoder.isV1Format(second));
        assertTrue(encoder.matches("same-password", first));
        assertTrue(encoder.matches("same-password", second));
    }

    @Test
    @DisplayName("matches returns true for correct password and false otherwise")
    void shouldMatchCorrectPasswordOnly() {
        var hash = encoder.hashPassword("correct");

        assertTrue(encoder.matches("correct", hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    @DisplayName("matches uses MessageDigest.isEqual path for v1 hashes")
    void shouldCompareV1HashesConstantTime() throws Exception {
        var hash = encoder.hashPassword("secret");
        var parts = hash.split("\\$", 4);
        var salt = Base64.getDecoder().decode(parts[2]);
        var expected = Base64.getDecoder().decode(parts[3]);
        var actual = pbkdf2("secret", salt, Integer.parseInt(parts[1]));

        assertTrue(MessageDigest.isEqual(expected, actual));
        assertTrue(encoder.matches("secret", hash));
    }

    @Test
    @DisplayName("Legacy global-salt hashes still verify and need rehash")
    void shouldMatchLegacyHashAndFlagRehash() throws Exception {
        var legacyHash = Base64.getEncoder().encodeToString(pbkdf2("qwas1234",
                                                                   LEGACY_SALT.getBytes(StandardCharsets.UTF_8),
                                                                   ITERATIONS));

        assertTrue(encoder.matches("qwas1234", legacyHash));
        assertFalse(encoder.matches("wrong", legacyHash));
        assertTrue(encoder.needsRehash(legacyHash));
        assertFalse(encoder.needsRehash(encoder.hashPassword("qwas1234")));
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) throws Exception {
        var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}

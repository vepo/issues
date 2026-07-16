package dev.vepo.issues.git;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import dev.vepo.issues.infra.IssuesException;
import jakarta.ws.rs.ForbiddenException;

final class WebhookSignatureVerifier {

    private WebhookSignatureVerifier() {}

    static void requireValid(String secret, byte[] body, String hubSignature256, String gitlabToken) {
        if (secret == null || secret.isBlank()) {
            throw new ForbiddenException("Webhook secret is not configured");
        }
        if (hubSignature256 != null && !hubSignature256.isBlank()) {
            if (!matchesHubSignature(secret, body, hubSignature256)) {
                throw new ForbiddenException("Invalid webhook signature");
            }
            return;
        }
        if (gitlabToken != null && !gitlabToken.isBlank()) {
            if (!MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),
                                       gitlabToken.getBytes(StandardCharsets.UTF_8))) {
                throw new ForbiddenException("Invalid webhook token");
            }
            return;
        }
        throw new ForbiddenException("Missing webhook signature");
    }

    private static boolean matchesHubSignature(String secret, byte[] body, String header) {
        var expected = "sha256=" + hmacSha256Hex(secret, body);
        return MessageDigest.isEqual(expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                                     header.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256Hex(String secret, byte[] body) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IssuesException("HMAC-SHA256 unavailable", e);
        }
    }
}

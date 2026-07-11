package dev.vepo.issues.auth.recovery;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.auth.apitoken.ApiTokenHasher;
import dev.vepo.issues.user.PasswordResetToken;
import dev.vepo.issues.user.PasswordResetTokenRepository;
import dev.vepo.issues.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
class ConfirmPasswordResetEndpointTest {

    @Inject
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Inject
    ApiTokenHasher tokenHasher;

    @Test
    void shouldConfirmPasswordResetWhenTokenIsValid() {
        var user = Given.randomUser();
        var rawToken = PasswordResetToken.generateRawToken();
        Given.transaction(() -> {
            var managedUser = Given.inject(UserRepository.class).findById(user.getId()).orElseThrow();
            passwordResetTokenRepository.invalidateAllUserTokens(managedUser.getId());
            passwordResetTokenRepository.save(new PasswordResetToken(managedUser, tokenHasher.hash(rawToken), rawToken));
        });

        given().contentType(ContentType.JSON)
               .body("""
                     {
                       "token": "%s",
                       "newPassword": "newSecret99"
                     }
                     """.formatted(rawToken))
               .when()
               .post("/api/auth/recovery/confirm")
               .then()
               .statusCode(200);

        given().contentType(ContentType.JSON)
               .body("""
                     {
                       "email": "%s",
                       "password": "newSecret99"
                     }
                     """.formatted(user.getEmail()))
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(200);
    }

    @Test
    void shouldRejectUnknownToken() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                       "token": "%s",
                       "newPassword": "newSecret99"
                     }
                     """.formatted(UUID.randomUUID()))
               .when()
               .post("/api/auth/recovery/confirm")
               .then()
               .statusCode(400);
    }

    @Test
    void shouldRejectExpiredToken() {
        var user = Given.randomUser();
        var rawToken = PasswordResetToken.generateRawToken();
        Given.transaction(() -> {
            var managedUser = Given.inject(UserRepository.class).findById(user.getId()).orElseThrow();
            passwordResetTokenRepository.invalidateAllUserTokens(managedUser.getId());
            var expired = new PasswordResetToken(managedUser, tokenHasher.hash(rawToken), rawToken);
            expired.setExpiryDate(LocalDateTime.now().minusHours(1));
            passwordResetTokenRepository.save(expired);
        });

        given().contentType(ContentType.JSON)
               .body("""
                     {
                       "token": "%s",
                       "newPassword": "newSecret99"
                     }
                     """.formatted(rawToken))
               .when()
               .post("/api/auth/recovery/confirm")
               .then()
               .statusCode(400);
    }

    @Test
    void shouldRejectReusedToken() {
        var user = Given.randomUser();
        var rawToken = PasswordResetToken.generateRawToken();
        Given.transaction(() -> {
            var managedUser = Given.inject(UserRepository.class).findById(user.getId()).orElseThrow();
            passwordResetTokenRepository.invalidateAllUserTokens(managedUser.getId());
            passwordResetTokenRepository.save(new PasswordResetToken(managedUser, tokenHasher.hash(rawToken), rawToken));
        });

        var body = """
                   {
                     "token": "%s",
                     "newPassword": "anotherSecret99"
                   }
                   """.formatted(rawToken);

        given().contentType(ContentType.JSON)
               .body(body)
               .when()
               .post("/api/auth/recovery/confirm")
               .then()
               .statusCode(200);

        given().contentType(ContentType.JSON)
               .body(body)
               .when()
               .post("/api/auth/recovery/confirm")
               .then()
               .statusCode(400)
               .body("message", equalTo("Invalid or expired reset token"));
    }
}

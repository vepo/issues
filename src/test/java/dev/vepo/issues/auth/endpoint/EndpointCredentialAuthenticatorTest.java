package dev.vepo.issues.auth.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.issues.auth.AuthProvider;
import dev.vepo.issues.user.Role;
import jakarta.ws.rs.NotAuthorizedException;

class EndpointCredentialAuthenticatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should return USER identity when endpoint responds 200 with email")
    void shouldReturnUserIdentityOnSuccess() {
        EndpointAuthHttpTransport transport = (url, jsonBody, connectTimeoutMs, readTimeoutMs) -> """
                                                                                                  {
                                                                                                    "email": "ext@example.com",
                                                                                                    "name": "Ext User",
                                                                                                    "username": "extuser"
                                                                                                  }
                                                                                                  """;
        var authenticator = new EndpointCredentialAuthenticator(transport, objectMapper, "http://auth.example/verify");

        var identity = authenticator.authenticate("ext@example.com", "secret");

        assertEquals("ext@example.com", identity.email());
        assertEquals("Ext User", identity.name());
        assertEquals("extuser", identity.username());
        assertEquals(AuthProvider.ENDPOINT, identity.provider());
        assertTrue(identity.roles().contains(Role.USER));
        assertEquals(1, identity.roles().size());
        assertFalse(identity.syncRoles());
    }

    @Test
    @DisplayName("Should reject when endpoint returns non-200")
    void shouldRejectWhenEndpointReturnsNon200() {
        EndpointAuthHttpTransport transport = (url, jsonBody, connectTimeoutMs, readTimeoutMs) -> {
            throw new EndpointAuthHttpException(401, "unauthorized");
        };
        var authenticator = new EndpointCredentialAuthenticator(transport, objectMapper, "http://auth.example/verify");

        assertThrows(NotAuthorizedException.class, () -> authenticator.authenticate("ext@example.com", "bad"));
    }

    @Test
    @DisplayName("Should reject when endpoint URL is empty")
    void shouldRejectWhenEndpointUrlEmpty() {
        EndpointAuthHttpTransport transport = (url, jsonBody, connectTimeoutMs, readTimeoutMs) -> {
            throw new AssertionError("should not call transport");
        };
        var authenticator = new EndpointCredentialAuthenticator(transport, objectMapper, "");

        assertThrows(NotAuthorizedException.class, () -> authenticator.authenticate("ext@example.com", "secret"));
    }
}

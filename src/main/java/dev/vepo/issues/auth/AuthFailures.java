package dev.vepo.issues.auth;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

/**
 * Builds {@link NotAuthorizedException} with a proper message (single-arg
 * treats the string as a challenge).
 */
public final class AuthFailures {

    private AuthFailures() {}

    public static NotAuthorizedException invalidCredentials() {
        return new NotAuthorizedException("Invalid credentials!",
                                          Response.status(Response.Status.UNAUTHORIZED).build());
    }

    public static NotAuthorizedException invalidRefreshToken() {
        return new NotAuthorizedException("Invalid refresh token",
                                          Response.status(Response.Status.UNAUTHORIZED).build());
    }
}

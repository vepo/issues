package dev.vepo.issues.auth;

import jakarta.ws.rs.NotAuthorizedException;

/**
 * Pluggable credential check. One implementation is selected per deployment via
 * {@code auth.provider}.
 */
public interface CredentialAuthenticator {

    AuthProvider provider();

    /**
     * Verifies credentials with the external or local store.
     *
     * @throws NotAuthorizedException when credentials are invalid
     */
    VerifiedIdentity authenticate(String email, String password);
}

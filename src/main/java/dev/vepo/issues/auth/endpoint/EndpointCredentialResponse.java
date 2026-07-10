package dev.vepo.issues.auth.endpoint;

/**
 * Successful identity payload from the external credential endpoint.
 */
public record EndpointCredentialResponse(String email, String name, String username) {}

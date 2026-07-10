package dev.vepo.issues.auth.endpoint;

/**
 * JSON body posted to the external credential endpoint.
 */
public record EndpointCredentialRequest(String email, String password) {}

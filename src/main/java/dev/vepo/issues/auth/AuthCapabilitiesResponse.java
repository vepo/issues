package dev.vepo.issues.auth;

/**
 * Public auth UI capabilities for the active credential provider.
 */
public record AuthCapabilitiesResponse(String provider, boolean passwordRecovery, boolean changePassword) {}

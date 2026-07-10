package dev.vepo.issues.auth;

import java.util.Set;

import dev.vepo.issues.user.Role;

/**
 * Identity verified by a {@link CredentialAuthenticator} before local User
 * resolve/provision.
 */
public record VerifiedIdentity(String email,
                               String name,
                               String username,
                               Set<Role> roles,
                               AuthProvider provider,
                               boolean syncRoles) {}

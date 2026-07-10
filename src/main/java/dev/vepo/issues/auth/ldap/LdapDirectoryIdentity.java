package dev.vepo.issues.auth.ldap;

import java.util.Set;

/**
 * Directory attributes returned after a successful LDAP bind/search.
 */
public record LdapDirectoryIdentity(String email, String name, String username, Set<String> groups) {}

package dev.vepo.issues.auth.ldap;

import jakarta.ws.rs.NotAuthorizedException;

/**
 * Abstracts LDAP search/bind so authenticators can be unit-tested without a
 * live directory.
 */
public interface LdapDirectoryClient {

    /**
     * Authenticates against the directory and returns identity attributes.
     *
     * @throws NotAuthorizedException when bind/search fails or LDAP is not
     *                                configured
     */
    LdapDirectoryIdentity authenticate(String email, String password);
}

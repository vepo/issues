package dev.vepo.issues.auth.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.auth.AuthProvider;
import dev.vepo.issues.user.Role;

class LdapCredentialAuthenticatorTest {

    @Test
    @DisplayName("Should always include USER and map matching groups to roles")
    void shouldMapGroupsToRolesAndAlwaysIncludeUser() {
        LdapDirectoryClient client = (email, password) -> new LdapDirectoryIdentity(email,
                                                                                    "LDAP User",
                                                                                    "ldapuser",
                                                                                    Set.of("cn=admins,ou=groups", "cn=others,ou=groups"));
        var authenticator = new LdapCredentialAuthenticator(client,
                                                            Map.of("admins", Role.ADMIN,
                                                                   "managers", Role.PROJECT_MANAGER));

        var identity = authenticator.authenticate("ldap@example.com", "secret");

        assertEquals(AuthProvider.LDAP, identity.provider());
        assertTrue(identity.syncRoles());
        assertTrue(identity.roles().contains(Role.USER));
        assertTrue(identity.roles().contains(Role.ADMIN));
        assertEquals(2, identity.roles().size());
        assertEquals("LDAP User", identity.name());
        assertEquals("ldapuser", identity.username());
    }

    @Test
    @DisplayName("Should return only USER when group map is empty")
    void shouldReturnOnlyUserWhenGroupMapEmpty() {
        LdapDirectoryClient client = (email, password) -> new LdapDirectoryIdentity(email,
                                                                                    "LDAP User",
                                                                                    "ldapuser",
                                                                                    Set.of("cn=admins,ou=groups"));
        var authenticator = new LdapCredentialAuthenticator(client, Map.of());

        var identity = authenticator.authenticate("ldap@example.com", "secret");

        assertEquals(Set.of(Role.USER), identity.roles());
    }

    @Test
    @DisplayName("Should parse group-role-map config string")
    void shouldParseGroupRoleMapConfig() {
        var map = LdapCredentialAuthenticator.parseGroupRoleMap("admins=admin,managers=project-manager");
        assertEquals(Role.ADMIN, map.get("admins"));
        assertEquals(Role.PROJECT_MANAGER, map.get("managers"));
    }
}

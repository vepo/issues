package dev.vepo.issues.auth.ldap;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.auth.AuthProvider;
import dev.vepo.issues.auth.CredentialAuthenticator;
import dev.vepo.issues.auth.VerifiedIdentity;
import dev.vepo.issues.user.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LdapCredentialAuthenticator implements CredentialAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(LdapCredentialAuthenticator.class);

    private final LdapDirectoryClient directoryClient;
    private final Map<String, Role> groupRoleMap;

    @Inject
    public LdapCredentialAuthenticator(LdapDirectoryClient directoryClient,
                                       @ConfigProperty(name = "auth.ldap.group-role-map") Optional<String> groupRoleMapConfig) {
        this.directoryClient = directoryClient;
        this.groupRoleMap = parseGroupRoleMap(groupRoleMapConfig.orElse(""));
    }

    LdapCredentialAuthenticator(LdapDirectoryClient directoryClient, Map<String, Role> groupRoleMap) {
        this.directoryClient = directoryClient;
        this.groupRoleMap = Map.copyOf(groupRoleMap);
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.LDAP;
    }

    @Override
    public VerifiedIdentity authenticate(String email, String password) {
        var directoryIdentity = directoryClient.authenticate(email, password);
        var roles = mapRoles(directoryIdentity.groups());
        logger.debug("LDAP authentication succeeded for email={} roles={}", directoryIdentity.email(), roles);
        return new VerifiedIdentity(directoryIdentity.email(),
                                    directoryIdentity.name(),
                                    directoryIdentity.username(),
                                    roles,
                                    AuthProvider.LDAP,
                                    true);
    }

    Set<Role> mapRoles(Set<String> groups) {
        var roles = new HashSet<Role>();
        roles.add(Role.USER);
        if (groups == null || groups.isEmpty() || groupRoleMap.isEmpty()) {
            return roles;
        }
        for (var entry : groupRoleMap.entrySet()) {
            if (groups.stream().anyMatch(group -> matchesGroup(group, entry.getKey()))) {
                roles.add(entry.getValue());
            }
        }
        return roles;
    }

    private static boolean matchesGroup(String directoryGroup, String mappedGroup) {
        if (directoryGroup.equalsIgnoreCase(mappedGroup)) {
            return true;
        }
        return directoryGroup.toLowerCase(Locale.ROOT)
                             .contains(mappedGroup.toLowerCase(Locale.ROOT));
    }

    static Map<String, Role> parseGroupRoleMap(String config) {
        var map = new LinkedHashMap<String, Role>();
        if (config == null || config.isBlank()) {
            return map;
        }
        for (var part : config.split(",")) {
            var trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            var eq = trimmed.indexOf('=');
            if (eq <= 0 || eq == trimmed.length() - 1) {
                logger.warn("Ignoring invalid LDAP group-role-map entry: {}", trimmed);
                continue;
            }
            var group = trimmed.substring(0, eq).trim();
            var roleValue = trimmed.substring(eq + 1).trim();
            Role.from(roleValue)
                .ifPresentOrElse(role -> map.put(group, role),
                                 () -> logger.warn("Ignoring unknown role in LDAP group-role-map: {}", roleValue));
        }
        return map;
    }
}

package dev.vepo.issues.auth.ldap;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Optional;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.auth.AuthFailures;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JndiLdapDirectoryClient implements LdapDirectoryClient {

    private static final Logger logger = LoggerFactory.getLogger(JndiLdapDirectoryClient.class);

    private final Optional<String> url;
    private final Optional<String> baseDn;
    private final Optional<String> bindDn;
    private final Optional<String> bindPassword;
    private final String userFilter;
    private final String emailAttribute;
    private final String nameAttribute;
    private final String usernameAttribute;
    private final String groupAttribute;

    @Inject
    public JndiLdapDirectoryClient(@ConfigProperty(name = "auth.ldap.url") Optional<String> url,
                                   @ConfigProperty(name = "auth.ldap.base-dn") Optional<String> baseDn,
                                   @ConfigProperty(name = "auth.ldap.bind-dn") Optional<String> bindDn,
                                   @ConfigProperty(name = "auth.ldap.bind-password") Optional<String> bindPassword,
                                   @ConfigProperty(name = "auth.ldap.user-filter", defaultValue = "(mail={0})") String userFilter,
                                   @ConfigProperty(name = "auth.ldap.email-attribute", defaultValue = "mail") String emailAttribute,
                                   @ConfigProperty(name = "auth.ldap.name-attribute", defaultValue = "cn") String nameAttribute,
                                   @ConfigProperty(name = "auth.ldap.username-attribute", defaultValue = "uid") String usernameAttribute,
                                   @ConfigProperty(name = "auth.ldap.group-attribute", defaultValue = "memberOf") String groupAttribute) {
        this.url = url;
        this.baseDn = baseDn;
        this.bindDn = bindDn;
        this.bindPassword = bindPassword;
        this.userFilter = userFilter;
        this.emailAttribute = emailAttribute;
        this.nameAttribute = nameAttribute;
        this.usernameAttribute = usernameAttribute;
        this.groupAttribute = groupAttribute;
    }

    @Override
    public LdapDirectoryIdentity authenticate(String email, String password) {
        if (url.isEmpty() || url.get().isBlank()) {
            logger.warn("LDAP authentication attempted but auth.ldap.url is empty");
            throw AuthFailures.invalidCredentials();
        }
        try {
            var userDn = findUserDn(email);
            bindAsUser(userDn, password);
            return readIdentity(userDn, email);
        } catch (NamingException e) {
            logger.debug("LDAP authentication failed for email={}", email, e);
            throw AuthFailures.invalidCredentials();
        }
    }

    private String findUserDn(String email) throws NamingException {
        var filter = userFilter.replace("{0}", escapeFilterValue(email));
        var controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[] { "dn" });

        DirContext context = null;
        try {
            context = openContext(bindDn.filter(dn -> !dn.isBlank()).orElse(null),
                                  bindPassword.filter(p -> !p.isBlank()).orElse(null));
            NamingEnumeration<SearchResult> results = context.search(baseDn.orElse(""), filter, controls);
            if (!results.hasMore()) {
                throw new NamingException("User not found in LDAP");
            }
            var result = results.next();
            if (results.hasMore()) {
                throw new NamingException("Multiple LDAP entries for email");
            }
            return result.getNameInNamespace();
        } finally {
            closeQuietly(context);
        }
    }

    private void bindAsUser(String userDn, String password) throws NamingException {
        DirContext userContext = null;
        try {
            userContext = openContext(userDn, password);
        } finally {
            closeQuietly(userContext);
        }
    }

    private LdapDirectoryIdentity readIdentity(String userDn, String fallbackEmail) throws NamingException {
        DirContext context = null;
        try {
            context = openContext(bindDn.filter(dn -> !dn.isBlank()).orElse(userDn),
                                  bindPassword.filter(p -> !p.isBlank()).orElse(null));
            var attrs = context.getAttributes(userDn,
                                              new String[] { emailAttribute, nameAttribute, usernameAttribute, groupAttribute });
            var email = firstAttribute(attrs.get(emailAttribute)).orElse(fallbackEmail);
            var name = firstAttribute(attrs.get(nameAttribute)).orElse(null);
            var username = firstAttribute(attrs.get(usernameAttribute)).orElse(null);
            var groups = allAttributes(attrs.get(groupAttribute));
            return new LdapDirectoryIdentity(email, name, username, groups);
        } finally {
            closeQuietly(context);
        }
    }

    private DirContext openContext(String principal, String credentials) throws NamingException {
        var env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url.orElseThrow());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        if (principal != null && !principal.isBlank()) {
            env.put(Context.SECURITY_PRINCIPAL, principal);
            env.put(Context.SECURITY_CREDENTIALS, credentials == null ? "" : credentials);
        }
        return new InitialDirContext(env);
    }

    private static Optional<String> firstAttribute(Attribute attribute) throws NamingException {
        if (attribute == null || attribute.size() == 0) {
            return Optional.empty();
        }
        var value = attribute.get(0);
        return value == null ? Optional.empty() : Optional.of(value.toString());
    }

    private static Set<String> allAttributes(Attribute attribute) throws NamingException {
        var groups = new HashSet<String>();
        if (attribute == null) {
            return groups;
        }
        var values = attribute.getAll();
        while (values.hasMore()) {
            var value = values.next();
            if (value != null) {
                groups.add(value.toString());
            }
        }
        return groups;
    }

    private static String escapeFilterValue(String value) {
        return value.replace("\\", "\\5c")
                    .replace("*", "\\2a")
                    .replace("(", "\\28")
                    .replace(")", "\\29")
                    .replace("\0", "\\00");
    }

    private static void closeQuietly(DirContext context) {
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (NamingException e) {
            logger.debug("Failed to close LDAP context", e);
        }
    }
}

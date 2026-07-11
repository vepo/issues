package dev.vepo.issues.auth.apitoken;

import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ApiTokenIdentityProvider implements IdentityProvider<ApiTokenAuthenticationRequest> {

    private static final Logger LOGGER = Logger.getLogger(ApiTokenIdentityProvider.class);

    public static final String VIA_AGENT_ATTRIBUTE = "via_agent";

    private final ApiTokenService apiTokenService;
    private final ApiTokenHasher apiTokenHasher;
    private final ServiceAccountTokenAuthenticator serviceAccountTokenAuthenticator;

    @Inject
    public ApiTokenIdentityProvider(ApiTokenService apiTokenService,
                                    ApiTokenHasher apiTokenHasher,
                                    ServiceAccountTokenAuthenticator serviceAccountTokenAuthenticator) {
        this.apiTokenService = apiTokenService;
        this.apiTokenHasher = apiTokenHasher;
        this.serviceAccountTokenAuthenticator = serviceAccountTokenAuthenticator;
    }

    @Override
    public Class<ApiTokenAuthenticationRequest> getRequestType() {
        return ApiTokenAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(ApiTokenAuthenticationRequest request, AuthenticationRequestContext context) {
        return context.runBlocking(() -> authenticateBlocking(request.getToken()));
    }

    private SecurityIdentity authenticateBlocking(String rawToken) {
        var user = apiTokenHasher.isPersonalApiToken(rawToken)
                                                               ? apiTokenService.authenticatePersonalApiToken(rawToken)
                                                               : serviceAccountTokenAuthenticator.authenticate(rawToken);
        if (user.isEmpty()) {
            LOGGER.debugf("Rejected API token authentication");
            throw new AuthenticationFailedException("Invalid API token");
        }
        var identity = user.get();
        var builder = QuarkusSecurityIdentity.builder()
                                             .setPrincipal(() -> identity.getUsername())
                                             .addAttribute(VIA_AGENT_ATTRIBUTE, Boolean.TRUE)
                                             .setAnonymous(false);
        identity.getRoles().forEach(role -> builder.addRole(role.role()));
        return builder.build();
    }
}

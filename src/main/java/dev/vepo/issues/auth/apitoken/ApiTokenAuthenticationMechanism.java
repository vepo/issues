package dev.vepo.issues.auth.apitoken;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Priority(1)
public class ApiTokenAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiTokenHasher apiTokenHasher;

    @Inject
    public ApiTokenAuthenticationMechanism(ApiTokenHasher apiTokenHasher) {
        this.apiTokenHasher = apiTokenHasher;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        var authorization = context.request().headers().get(HttpHeaderNames.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return Uni.createFrom().optional(Optional.empty());
        }
        var rawToken = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!apiTokenHasher.isApiToken(rawToken)) {
            return Uni.createFrom().optional(Optional.empty());
        }
        var request = new ApiTokenAuthenticationRequest(rawToken);
        HttpSecurityUtils.setRoutingContextAttribute(request, context);
        return identityProviderManager.authenticate(request);
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom()
                  .item(new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(), HttpHeaderNames.WWW_AUTHENTICATE, "Bearer"));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.singleton(ApiTokenAuthenticationRequest.class);
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "bearer"));
    }
}

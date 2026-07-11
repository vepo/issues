package dev.vepo.issues.auth;

import java.util.Optional;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class AuthRateLimitFilter {

    private final AuthRateLimiter authRateLimiter;

    @Inject
    public AuthRateLimitFilter(AuthRateLimiter authRateLimiter) {
        this.authRateLimiter = authRateLimiter;
    }

    @ServerRequestFilter(preMatching = true)
    public Optional<Response> filter(ContainerRequestContext requestContext) {
        if (!"POST".equalsIgnoreCase(requestContext.getMethod())) {
            return Optional.empty();
        }
        var path = normalize(requestContext.getUriInfo().getPath());
        if (!isAuthMutation(path)) {
            return Optional.empty();
        }
        var clientKey = clientKey(requestContext, path);
        if (!authRateLimiter.tryAcquire(clientKey)) {
            return Optional.of(Response.status(429)
                                       .entity("{\"code\":429,\"message\":\"Too many authentication requests\"}")
                                       .type("application/json")
                                       .build());
        }
        return Optional.empty();
    }

    private static boolean isAuthMutation(String path) {
        return path.endsWith("/auth/login")
                || path.endsWith("/auth/register")
                || path.endsWith("/auth/refresh")
                || path.endsWith("/auth/recovery")
                || path.endsWith("/auth/recovery/confirm");
    }

    private static String normalize(String path) {
        if (path == null) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String clientKey(ContainerRequestContext requestContext, String path) {
        var forwarded = requestContext.getHeaderString("X-Forwarded-For");
        var ip = (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : "unknown";
        return "auth:%s:%s".formatted(path, ip);
    }
}

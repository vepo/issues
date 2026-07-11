package dev.vepo.issues.mcp;

import io.quarkiverse.mcp.server.ToolResponse;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IssuesAuthSupport {

    private final HttpServerRequest request;

    @Inject
    public IssuesAuthSupport(HttpServerRequest request) {
        this.request = request;
    }

    public String requireAuthorization() {
        var authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new MissingAuthorizationException();
        }
        return authorization;
    }

    public static ToolResponse missingAuthResponse() {
        return ToolResponse.error("Missing Authorization header. Pass Bearer <PAT or SA token> from the MCP client.");
    }

    public static final class MissingAuthorizationException extends RuntimeException {
        public MissingAuthorizationException() {
            super("Missing Authorization header");
        }
    }
}

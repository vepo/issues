package dev.vepo.issues.auth.apitoken.revoke;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.auth.apitoken.ApiTokenPaths;
import dev.vepo.issues.auth.apitoken.ApiTokenService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path(ApiTokenPaths.BASE)
@ApplicationScoped
@DenyAll
@Tag(name = "ApiToken")
public class RevokeApiTokenEndpoint {

    private final ApiTokenService apiTokenService;

    @Inject
    public RevokeApiTokenEndpoint(ApiTokenService apiTokenService) {
        this.apiTokenService = apiTokenService;
    }

    @DELETE
    @Path("/{id}")
    @ResponseStatus(204)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "revokeApiToken", summary = "Revoke a personal API token")
    public void revoke(@PathParam("id") long id, @Context SecurityContext securityContext) {
        apiTokenService.revoke(securityContext.getUserPrincipal().getName(), id);
    }
}

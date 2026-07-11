package dev.vepo.issues.auth.apitoken.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.auth.apitoken.ApiTokenPaths;
import dev.vepo.issues.auth.apitoken.ApiTokenResponse;
import dev.vepo.issues.auth.apitoken.ApiTokenService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(ApiTokenPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "ApiToken")
public class ListApiTokensEndpoint {

    private final ApiTokenService apiTokenService;

    @Inject
    public ListApiTokensEndpoint(ApiTokenService apiTokenService) {
        this.apiTokenService = apiTokenService;
    }

    @GET
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listApiTokens", summary = "List personal API tokens")
    public List<ApiTokenResponse> list(@Context SecurityContext securityContext) {
        return apiTokenService.list(securityContext.getUserPrincipal().getName());
    }
}

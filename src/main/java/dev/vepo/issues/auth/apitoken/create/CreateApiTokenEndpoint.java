package dev.vepo.issues.auth.apitoken.create;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.auth.apitoken.ApiTokenPaths;
import dev.vepo.issues.auth.apitoken.ApiTokenService;
import dev.vepo.issues.auth.apitoken.CreateApiTokenRequest;
import dev.vepo.issues.auth.apitoken.CreatedApiTokenResponse;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(ApiTokenPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "ApiToken")
public class CreateApiTokenEndpoint {

    private final ApiTokenService apiTokenService;

    @Inject
    public CreateApiTokenEndpoint(ApiTokenService apiTokenService) {
        this.apiTokenService = apiTokenService;
    }

    @POST
    @ResponseStatus(201)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "createApiToken", summary = "Create a personal API token")
    public CreatedApiTokenResponse create(@Valid CreateApiTokenRequest request, @Context SecurityContext securityContext) {
        return apiTokenService.create(securityContext.getUserPrincipal().getName(), request);
    }
}

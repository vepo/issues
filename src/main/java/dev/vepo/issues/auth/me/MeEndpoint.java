package dev.vepo.issues.auth.me;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.auth.AuthPaths;
import dev.vepo.issues.auth.AuthResponse;
import dev.vepo.issues.auth.AuthenticationService;
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

@Path(AuthPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Auth")
public class MeEndpoint {

    private final AuthenticationService authenticationService;

    @Inject
    public MeEndpoint(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GET
    @Path("/me")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "me", summary = "Get authenticated user profile")
    public AuthResponse me(@Context SecurityContext ctx) {
        return authenticationService.me(ctx.getUserPrincipal().getName());
    }
}

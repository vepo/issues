package dev.vepo.issues.auth.changepassword;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.auth.AuthPaths;
import dev.vepo.issues.auth.AuthenticationService;
import dev.vepo.issues.auth.ChangePasswordRequest;
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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path(AuthPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Auth")
public class ChangePasswordEndpoint {

    private final AuthenticationService authenticationService;

    @Inject
    public ChangePasswordEndpoint(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @POST
    @Path("/change-password")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "changePassword", summary = "Change password for authenticated user")
    public Response changePassword(@Valid ChangePasswordRequest request, @Context SecurityContext ctx) {
        authenticationService.changePassword(ctx.getUserPrincipal().getName(), request);
        return Response.ok().build();
    }
}

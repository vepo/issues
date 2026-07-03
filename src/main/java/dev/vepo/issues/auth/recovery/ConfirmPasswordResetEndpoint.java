package dev.vepo.issues.auth.recovery;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.auth.AuthPaths;
import dev.vepo.issues.auth.AuthenticationService;
import dev.vepo.issues.auth.ConfirmPasswordResetRequest;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path(AuthPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Auth")
public class ConfirmPasswordResetEndpoint {

    private final AuthenticationService authenticationService;

    @Inject
    public ConfirmPasswordResetEndpoint(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @POST
    @Path("/recovery/confirm")
    @PermitAll
    @Operation(operationId = "confirmPasswordReset", summary = "Confirm password reset with token")
    public Response confirmPasswordReset(@Valid ConfirmPasswordResetRequest request) {
        authenticationService.confirmPasswordReset(request);
        return Response.ok().build();
    }
}

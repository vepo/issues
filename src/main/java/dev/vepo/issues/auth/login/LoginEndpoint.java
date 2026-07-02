package dev.vepo.issues.auth.login;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.auth.AuthPaths;
import dev.vepo.issues.auth.AuthenticationService;
import dev.vepo.issues.auth.LoginRequest;
import dev.vepo.issues.auth.LoginResponse;
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

@Path(AuthPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Auth")
public class LoginEndpoint {

    private final AuthenticationService authenticationService;

    @Inject
    public LoginEndpoint(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(operationId = "login", summary = "Authenticate user")
    public LoginResponse login(@Valid LoginRequest request) {
        return authenticationService.login(request);
    }
}

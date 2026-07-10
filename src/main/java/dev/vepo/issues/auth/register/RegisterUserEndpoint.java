package dev.vepo.issues.auth.register;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.auth.AuthPaths;
import dev.vepo.issues.auth.AuthenticationService;
import dev.vepo.issues.auth.RegisterUserRequest;
import dev.vepo.issues.user.UserResponse;
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
public class RegisterUserEndpoint {

    private final AuthenticationService authenticationService;

    @Inject
    public RegisterUserEndpoint(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @POST
    @Path("/register")
    @PermitAll
    @ResponseStatus(201)
    @Operation(operationId = "registerUser", summary = "Register a new user with role user")
    public UserResponse register(@Valid RegisterUserRequest request) {
        return authenticationService.register(request);
    }
}

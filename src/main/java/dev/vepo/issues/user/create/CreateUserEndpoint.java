package dev.vepo.issues.user.create;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.user.CreateUserRequest;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.UserPaths;
import dev.vepo.issues.user.UserResponse;
import dev.vepo.issues.user.UserService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(UserPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "User")
public class CreateUserEndpoint {

    private final UserService userService;

    @Inject
    public CreateUserEndpoint(UserService userService) {
        this.userService = userService;
    }

    @POST
    @ResponseStatus(201)
    @RolesAllowed(Role.ADMIN_ROLE)
    @Operation(operationId = "createUser", summary = "Create a user")
    public UserResponse create(@Valid CreateUserRequest request) {
        return userService.create(request);
    }
}

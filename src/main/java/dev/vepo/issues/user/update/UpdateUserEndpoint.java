package dev.vepo.issues.user.update;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(UserPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "User")
public class UpdateUserEndpoint {

    private final UserService userService;

    @Inject
    public UpdateUserEndpoint(UserService userService) {
        this.userService = userService;
    }

    @POST
    @Path("{userId}")
    @RolesAllowed(Role.ADMIN_ROLE)
    @Operation(operationId = "updateUser", summary = "Update a user")
    public UserResponse update(@PathParam("userId") long userId, @Valid CreateUserRequest request) {
        return userService.update(userId, request);
    }
}

package dev.vepo.issues.user.delete;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.UserPaths;
import dev.vepo.issues.user.UserService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(UserPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "User")
public class DeleteUserEndpoint {

    private final UserService userService;

    @Inject
    public DeleteUserEndpoint(UserService userService) {
        this.userService = userService;
    }

    @DELETE
    @Path("{userId}")
    @RolesAllowed(Role.ADMIN_ROLE)
    @Operation(operationId = "deleteUser", summary = "Soft-delete a user")
    public void delete(@PathParam("userId") long userId) {
        userService.delete(userId);
    }
}

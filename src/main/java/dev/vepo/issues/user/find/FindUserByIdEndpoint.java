package dev.vepo.issues.user.find;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.UserPaths;
import dev.vepo.issues.user.UserResponse;
import dev.vepo.issues.user.UserService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(UserPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "User")
public class FindUserByIdEndpoint {

    private final UserService userService;

    @Inject
    public FindUserByIdEndpoint(UserService userService) {
        this.userService = userService;
    }

    @GET
    @Path("{userId}")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE, Role.USER_ROLE })
    @Operation(operationId = "findUserById", summary = "Find user by ID")
    public UserResponse findUserById(@PathParam("userId") long userId) {
        return userService.findById(userId);
    }
}

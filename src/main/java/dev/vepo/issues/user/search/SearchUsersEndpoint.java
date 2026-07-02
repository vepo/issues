package dev.vepo.issues.user.search;

import java.util.List;

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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path(UserPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "User")
public class SearchUsersEndpoint {

    private final UserService userService;

    @Inject
    public SearchUsersEndpoint(UserService userService) {
        this.userService = userService;
    }

    @GET
    @Path("search")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE, Role.USER_ROLE })
    @Operation(operationId = "searchUsers", summary = "Search users")
    public List<UserResponse> search(@QueryParam("name") String name,
                                     @QueryParam("email") String email,
                                     @QueryParam("roles") List<String> roles) {
        return userService.search(name, email, roles);
    }
}

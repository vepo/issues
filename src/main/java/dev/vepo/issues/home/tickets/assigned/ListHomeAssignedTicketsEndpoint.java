package dev.vepo.issues.home.tickets.assigned;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.home.HomePaths;
import dev.vepo.issues.home.HomeService;
import dev.vepo.issues.home.HomeTicketResponse;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(HomePaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Home")
public class ListHomeAssignedTicketsEndpoint {

    private final HomeService homeService;

    @Inject
    public ListHomeAssignedTicketsEndpoint(HomeService homeService) {
        this.homeService = homeService;
    }

    @GET
    @Path("tickets/assigned")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listHomeAssignedTickets", summary = "List open tickets assigned to the current user")
    public List<HomeTicketResponse> list(@Context SecurityContext securityContext) {
        return homeService.listAssignedTickets(securityContext.getUserPrincipal().getName());
    }
}

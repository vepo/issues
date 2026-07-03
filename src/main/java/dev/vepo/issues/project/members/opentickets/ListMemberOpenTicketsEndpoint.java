package dev.vepo.issues.project.members.opentickets;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectMemberService;
import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(ProjectPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Project")
public class ListMemberOpenTicketsEndpoint {

    private final ProjectMemberService memberService;

    @Inject
    public ListMemberOpenTicketsEndpoint(ProjectMemberService memberService) {
        this.memberService = memberService;
    }

    @GET
    @Path("{projectId}/members/{userId}/open-tickets")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listMemberOpenTickets", summary = "List open tickets assigned to a project member")
    public List<TicketResponse> list(@PathParam("projectId") long projectId,
                                     @PathParam("userId") long userId,
                                     @Context SecurityContext securityContext) {
        return memberService.listOpenAssignedTickets(projectId, userId, securityContext.getUserPrincipal().getName());
    }
}

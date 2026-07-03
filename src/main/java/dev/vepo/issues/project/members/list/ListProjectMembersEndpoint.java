package dev.vepo.issues.project.members.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectMemberResponse;
import dev.vepo.issues.project.ProjectMemberService;
import dev.vepo.issues.project.ProjectPaths;
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
public class ListProjectMembersEndpoint {

    private final ProjectMemberService memberService;

    @Inject
    public ListProjectMembersEndpoint(ProjectMemberService memberService) {
        this.memberService = memberService;
    }

    @GET
    @Path("{projectId}/members")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listProjectMembers", summary = "List project members")
    public List<ProjectMemberResponse> list(@PathParam("projectId") long projectId, @Context SecurityContext securityContext) {
        return memberService.listMembers(projectId, securityContext.getUserPrincipal().getName());
    }
}

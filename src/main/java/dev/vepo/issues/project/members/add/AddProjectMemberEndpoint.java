package dev.vepo.issues.project.members.add;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.project.AddProjectMemberRequest;
import dev.vepo.issues.project.ProjectMemberResponse;
import dev.vepo.issues.project.ProjectMemberService;
import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.user.Role;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(ProjectPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Project")
public class AddProjectMemberEndpoint {

    private final ProjectMemberService memberService;

    @Inject
    public AddProjectMemberEndpoint(ProjectMemberService memberService) {
        this.memberService = memberService;
    }

    @POST
    @Path("{projectId}/members")
    @ResponseStatus(201)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "addProjectMember", summary = "Add a project member")
    public ProjectMemberResponse add(@PathParam("projectId") long projectId,
                                     @Valid AddProjectMemberRequest request,
                                     @Context SecurityContext securityContext) {
        return memberService.addMember(projectId, request, securityContext.getUserPrincipal().getName());
    }
}

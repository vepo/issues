package dev.vepo.issues.project.customfield.inscope;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.customfield.CustomFieldResponse;
import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
@DenyAll
@Tag(name = "Project")
public class ListInScopeCustomFieldsEndpoint {

    private final CustomFieldService customFieldService;
    private final ProjectAccessService accessService;

    @Inject
    public ListInScopeCustomFieldsEndpoint(CustomFieldService customFieldService, ProjectAccessService accessService) {
        this.customFieldService = customFieldService;
        this.accessService = accessService;
    }

    @GET
    @Path("{projectId}/custom-fields/in-scope")
    @RolesAllowed({ Role.PROJECT_MANAGER_ROLE, Role.ADMIN_ROLE, Role.USER_ROLE })
    @Operation(operationId = "listInScopeCustomFields", summary = "List enabled project and workflow custom fields")
    public List<CustomFieldResponse> list(@PathParam("projectId") long projectId,
                                          @Context SecurityContext securityContext) {
        accessService.requireView(projectId, securityContext.getUserPrincipal().getName());
        var project = accessService.requireProject(projectId);
        return customFieldService.listInScope(projectId, project.getWorkflow().getId());
    }
}

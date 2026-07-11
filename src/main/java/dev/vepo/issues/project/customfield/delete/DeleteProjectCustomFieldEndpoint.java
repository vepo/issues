package dev.vepo.issues.project.customfield.delete;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path(ProjectPaths.BASE)
@ApplicationScoped
@DenyAll
@Tag(name = "Project")
public class DeleteProjectCustomFieldEndpoint {

    private final CustomFieldService customFieldService;
    private final ProjectAccessService accessService;

    @Inject
    public DeleteProjectCustomFieldEndpoint(CustomFieldService customFieldService, ProjectAccessService accessService) {
        this.customFieldService = customFieldService;
        this.accessService = accessService;
    }

    @DELETE
    @Path("{projectId}/custom-fields/{fieldId}")
    @ResponseStatus(204)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "deleteProjectCustomField", summary = "Delete a project custom field")
    public void delete(@PathParam("projectId") long projectId,
                       @PathParam("fieldId") long fieldId,
                       @Context SecurityContext securityContext) {
        accessService.requireManage(projectId, securityContext.getUserPrincipal().getName());
        customFieldService.deleteForProject(projectId, fieldId);
    }
}

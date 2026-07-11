package dev.vepo.issues.project.customfield.update;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.customfield.CustomFieldRequest;
import dev.vepo.issues.customfield.CustomFieldResponse;
import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
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
public class UpdateProjectCustomFieldEndpoint {

    private final CustomFieldService customFieldService;
    private final ProjectAccessService accessService;

    @Inject
    public UpdateProjectCustomFieldEndpoint(CustomFieldService customFieldService, ProjectAccessService accessService) {
        this.customFieldService = customFieldService;
        this.accessService = accessService;
    }

    @PUT
    @Path("{projectId}/custom-fields/{fieldId}")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "updateProjectCustomField", summary = "Update a project custom field")
    public CustomFieldResponse update(@PathParam("projectId") long projectId,
                                      @PathParam("fieldId") long fieldId,
                                      @Valid CustomFieldRequest request,
                                      @Context SecurityContext securityContext) {
        accessService.requireManage(projectId, securityContext.getUserPrincipal().getName());
        return customFieldService.updateForProject(projectId, fieldId, request);
    }
}

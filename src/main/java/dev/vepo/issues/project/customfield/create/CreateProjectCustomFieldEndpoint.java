package dev.vepo.issues.project.customfield.create;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

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
public class CreateProjectCustomFieldEndpoint {

    private final CustomFieldService customFieldService;
    private final ProjectAccessService accessService;

    @Inject
    public CreateProjectCustomFieldEndpoint(CustomFieldService customFieldService, ProjectAccessService accessService) {
        this.customFieldService = customFieldService;
        this.accessService = accessService;
    }

    @POST
    @Path("{projectId}/custom-fields")
    @ResponseStatus(201)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "createProjectCustomField", summary = "Create a project custom field")
    public CustomFieldResponse create(@PathParam("projectId") long projectId,
                                      @Valid CustomFieldRequest request,
                                      @Context SecurityContext securityContext) {
        accessService.requireManage(projectId, securityContext.getUserPrincipal().getName());
        var project = accessService.requireProject(projectId);
        return customFieldService.createForProject(projectId, project.getWorkflow().getId(), request);
    }
}

package dev.vepo.issues.workflow.customfield.update;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.customfield.CustomFieldRequest;
import dev.vepo.issues.customfield.CustomFieldResponse;
import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.workflow.WorkflowPaths;
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
import jakarta.ws.rs.core.MediaType;

@Path(WorkflowPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Workflow")
public class UpdateWorkflowCustomFieldEndpoint {

    private final CustomFieldService customFieldService;

    @Inject
    public UpdateWorkflowCustomFieldEndpoint(CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @PUT
    @Path("{workflowId}/custom-fields/{fieldId}")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "updateWorkflowCustomField", summary = "Update a workflow custom field")
    public CustomFieldResponse update(@PathParam("workflowId") long workflowId,
                                      @PathParam("fieldId") long fieldId,
                                      @Valid CustomFieldRequest request) {
        return customFieldService.updateForWorkflow(workflowId, fieldId, request);
    }
}

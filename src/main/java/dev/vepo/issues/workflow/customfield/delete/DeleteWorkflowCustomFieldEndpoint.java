package dev.vepo.issues.workflow.customfield.delete;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.workflow.WorkflowPaths;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path(WorkflowPaths.BASE)
@ApplicationScoped
@DenyAll
@Tag(name = "Workflow")
public class DeleteWorkflowCustomFieldEndpoint {

    private final CustomFieldService customFieldService;

    @Inject
    public DeleteWorkflowCustomFieldEndpoint(CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @DELETE
    @Path("{workflowId}/custom-fields/{fieldId}")
    @ResponseStatus(204)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "deleteWorkflowCustomField", summary = "Delete a workflow custom field")
    public void delete(@PathParam("workflowId") long workflowId, @PathParam("fieldId") long fieldId) {
        customFieldService.deleteForWorkflow(workflowId, fieldId);
    }
}

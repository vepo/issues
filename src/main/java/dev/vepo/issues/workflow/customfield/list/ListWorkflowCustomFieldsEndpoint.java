package dev.vepo.issues.workflow.customfield.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.customfield.CustomFieldResponse;
import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.workflow.WorkflowPaths;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(WorkflowPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Workflow")
public class ListWorkflowCustomFieldsEndpoint {

    private final CustomFieldService customFieldService;

    @Inject
    public ListWorkflowCustomFieldsEndpoint(CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @GET
    @Path("{workflowId}/custom-fields")
    @RolesAllowed({ Role.PROJECT_MANAGER_ROLE, Role.ADMIN_ROLE, Role.USER_ROLE })
    @Operation(operationId = "listWorkflowCustomFields", summary = "List workflow-owned custom fields")
    public List<CustomFieldResponse> list(@PathParam("workflowId") long workflowId) {
        return customFieldService.listByWorkflow(workflowId);
    }
}

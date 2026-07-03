package dev.vepo.issues.workflow.update;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.workflow.UpdateWorkflowRequest;
import dev.vepo.issues.workflow.WorkflowPaths;
import dev.vepo.issues.workflow.WorkflowResponse;
import dev.vepo.issues.workflow.WorkflowService;
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
public class UpdateWorkflowEndpoint {

    private final WorkflowService workflowService;

    @Inject
    public UpdateWorkflowEndpoint(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "updateWorkflow", summary = "Update workflow name, start status and transitions")
    public WorkflowResponse update(@PathParam("id") long id, @Valid UpdateWorkflowRequest request) {
        return workflowService.update(id, request);
    }
}

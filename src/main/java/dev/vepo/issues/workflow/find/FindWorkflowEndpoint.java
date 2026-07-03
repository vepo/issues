package dev.vepo.issues.workflow.find;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.workflow.WorkflowPaths;
import dev.vepo.issues.workflow.WorkflowResponse;
import dev.vepo.issues.workflow.WorkflowService;
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
public class FindWorkflowEndpoint {

    private final WorkflowService workflowService;

    @Inject
    public FindWorkflowEndpoint(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "findWorkflowById", summary = "Find workflow by id")
    public WorkflowResponse findById(@PathParam("id") long id) {
        return workflowService.findById(id);
    }
}

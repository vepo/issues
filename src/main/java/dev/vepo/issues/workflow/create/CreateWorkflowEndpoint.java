package dev.vepo.issues.workflow.create;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.workflow.CreateWorkflowRequest;
import dev.vepo.issues.workflow.WorkflowPaths;
import dev.vepo.issues.workflow.WorkflowResponse;
import dev.vepo.issues.workflow.WorkflowService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(WorkflowPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Workflow")
public class CreateWorkflowEndpoint {

    private final WorkflowService workflowService;

    @Inject
    public CreateWorkflowEndpoint(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @POST
    @ResponseStatus(201)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "createWorkflow", summary = "Create a workflow")
    public WorkflowResponse create(@Valid CreateWorkflowRequest request) {
        return workflowService.create(request);
    }
}

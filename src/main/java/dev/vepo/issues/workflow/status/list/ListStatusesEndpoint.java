package dev.vepo.issues.workflow.status.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.workflow.StatusResponse;
import dev.vepo.issues.workflow.WorkflowPaths;
import dev.vepo.issues.workflow.WorkflowService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(WorkflowPaths.STATUS)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Workflow")
public class ListStatusesEndpoint {

    private final WorkflowService workflowService;

    @Inject
    public ListStatusesEndpoint(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GET
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listStatuses", summary = "List all workflow statuses")
    public List<StatusResponse> listAll() {
        return workflowService.listAllStatuses();
    }
}

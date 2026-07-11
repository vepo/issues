package dev.vepo.issues.workflow.customfield.create;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

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
import jakarta.ws.rs.POST;
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
public class CreateWorkflowCustomFieldEndpoint {

    private final CustomFieldService customFieldService;

    @Inject
    public CreateWorkflowCustomFieldEndpoint(CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @POST
    @Path("{workflowId}/custom-fields")
    @ResponseStatus(201)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "createWorkflowCustomField", summary = "Create a workflow custom field")
    public CustomFieldResponse create(@PathParam("workflowId") long workflowId, @Valid CustomFieldRequest request) {
        return customFieldService.createForWorkflow(workflowId, request);
    }
}

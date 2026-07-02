package dev.vepo.issues.workflow;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.resteasy.reactive.ResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
public class WorkflowEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowEndpoint.class);

    private final WorkflowRepository repository;

    @Inject
    public WorkflowEndpoint(WorkflowRepository repository) {
        this.repository = repository;
    }

    @GET
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    public List<WorkflowResponse> listWorkflows() {
        return repository.findAll()
                         .map(WorkflowResponse::load)
                         .toList();
    }

    @POST
    @Transactional
    @ResponseStatus(201)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    public WorkflowResponse create(@Valid CreateWorkflowRequest request) {
        logger.debug("Processing create ticket request! request={}", request);
        var statuses = request.statuses()
                              .stream()
                              .map(status -> repository.findStatusByName(status)
                                                       .orElseGet(() -> {
                                                           var dbStatus = new WorkflowStatus(status);
                                                           repository.save(dbStatus);
                                                           return dbStatus;
                                                       }))
                              .collect(Collectors.toMap(w -> w.getName(), Function.identity()));
        logger.debug("All status exists on database! statuses={}", statuses);
        return WorkflowResponse.load(repository.save(new Workflow(request.name(),
                                                                  statuses.values()
                                                                          .stream()
                                                                          .toList(),
                                                                  statuses.get(request.start()),
                                                                  request.transitions().stream()
                                                                         .map(transition -> new WorkflowTransition(statuses.get(transition.from()),
                                                                                                                   statuses.get(transition.to())))
                                                                         .toList())));
    }
}

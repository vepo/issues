package dev.vepo.issues.workflow;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowRepository repository;

    @Inject
    public WorkflowService(WorkflowRepository repository) {
        this.repository = repository;
    }

    public List<WorkflowResponse> listAll() {
        return repository.findAll()
                         .map(WorkflowResponse::load)
                         .toList();
    }

    @Transactional
    public WorkflowResponse create(CreateWorkflowRequest request) {
        logger.debug("Processing create workflow request! request={}", request);
        var statuses = request.statuses()
                              .stream()
                              .map(status -> repository.findStatusByName(status)
                                                       .orElseGet(() -> {
                                                           var dbStatus = new WorkflowStatus(status);
                                                           repository.save(dbStatus);
                                                           return dbStatus;
                                                       }))
                              .collect(Collectors.toMap(WorkflowStatus::getName, Function.identity()));
        logger.debug("All status exists on database! statuses={}", statuses);
        return WorkflowResponse.load(repository.save(new Workflow(request.name(),
                                                                  statuses.values()
                                                                          .stream()
                                                                          .toList(),
                                                                  statuses.get(request.start()),
                                                                  request.transitions()
                                                                         .stream()
                                                                         .map(transition -> new WorkflowTransition(statuses.get(transition.from()),
                                                                                                                   statuses.get(transition.to())))
                                                                         .toList())));
    }

    public List<StatusResponse> listAllStatuses() {
        return repository.findAllStatus()
                         .map(StatusResponse::load)
                         .toList();
    }
}

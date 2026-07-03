package dev.vepo.issues.workflow;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

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

    public WorkflowResponse findById(long id) {
        return repository.findById(id)
                         .map(WorkflowResponse::load)
                         .orElseThrow(() -> new NotFoundException("Workflow not found! id=%d".formatted(id)));
    }

    @Transactional
    public WorkflowResponse update(long id, UpdateWorkflowRequest request) {
        var workflow = repository.findById(id)
                                 .orElseThrow(() -> new NotFoundException("Workflow not found! id=%d".formatted(id)));
        var statusNames = workflow.getStatuses()
                                  .stream()
                                  .map(WorkflowStatus::getName)
                                  .toList();
        if (!statusNames.contains(request.start())) {
            throw new BadRequestException("Start status is not part of this workflow");
        }
        for (var transition : request.transitions()) {
            if (!statusNames.contains(transition.from()) || !statusNames.contains(transition.to())) {
                throw new BadRequestException("Transition references unknown status for this workflow");
            }
        }
        var statuses = workflow.getStatuses()
                               .stream()
                               .collect(Collectors.toMap(WorkflowStatus::getName, Function.identity()));
        workflow.setName(request.name());
        workflow.setStart(statuses.get(request.start()));
        workflow.getTransitions().clear();
        workflow.getTransitions()
                .addAll(request.transitions()
                               .stream()
                               .map(transition -> new WorkflowTransition(statuses.get(transition.from()),
                                                                         statuses.get(transition.to())))
                               .toList());
        return WorkflowResponse.load(workflow);
    }
}

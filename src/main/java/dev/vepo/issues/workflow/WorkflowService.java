package dev.vepo.issues.workflow;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
        var statuses = resolveStatuses(request.statuses());
        logger.debug("All status exists on database! statuses={}", statuses);
        var workflow = repository.save(new Workflow(request.name(),
                                                    statuses.values()
                                                            .stream()
                                                            .toList(),
                                                    statuses.get(request.start()),
                                                    request.transitions()
                                                           .stream()
                                                           .map(transition -> new WorkflowTransition(statuses.get(transition.from()),
                                                                                                     statuses.get(transition.to())))
                                                           .toList()));
        applyPhaseStart(workflow, statuses, request.phaseStart());
        applyFinishStatuses(workflow, statuses, request.finishStatuses());
        applyWipLimits(workflow, statuses, request.wipLimits());
        return WorkflowResponse.load(workflow);
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
        applyPhaseStart(workflow, statuses, request.phaseStart());
        applyFinishStatuses(workflow, statuses, request.finishStatuses());
        applyWipLimits(workflow, statuses, request.wipLimits());
        return WorkflowResponse.load(workflow);
    }

    public Optional<FinishOutcome> findFinishOutcome(long workflowId, long statusId) {
        return repository.findFinishOutcome(workflowId, statusId);
    }

    private Map<String, WorkflowStatus> resolveStatuses(List<String> statusNames) {
        return statusNames.stream()
                          .map(status -> repository.findStatusByName(status)
                                                   .orElseGet(() -> {
                                                       var dbStatus = new WorkflowStatus(status);
                                                       repository.save(dbStatus);
                                                       return dbStatus;
                                                   }))
                          .collect(Collectors.toMap(WorkflowStatus::getName, Function.identity()));
    }

    private void applyPhaseStart(Workflow workflow, Map<String, WorkflowStatus> statuses, String phaseStartName) {
        if (Objects.isNull(phaseStartName) || phaseStartName.isBlank()) {
            workflow.setPhaseStart(null);
            return;
        }
        var phaseStart = statuses.get(phaseStartName);
        if (Objects.isNull(phaseStart)) {
            throw new BadRequestException("Phase start status is not part of this workflow");
        }
        workflow.setPhaseStart(phaseStart);
    }

    private void applyFinishStatuses(Workflow workflow, Map<String, WorkflowStatus> statuses, List<FinishStatusRequest> finishStatuses) {
        workflow.getFinishStatuses().clear();
        var requested = Optional.ofNullable(finishStatuses).orElseGet(Collections::emptyList);
        for (var finishStatus : requested) {
            var status = statuses.get(finishStatus.status());
            if (Objects.isNull(status)) {
                throw new BadRequestException("Finish status is not part of this workflow: %s".formatted(finishStatus.status()));
            }
            workflow.getFinishStatuses().add(new WorkflowFinishStatus(workflow, status, finishStatus.outcome()));
        }
    }

    private void applyWipLimits(Workflow workflow, Map<String, WorkflowStatus> statuses, List<StatusWipRequest> wipLimits) {
        workflow.getWipLimits().clear();
        var requested = Optional.ofNullable(wipLimits).orElseGet(Collections::emptyList);
        for (var wipLimit : requested) {
            var status = statuses.get(wipLimit.status());
            if (Objects.isNull(status)) {
                throw new BadRequestException("WIP limit status is not part of this workflow: %s".formatted(wipLimit.status()));
            }
            workflow.getWipLimits().add(new WorkflowWipLimit(workflow, status, wipLimit.wipLimit()));
        }
    }
}

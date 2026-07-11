package dev.vepo.issues.workflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.customfield.CustomFieldRepository;
import dev.vepo.issues.ticket.TicketService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowRepository repository;
    private final TicketService ticketService;
    private final CustomFieldRepository customFieldRepository;

    @Inject
    public WorkflowService(WorkflowRepository repository,
                           TicketService ticketService,
                           CustomFieldRepository customFieldRepository) {
        this.repository = repository;
        this.ticketService = ticketService;
        this.customFieldRepository = customFieldRepository;
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
    public WorkflowResponse update(long id, UpdateWorkflowRequest request, String username) {
        var workflow = repository.findById(id)
                                 .orElseThrow(() -> new NotFoundException("Workflow not found! id=%d".formatted(id)));
        var newStatusNames = request.statuses()
                                    .stream()
                                    .map(String::trim)
                                    .filter(name -> !name.isBlank())
                                    .toList();
        if (newStatusNames.size() < 2) {
            throw new BadRequestException("At least 2 statuses must be defined!");
        }
        if (new HashSet<>(newStatusNames).size() != newStatusNames.size()) {
            throw new BadRequestException("Status names must be unique");
        }
        if (!newStatusNames.contains(request.start())) {
            throw new BadRequestException("Start status is not part of this workflow");
        }
        for (var transition : request.transitions()) {
            if (!newStatusNames.contains(transition.from()) || !newStatusNames.contains(transition.to())) {
                throw new BadRequestException("Transition references unknown status for this workflow");
            }
        }

        var oldByName = workflow.getStatuses()
                                .stream()
                                .collect(Collectors.toMap(WorkflowStatus::getName, Function.identity()));
        var newStatuses = resolveStatuses(newStatusNames);
        var removedNames = oldByName.keySet()
                                    .stream()
                                    .filter(name -> !newStatuses.containsKey(name))
                                    .toList();
        var replacements = buildReplacementMap(request.statusReplacements(), newStatusNames);

        for (var removedName : removedNames) {
            var removed = oldByName.get(removedName);
            var ticketCount = ticketService.countTicketsOnWorkflowStatusIncludingDeleted(workflow.getId(), removed.getId());
            if (ticketCount > 0) {
                var replacementName = replacements.get(removedName);
                if (Objects.isNull(replacementName)) {
                    throw new BadRequestException("Status \"%s\" still has tickets; provide statusReplacements.to".formatted(removedName));
                }
                var replacement = newStatuses.get(replacementName);
                ticketService.remapWorkflowStatus(workflow.getId(), removed, replacement, username);
            }
        }

        dropStatusRequiredLinks(workflow.getId(), removedNames.stream().map(oldByName::get).map(WorkflowStatus::getId).toList());

        workflow.setName(request.name());
        workflow.setStatuses(newStatusNames.stream().map(newStatuses::get).toList());
        workflow.setStart(newStatuses.get(request.start()));
        workflow.getTransitions().clear();
        workflow.getTransitions()
                .addAll(request.transitions()
                               .stream()
                               .map(transition -> new WorkflowTransition(newStatuses.get(transition.from()),
                                                                         newStatuses.get(transition.to())))
                               .toList());
        applyPhaseStart(workflow, newStatuses, request.phaseStart());
        applyFinishStatuses(workflow, newStatuses, request.finishStatuses());
        applyWipLimits(workflow, newStatuses, request.wipLimits());
        return WorkflowResponse.load(workflow);
    }

    public Optional<FinishOutcome> findFinishOutcome(long workflowId, long statusId) {
        return repository.findFinishOutcome(workflowId, statusId);
    }

    private Map<String, String> buildReplacementMap(List<StatusReplacementRequest> statusReplacements, List<String> newStatusNames) {
        var map = new HashMap<String, String>();
        for (var replacement : Optional.ofNullable(statusReplacements).orElseGet(Collections::emptyList)) {
            if (!newStatusNames.contains(replacement.to())) {
                throw new BadRequestException("Replacement status is not part of this workflow: %s".formatted(replacement.to()));
            }
            if (replacement.from().equals(replacement.to())) {
                throw new BadRequestException("Replacement status must differ from removed status");
            }
            if (map.put(replacement.from(), replacement.to()) != null) {
                throw new BadRequestException("Duplicate status replacement for %s".formatted(replacement.from()));
            }
        }
        return map;
    }

    private void dropStatusRequiredLinks(long workflowId, List<Long> removedStatusIds) {
        if (removedStatusIds.isEmpty()) {
            return;
        }
        var removed = new HashSet<>(removedStatusIds);
        for (var field : customFieldRepository.listByWorkflowId(workflowId)) {
            field.getStatusRequired().removeIf(link -> removed.contains(link.getStatusId()));
        }
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
        repository.flush();
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
        var requested = Optional.ofNullable(wipLimits).orElseGet(Collections::emptyList);
        var requestedStatusIds = new HashSet<Long>();
        for (var wipLimit : requested) {
            var status = statuses.get(wipLimit.status());
            if (Objects.isNull(status)) {
                throw new BadRequestException("WIP limit status is not part of this workflow: %s".formatted(wipLimit.status()));
            }
            requestedStatusIds.add(status.getId());
            var existing = workflow.getWipLimits()
                                   .stream()
                                   .filter(limit -> limit.getStatus().getId().equals(status.getId()))
                                   .findFirst();
            if (existing.isPresent()) {
                existing.get().setWipLimit(wipLimit.wipLimit());
            } else {
                workflow.getWipLimits().add(new WorkflowWipLimit(workflow, status, wipLimit.wipLimit()));
            }
        }
        workflow.getWipLimits().removeIf(limit -> !requestedStatusIds.contains(limit.getStatus().getId()));
    }
}

package dev.vepo.issues.phase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import dev.vepo.issues.project.Project;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.project.ProjectPhaseDeliverableTemplate;
import dev.vepo.issues.project.ProjectRepository;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.history.TicketHistoryService;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import dev.vepo.issues.workflow.FinishOutcome;
import dev.vepo.issues.workflow.WorkflowRepository;
import dev.vepo.issues.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class PhaseService {

    private final PhaseRepository phaseRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;
    private final VersionService versionService;
    private final TicketRepository ticketRepository;
    private final WorkflowRepository workflowRepository;
    private final TicketHistoryService historyService;
    private final UserRepository userRepository;

    @Inject
    public PhaseService(PhaseRepository phaseRepository,
                        ProjectRepository projectRepository,
                        ProjectAccessService projectAccessService,
                        VersionService versionService,
                        TicketRepository ticketRepository,
                        WorkflowRepository workflowRepository,
                        TicketHistoryService historyService,
                        UserRepository userRepository) {
        this.phaseRepository = phaseRepository;
        this.projectRepository = projectRepository;
        this.projectAccessService = projectAccessService;
        this.versionService = versionService;
        this.ticketRepository = ticketRepository;
        this.workflowRepository = workflowRepository;
        this.historyService = historyService;
        this.userRepository = userRepository;
    }

    public List<PhaseResponse> listByProject(long projectId, String username) {
        projectAccessService.requireRead(projectId, Optional.ofNullable(username));
        requireProject(projectId);
        return phaseRepository.findByProjectId(projectId)
                              .map(PhaseResponse::load)
                              .toList();
    }

    public PhaseResponse findById(long projectId, long phaseId, String username) {
        projectAccessService.requireRead(projectId, Optional.ofNullable(username));
        return PhaseResponse.load(requirePhase(projectId, phaseId));
    }

    public PhaseResponse findActive(long projectId, String username) {
        projectAccessService.requireRead(projectId, Optional.ofNullable(username));
        requireProject(projectId);
        return phaseRepository.findActiveByProjectId(projectId)
                              .map(PhaseResponse::load)
                              .orElseThrow(() -> new NotFoundException("No active phase for project! projectId=%d".formatted(projectId)));
    }

    @Transactional
    public PhaseResponse create(long projectId, CreatePhaseRequest request, String username) {
        projectAccessService.requireManage(projectId, username);
        var project = requireProject(projectId);
        var objective = request.objective();
        if (objective == null || objective.isBlank()) {
            objective = project.getPhaseTemplateObjective();
        }
        var phase = new Phase(project,
                              request.name().trim(),
                              objective,
                              PhaseStatus.PLANNED,
                              LocalDateTime.now());
        var deliverables = request.deliverables();
        if (deliverables == null || deliverables.isEmpty()) {
            deliverables = project.getPhaseDeliverableTemplates()
                                  .stream()
                                  .map(ProjectPhaseDeliverableTemplate::getText)
                                  .toList();
        }
        applyFields(phase, projectId, request.startDate(), request.endDate(), request.deliverableVersionId(), deliverables);
        return PhaseResponse.load(phaseRepository.save(phase));
    }

    @Transactional
    public PhaseResponse update(long projectId, long phaseId, UpdatePhaseRequest request, String username) {
        projectAccessService.requireManage(projectId, username);
        var phase = requirePhase(projectId, phaseId);
        phase.setName(request.name().trim());
        phase.setObjective(request.objective());
        applyFields(phase, projectId, request.startDate(), request.endDate(), request.deliverableVersionId(), request.deliverables());
        return PhaseResponse.load(phase);
    }

    @Transactional
    public PhaseResponse activate(long projectId, long phaseId, String username) {
        projectAccessService.requireManage(projectId, username);
        var phase = requirePhase(projectId, phaseId);
        if (phase.getStatus() != PhaseStatus.PLANNED) {
            throw new BadRequestException("Only planned phases can be activated! phaseId=%d status=%s".formatted(phaseId, phase.getStatus()));
        }
        var user = requireUser(username);
        phaseRepository.findActiveByProjectId(projectId)
                       .ifPresent(this::completePhase);
        phase.setStatus(PhaseStatus.ACTIVE);
        moveAssignedTicketsToPhaseStart(phase, user);
        return PhaseResponse.load(phase);
    }

    @Transactional
    public PhaseResponse complete(long projectId, long phaseId, String username) {
        projectAccessService.requireManage(projectId, username);
        var phase = requirePhase(projectId, phaseId);
        if (phase.getStatus() != PhaseStatus.ACTIVE) {
            throw new BadRequestException("Only active phases can be completed! phaseId=%d status=%s".formatted(phaseId, phase.getStatus()));
        }
        completePhase(phase);
        return PhaseResponse.load(phase);
    }

    public Phase requirePhase(long projectId, long phaseId) {
        return phaseRepository.findByIdAndProjectId(phaseId, projectId)
                              .orElseThrow(() -> new NotFoundException("Phase not found! projectId=%d phaseId=%d".formatted(projectId, phaseId)));
    }

    public Phase requireAssignablePhase(long projectId, Long phaseId) {
        if (Objects.isNull(phaseId)) {
            return null;
        }
        var phase = requirePhase(projectId, phaseId);
        if (phase.getStatus() == PhaseStatus.COMPLETED) {
            throw new BadRequestException("Completed phases cannot be assigned to tickets! phaseId=%d".formatted(phaseId));
        }
        return phase;
    }

    private void completePhase(Phase phase) {
        if (phase.getStatus() == PhaseStatus.COMPLETED) {
            return;
        }
        phase.setStatus(PhaseStatus.COMPLETED);
        phase.setCompletedAt(LocalDateTime.now());
    }

    private void moveAssignedTicketsToPhaseStart(Phase phase, User user) {
        var phaseStart = phase.getProject().getWorkflow().getPhaseStart();
        if (Objects.isNull(phaseStart)) {
            return;
        }
        ticketRepository.findByPhaseId(phase.getId())
                        .forEach(ticket -> moveToStatusIfTransitionExists(ticket, phaseStart, user));
    }

    private void moveToStatusIfTransitionExists(Ticket ticket, WorkflowStatus to, User user) {
        if (ticket.getStatus().equals(to)) {
            return;
        }
        var workflow = ticket.getProject().getWorkflow();
        var hasTransition = workflow.getTransitions()
                                    .stream()
                                    .anyMatch(t -> t.getFrom().equals(ticket.getStatus()) && t.getTo().equals(to));
        if (!hasTransition) {
            return;
        }
        var fromStatus = ticket.getStatus().getName();
        var workflowId = workflow.getId();
        var fromFinishOutcome = workflowRepository.findFinishOutcome(workflowId, ticket.getStatus().getId());
        var toFinishOutcome = workflowRepository.findFinishOutcome(workflowId, to.getId());
        ticket.setStatus(to);
        applyFinishDate(ticket, fromFinishOutcome, toFinishOutcome, user);
        historyService.logStatusChanged(ticket, user, fromStatus, to.getName());
        ticket.setUpdatedAt(LocalDateTime.now());
    }

    private void applyFinishDate(Ticket ticket, Optional<FinishOutcome> fromOutcome, Optional<FinishOutcome> toOutcome, User user) {
        var to = toOutcome.orElse(null);
        var from = fromOutcome.orElse(null);

        if (to == FinishOutcome.DONE) {
            var previous = ticket.getFinishedAt();
            ticket.setFinishedAt(LocalDateTime.now());
            historyService.logFieldChanged(ticket, user, "finishedAt", formatDateTime(previous), formatDateTime(ticket.getFinishedAt()));
            clearCanceledAt(ticket, user);
        } else if (from == FinishOutcome.DONE && to != FinishOutcome.CANCELED) {
            var previous = ticket.getFinishedAt();
            ticket.setFinishedAt(null);
            historyService.logFieldChanged(ticket, user, "finishedAt", formatDateTime(previous), null);
        }

        if (to == FinishOutcome.CANCELED) {
            var previous = ticket.getCanceledAt();
            ticket.setCanceledAt(LocalDateTime.now());
            historyService.logFieldChanged(ticket, user, "canceledAt", formatDateTime(previous), formatDateTime(ticket.getCanceledAt()));
            clearFinishedAt(ticket, user);
        } else if (from == FinishOutcome.CANCELED && to != FinishOutcome.DONE) {
            var previous = ticket.getCanceledAt();
            ticket.setCanceledAt(null);
            historyService.logFieldChanged(ticket, user, "canceledAt", formatDateTime(previous), null);
        }
    }

    private void clearFinishedAt(Ticket ticket, User user) {
        if (ticket.getFinishedAt() == null) {
            return;
        }
        var previous = ticket.getFinishedAt();
        ticket.setFinishedAt(null);
        historyService.logFieldChanged(ticket, user, "finishedAt", formatDateTime(previous), null);
    }

    private void clearCanceledAt(Ticket ticket, User user) {
        if (ticket.getCanceledAt() == null) {
            return;
        }
        var previous = ticket.getCanceledAt();
        ticket.setCanceledAt(null);
        historyService.logFieldChanged(ticket, user, "canceledAt", formatDateTime(previous), null);
    }

    private String formatDateTime(LocalDateTime value) {
        return Objects.nonNull(value) ? value.toString() : null;
    }

    private void applyFields(Phase phase,
                             long projectId,
                             java.time.LocalDate startDate,
                             java.time.LocalDate endDate,
                             Long deliverableVersionId,
                             List<String> deliverables) {
        phase.setStartDate(startDate);
        phase.setEndDate(endDate);
        phase.setDeliverableVersion(deliverableVersionId != null ? versionService.requireVersion(projectId, deliverableVersionId) : null);
        replaceDeliverables(phase, deliverables);
    }

    private void replaceDeliverables(Phase phase, List<String> deliverables) {
        phase.getDeliverables().clear();
        if (Objects.isNull(deliverables)) {
            return;
        }
        var order = 0;
        for (var text : deliverables) {
            if (Objects.isNull(text) || text.isBlank()) {
                continue;
            }
            phase.getDeliverables().add(new PhaseDeliverable(phase, order++, text.trim()));
        }
    }

    private Project requireProject(long projectId) {
        return projectRepository.findById(projectId)
                                .orElseThrow(() -> new NotFoundException("Project not found! projectId=%d".formatted(projectId)));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                             .orElseThrow(() -> new NotFoundException("User not found! username=%s".formatted(username)));
    }
}

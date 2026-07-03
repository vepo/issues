package dev.vepo.issues.ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.notifications.NotificationEvent;
import dev.vepo.issues.phase.Phase;
import dev.vepo.issues.phase.PhaseService;
import dev.vepo.issues.phase.Version;
import dev.vepo.issues.phase.VersionService;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.project.ProjectMemberRepository;
import dev.vepo.issues.project.ProjectRepository;
import dev.vepo.issues.ticket.comments.Comment;
import dev.vepo.issues.ticket.comments.CommentRequest;
import dev.vepo.issues.ticket.comments.CommentResponse;
import dev.vepo.issues.ticket.history.TicketHistory;
import dev.vepo.issues.ticket.history.TicketHistoryService;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import dev.vepo.issues.workflow.FinishOutcome;
import dev.vepo.issues.workflow.WorkflowRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    private static final Predicate<String> IS_NUMBER = Pattern.compile("\\d+").asMatchPredicate();

    private final TicketRepository repository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final TicketHistoryService historyService;
    private final WorkflowRepository workflowRepository;
    private final VersionService versionService;
    private final PhaseService phaseService;
    private final ProjectMemberRepository memberRepository;
    private final ProjectAccessService projectAccessService;
    private final Event<NotificationEvent> notificationEmitter;

    @Inject
    public TicketService(TicketRepository repository,
                         UserRepository userRepository,
                         ProjectRepository projectRepository,
                         CategoryRepository categoryRepository,
                         TicketHistoryService historyService,
                         WorkflowRepository workflowRepository,
                         VersionService versionService,
                         PhaseService phaseService,
                         ProjectMemberRepository memberRepository,
                         ProjectAccessService projectAccessService,
                         Event<NotificationEvent> notificationEmitter) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.categoryRepository = categoryRepository;
        this.historyService = historyService;
        this.workflowRepository = workflowRepository;
        this.versionService = versionService;
        this.phaseService = phaseService;
        this.memberRepository = memberRepository;
        this.projectAccessService = projectAccessService;
        this.notificationEmitter = notificationEmitter;
    }

    @Transactional
    public List<TicketResponse> listAll(String status) {
        if (Objects.nonNull(status) && IS_NUMBER.test(status)) {
            return repository.findByStatusId(Long.parseLong(status))
                             .map(TicketResponse::load)
                             .toList();
        } else if (Objects.nonNull(status) && !status.isBlank()) {
            return repository.findByStatusName(status)
                             .map(TicketResponse::load)
                             .toList();
        }
        return repository.findAll()
                         .map(TicketResponse::load)
                         .toList();
    }

    @Transactional
    public List<TicketResponse> search(String term, long statusId) {
        return repository.search(Optional.ofNullable(term)
                                         .filter(Predicate.not(String::isBlank))
                                         .map(String::trim)
                                         .map(s -> s.split("\\s+"))
                                         .orElseGet(() -> new String[] {}),
                                 statusId)
                         .map(TicketResponse::load)
                         .toList();
    }

    @Transactional
    public List<TicketResponse> findByProjectId(long projectId, String username) {
        projectAccessService.requireView(projectId, username);
        return repository.findByProjectId(projectId)
                         .map(TicketResponse::load)
                         .toList();
    }

    @Transactional
    public TicketResponse findById(long id) {
        return TicketResponse.load(requireTicket(id));
    }

    @Transactional
    public TicketExpandedResponse findExpandedById(long id) {
        return TicketExpandedResponse.load(requireTicket(id), loadHistory(id));
    }

    @Transactional
    public TicketExpandedResponse findExpandedByIdentifier(String identifier) {
        var ticket = requireTicketByIdentifier(identifier);
        return TicketExpandedResponse.load(ticket, loadHistory(ticket.getId()));
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request, String authorUsername) {
        var project = projectRepository.findById(request.projectId())
                                       .orElseThrow(() -> projectNotFound(request.projectId()));
        var author = requireUserByUsername(authorUsername);
        var projectTickets = repository.countProjectTickets(request.projectId());
        var ticket = new Ticket("%s-%03d".formatted(project.getPrefix(), projectTickets + 1),
                                request.title(),
                                request.description(),
                                categoryRepository.findById(request.categoryId())
                                                  .orElseThrow(() -> categoryNotFound(request.categoryId())),
                                author,
                                null,
                                project,
                                project.getWorkflow().getStart());
        ticket.setPriority(Objects.nonNull(request.priority()) ? request.priority() : TicketPriority.MEDIUM);
        ticket.setDueDate(request.dueDate());
        ticket.setPhase(phaseService.requireAssignablePhase(project.getId(), request.phaseId()));
        repository.save(ticket);
        historyService.logTicketCreated(ticket, author);
        return TicketResponse.load(ticket);
    }

    @Transactional
    public TicketResponse update(long id, UpdateTicketRequest request, String username) {
        var entity = requireTicket(id);
        var user = requireUserByUsername(username);

        if (!entity.getTitle().equals(request.title())) {
            historyService.logFieldChanged(entity, user, "title", entity.getTitle(), request.title());
        }
        if (!entity.getDescription().equals(request.description())) {
            historyService.logFieldChanged(entity, user, "description", entity.getDescription(), request.description());
        }
        var newCategory = categoryRepository.findById(request.categoryId())
                                            .orElseThrow(() -> categoryNotFound(request.categoryId()));
        if (!entity.getCategory().equals(newCategory)) {
            historyService.logFieldChanged(entity,
                                           user,
                                           "category",
                                           entity.getCategory().getName(),
                                           newCategory.getName());
        }
        if (!entity.getPriority().equals(request.priority())) {
            historyService.logPriorityChanged(entity, user, entity.getPriority().name(), request.priority().name());
        }
        if (!Objects.equals(entity.getDueDate(), request.dueDate())) {
            historyService.logFieldChanged(entity,
                                           user,
                                           "dueDate",
                                           formatDueDate(entity.getDueDate()),
                                           formatDueDate(request.dueDate()));
        }
        if (Objects.nonNull(request.planningFields())) {
            applyPhaseChange(entity,
                             user,
                             entity.getPhase(),
                             resolvePhase(entity, request.planningFields().phaseId()),
                             entity::setPhase);
            applyVersionChange(entity,
                               user,
                               "observedVersion",
                               entity.getObservedVersion(),
                               resolveVersion(entity, request.planningFields().observedVersionId()),
                               entity::setObservedVersion);
            applyVersionChange(entity,
                               user,
                               "targetVersion",
                               entity.getTargetVersion(),
                               resolveVersion(entity, request.planningFields().targetVersionId()),
                               entity::setTargetVersion);
        }

        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setCategory(newCategory);
        entity.setPriority(request.priority());
        entity.setDueDate(request.dueDate());
        entity.setUpdatedAt(LocalDateTime.now());

        return TicketResponse.load(entity);
    }

    @Transactional
    public TicketResponse updateAssignee(long id, UpdateAssigneeRequest request, String username) {
        var entity = requireTicket(id);
        var newAssignee = requireUserById(request.assigneeId());
        requireAssigneeIsProjectMember(entity.getProject().getId(), newAssignee.getId());
        var fromAssignee = entity.getAssignee() != null ? entity.getAssignee().getName() : null;
        var toAssignee = newAssignee.getName();

        entity.setAssignee(newAssignee);
        entity.setUpdatedAt(LocalDateTime.now());

        var user = requireUserByUsername(username);
        if (!java.util.Objects.equals(fromAssignee, toAssignee)) {
            historyService.logAssigneeChanged(entity, user, fromAssignee, toAssignee);
        }
        return TicketResponse.load(entity);
    }

    @Transactional
    public void delete(long id, String username) {
        var ticket = requireTicket(id);
        var user = requireUserByUsername(username);
        historyService.logTicketDeleted(ticket, user);
        repository.delete(id);
    }

    public List<CommentResponse> listComments(long id) {
        return repository.findCommentsByTicketId(id)
                         .map(CommentResponse::load)
                         .toList();
    }

    @Transactional
    public CommentResponse addComment(long id, CommentRequest request, String username) {
        var ticket = requireTicket(id);
        var user = requireUserByUsername(username);
        var comment = repository.saveComment(new Comment(ticket, user, request.content()));
        return CommentResponse.load(comment);
    }

    @Transactional
    public TicketResponse moveTicket(long id, MoveTicketRequest request, String username) {
        logger.debug("Moving ticket to a new status! ticketId={}, request={}", id, request);
        var ticket = requireTicket(id);

        if (Objects.isNull(request) || Objects.isNull(request.to())) {
            throw new BadRequestException("Destino não informado");
        }
        var to = ticket.getProject()
                       .getWorkflow()
                       .getStatuses()
                       .stream()
                       .filter(s -> Objects.equals(s.getId(), request.to()))
                       .findFirst()
                       .orElseThrow(() -> new BadRequestException("Stage not defined in project! stageId=%d".formatted(request.to())));
        if (ticket.getProject()
                  .getWorkflow()
                  .getTransitions()
                  .stream()
                  .noneMatch(t -> t.getTo().equals(to) && t.getFrom().equals(ticket.getStatus()))) {
            throw new BadRequestException("New stage not acceptable by workflow! stageId=%d".formatted(request.to()));
        }

        var fromStatus = ticket.getStatus().getName();
        var toStatus = to.getName();
        var workflowId = ticket.getProject().getWorkflow().getId();
        var fromFinishOutcome = workflowRepository.findFinishOutcome(workflowId, ticket.getStatus().getId());
        var toFinishOutcome = workflowRepository.findFinishOutcome(workflowId, to.getId());
        var user = requireUserByUsername(username);
        ticket.setStatus(to);
        applyFinishDate(ticket, fromFinishOutcome, toFinishOutcome, user);
        historyService.logStatusChanged(ticket, user, fromStatus, toStatus);
        logger.info("Enviando evento CDI!");
        notificationEmitter.fireAsync(new NotificationEvent(ticket.getId(),
                                                            "ticket-moved",
                                                            "Ticket %s mudou de status! %s alterou de %s para %s".formatted(ticket.getIdentifier(),
                                                                                                                            username,
                                                                                                                            fromStatus,
                                                                                                                            toStatus)));
        return TicketResponse.load(ticket);
    }

    public List<TicketHistoryResponse> getHistory(long id) {
        return repository.findHistoryByTicketId(id)
                         .map(TicketHistoryResponse::load)
                         .toList();
    }

    @Transactional
    public TicketExpandedResponse subscribe(long id, SubscribeTicketRequest request, String username) {
        var ticket = requireTicket(id);
        var subscriber = requireUserById(request.subscriberId());
        var actor = requireUserByUsername(username);
        if (ticket.getSubscribers().stream().anyMatch(u -> u.getId().equals(subscriber.getId()))) {
            return TicketExpandedResponse.load(ticket, loadHistory(id));
        }
        ticket.getSubscribers()
              .add(subscriber);
        historyService.logSubscribed(ticket, actor, subscriber.getName());
        return TicketExpandedResponse.load(repository.save(ticket), loadHistory(id));
    }

    @Transactional
    public TicketExpandedResponse unsubscribe(long id, long subscriberId, String username) {
        var ticket = requireTicket(id);
        var subscriber = requireUserById(subscriberId);
        var actor = requireUserByUsername(username);
        var wasSubscribed = ticket.getSubscribers()
                                  .removeIf(user -> subscriberId == user.getId());
        if (wasSubscribed) {
            historyService.logUnsubscribed(ticket, actor, subscriber.getName());
        }
        return TicketExpandedResponse.load(repository.save(ticket), loadHistory(id));
    }

    public List<Ticket> findTicketsByProjectId(long projectId) {
        return repository.findByProjectId(projectId).toList();
    }

    private List<TicketHistory> loadHistory(long id) {
        return repository.findHistoryByTicketId(id).toList();
    }

    private Ticket requireTicket(long id) {
        return repository.findById(id)
                         .orElseThrow(() -> ticketNotFound(id));
    }

    private Ticket requireTicketByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                         .orElseThrow(() -> ticketNotFound(identifier));
    }

    private User requireUserByUsername(String username) {
        return userRepository.findByUsername(username)
                             .orElseThrow(() -> userNotFound(username));
    }

    private User requireUserById(long userId) {
        return userRepository.findById(userId)
                             .orElseThrow(() -> userNotFound(userId));
    }

    private NotFoundException ticketNotFound(long ticketId) {
        return new NotFoundException("Ticket does not found! ticketId=%d".formatted(ticketId));
    }

    private NotFoundException ticketNotFound(String ticketIdentifier) {
        return new NotFoundException("Ticket does not found! ticketIdentifier=%s".formatted(ticketIdentifier));
    }

    private NotFoundException userNotFound(long userId) {
        return new NotFoundException("User does not found! userId=%d".formatted(userId));
    }

    private NotFoundException userNotFound(String username) {
        return new NotFoundException("User does not found! username=%s".formatted(username));
    }

    private NotFoundException projectNotFound(long projectId) {
        return new NotFoundException("Project does not found! projectId=%d".formatted(projectId));
    }

    private NotFoundException categoryNotFound(long categoryId) {
        return new NotFoundException("Category does not found! categoryId=%d".formatted(categoryId));
    }

    private void requireAssigneeIsProjectMember(long projectId, long assigneeId) {
        if (!memberRepository.isMember(projectId, assigneeId)) {
            throw new BadRequestException("Assignee must be a member of the project");
        }
    }

    private void applyFinishDate(Ticket ticket,
                                 Optional<FinishOutcome> fromOutcome,
                                 Optional<FinishOutcome> toOutcome,
                                 User user) {
        if (toOutcome.orElse(null) == FinishOutcome.DONE) {
            var previous = ticket.getFinishedAt();
            ticket.setFinishedAt(LocalDateTime.now());
            historyService.logFieldChanged(ticket,
                                           user,
                                           "finishedAt",
                                           formatDateTime(previous),
                                           formatDateTime(ticket.getFinishedAt()));
        } else if (fromOutcome.orElse(null) == FinishOutcome.DONE) {
            var previous = ticket.getFinishedAt();
            ticket.setFinishedAt(null);
            historyService.logFieldChanged(ticket, user, "finishedAt", formatDateTime(previous), null);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return Objects.nonNull(value) ? value.toString() : null;
    }

    private static String formatDueDate(LocalDate value) {
        return Objects.nonNull(value) ? value.toString() : null;
    }

    private Version resolveVersion(Ticket ticket, Long versionId) {
        return versionService.requireVersionForTicket(ticket.getProject().getId(), versionId);
    }

    private Phase resolvePhase(Ticket ticket, Long phaseId) {
        return phaseService.requireAssignablePhase(ticket.getProject().getId(), phaseId);
    }

    private void applyPhaseChange(Ticket ticket,
                                  User user,
                                  Phase current,
                                  Phase next,
                                  java.util.function.Consumer<Phase> setter) {
        var currentName = current != null ? current.getName() : null;
        var nextName = next != null ? next.getName() : null;
        if (!Objects.equals(currentName, nextName)) {
            historyService.logFieldChanged(ticket, user, "phase", currentName, nextName);
            setter.accept(next);
        }
    }

    private void applyVersionChange(Ticket ticket,
                                    User user,
                                    String field,
                                    Version current,
                                    Version next,
                                    java.util.function.Consumer<Version> setter) {
        var currentLabel = current != null ? current.getLabel() : null;
        var nextLabel = next != null ? next.getLabel() : null;
        if (!Objects.equals(currentLabel, nextLabel)) {
            historyService.logFieldChanged(ticket, user, field, currentLabel, nextLabel);
            setter.accept(next);
        }
    }
}

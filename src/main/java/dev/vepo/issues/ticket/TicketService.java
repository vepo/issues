package dev.vepo.issues.ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.auth.apitoken.ApiTokenIdentityProvider;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.customfield.CustomFieldValueResponse;
import dev.vepo.issues.git.GitCommitService;
import dev.vepo.issues.infra.HtmlSanitizer;
import dev.vepo.issues.notifications.NotificationEvent;
import dev.vepo.issues.notifications.NotificationService;
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
import dev.vepo.issues.ticket.comments.MentionParser;
import dev.vepo.issues.ticket.history.TicketHistory;
import dev.vepo.issues.ticket.history.TicketHistoryService;
import dev.vepo.issues.ticket.backlog.BacklogService;
import dev.vepo.issues.ticket.link.TicketLinkService;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import dev.vepo.issues.workflow.FinishOutcome;
import dev.vepo.issues.workflow.WorkflowRepository;
import dev.vepo.issues.workflow.WorkflowStatus;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
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
    private final CustomFieldService customFieldService;
    private final Event<NotificationEvent> notificationEmitter;
    private final NotificationService notificationService;
    private final Provider<TicketLinkService> ticketLinkService;
    private final BacklogService backlogService;
    private final SecurityIdentity securityIdentity;
    private final HtmlSanitizer htmlSanitizer;
    private final GitCommitService gitCommitService;

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
                         CustomFieldService customFieldService,
                         Event<NotificationEvent> notificationEmitter,
                         NotificationService notificationService,
                         Provider<TicketLinkService> ticketLinkService,
                         BacklogService backlogService,
                         SecurityIdentity securityIdentity,
                         HtmlSanitizer htmlSanitizer,
                         GitCommitService gitCommitService) {
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
        this.customFieldService = customFieldService;
        this.notificationEmitter = notificationEmitter;
        this.notificationService = notificationService;
        this.ticketLinkService = ticketLinkService;
        this.backlogService = backlogService;
        this.securityIdentity = securityIdentity;
        this.htmlSanitizer = htmlSanitizer;
        this.gitCommitService = gitCommitService;
    }

    @Transactional
    public List<TicketResponse> listAll(String status) {
        return listAll(status, null);
    }

    @Transactional
    public List<TicketResponse> listAll(String status, String username) {
        var stream = listAllStream(status);
        if (username != null) {
            var readable = projectAccessService.readableProjectIds(projectAccessService.requireUser(username));
            stream = stream.filter(ticket -> readable.contains(ticket.getProject().getId()));
        }
        return stream.map(this::toResponse).toList();
    }

    private java.util.stream.Stream<Ticket> listAllStream(String status) {
        if (Objects.nonNull(status) && IS_NUMBER.test(status)) {
            return repository.findByStatusId(Long.parseLong(status));
        } else if (Objects.nonNull(status) && !status.isBlank()) {
            return repository.findByStatusName(status);
        }
        return repository.findAll();
    }

    @Transactional
    public List<TicketResponse> search(String term, long statusId) {
        return search(term, statusId, null);
    }

    @Transactional
    public List<TicketResponse> search(String term, long statusId, String username) {
        var stream = repository.search(Optional.ofNullable(term)
                                               .filter(Predicate.not(String::isBlank))
                                               .map(String::trim)
                                               .map(s -> s.split("\\s+"))
                                               .orElseGet(() -> new String[] {}),
                                       statusId);
        if (username != null) {
            var readable = projectAccessService.readableProjectIds(projectAccessService.requireUser(username));
            stream = stream.filter(ticket -> readable.contains(ticket.getProject().getId()));
        }
        return stream.map(this::toResponse).toList();
    }

    @Transactional
    public List<TicketResponse> findByProjectId(long projectId, String username) {
        projectAccessService.requireRead(projectId, username);
        return repository.findByProjectId(projectId)
                         .map(this::toResponse)
                         .toList();
    }

    @Transactional
    public List<TicketResponse> findByProjectId(long projectId, java.util.Optional<String> username) {
        projectAccessService.requireRead(projectId, username);
        return repository.findByProjectId(projectId)
                         .map(this::toResponse)
                         .toList();
    }

    @Transactional
    public TicketResponse findById(long id) {
        return toResponse(requireTicket(id));
    }

    @Transactional
    public TicketResponse findById(long id, java.util.Optional<String> username) {
        var ticket = requireTicket(id);
        projectAccessService.requireRead(ticket.getProject().getId(), username);
        return toResponse(ticket);
    }

    @Transactional
    public TicketExpandedResponse findExpandedById(long id, String username) {
        var ticket = requireTicketForView(id, username);
        projectAccessService.requireRead(ticket.getProject().getId(), username);
        var user = requireUserByUsername(username);
        return toExpandedResponse(ticket, loadHistory(id), user);
    }

    @Transactional
    public TicketExpandedResponse findExpandedById(long id, java.util.Optional<String> username) {
        if (username.isPresent()) {
            return findExpandedById(id, username.get());
        }
        var ticket = requireTicket(id);
        projectAccessService.requireRead(ticket.getProject().getId(), username);
        return toExpandedResponse(ticket, loadHistory(id), null);
    }

    @Transactional
    public TicketExpandedResponse findExpandedByIdentifier(String identifier, String username) {
        var ticket = requireTicketByIdentifierForView(identifier, username);
        projectAccessService.requireRead(ticket.getProject().getId(), username);
        var user = requireUserByUsername(username);
        return toExpandedResponse(ticket, loadHistory(ticket.getId()), user);
    }

    @Transactional
    public TicketExpandedResponse findExpandedByIdentifier(String identifier, java.util.Optional<String> username) {
        if (username.isPresent()) {
            return findExpandedByIdentifier(identifier, username.get());
        }
        var ticket = requireTicketByIdentifier(identifier);
        projectAccessService.requireRead(ticket.getProject().getId(), username);
        return toExpandedResponse(ticket, loadHistory(ticket.getId()), null);
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request, String authorUsername) {
        projectAccessService.requireView(request.projectId(), authorUsername);
        var project = projectRepository.findById(request.projectId())
                                       .orElseThrow(() -> projectNotFound(request.projectId()));
        var author = requireUserByUsername(authorUsername);
        var projectTickets = repository.countProjectTickets(request.projectId());
        var workflowId = project.getWorkflow().getId();
        var startStatus = project.getWorkflow().getStart();
        var customValues = customFieldService.resolveCreateValues(project.getId(), workflowId, request.customFields());
        customFieldService.validateRequired(project.getId(), workflowId, customValues);
        customFieldService.validateStatusRequiredForCreate(project.getId(), workflowId, startStatus.getId(), customValues);
        var ticket = new Ticket("%s-%03d".formatted(project.getPrefix(), projectTickets + 1),
                                request.title(),
                                htmlSanitizer.sanitize(request.description()),
                                categoryRepository.findById(request.categoryId())
                                                  .orElseThrow(() -> categoryNotFound(request.categoryId())),
                                author,
                                null,
                                project,
                                startStatus);
        ticket.setPriority(Objects.nonNull(request.priority()) ? request.priority() : TicketPriority.MEDIUM);
        ticket.setTicketType(Objects.nonNull(request.ticketType()) ? request.ticketType() : TicketType.TASK);
        ticket.setDueDate(request.dueDate());
        ticket.setStoryPoints(request.storyPoints());
        ticket.setPhase(phaseService.requireAssignablePhase(project.getId(), request.phaseId()));
        ticket.setBacklogRank(backlogService.nextRank(project.getId()));
        repository.save(ticket);
        historyService.logTicketCreated(ticket, author);
        var changes = customFieldService.applyValuesToTicket(ticket.getId(),
                                                             project.getId(),
                                                             workflowId,
                                                             customValues,
                                                             false);
        for (var change : changes) {
            historyService.logFieldChanged(ticket, author, change.key(), change.oldValue(), change.newValue());
        }
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse update(long id, UpdateTicketRequest request, String username) {
        var entity = requireTicket(id);
        var user = requireUserByUsername(username);
        var workflowId = entity.getProject().getWorkflow().getId();

        if (!entity.getTitle().equals(request.title())) {
            historyService.logFieldChanged(entity, user, "title", entity.getTitle(), request.title());
        }
        var sanitizedDescription = htmlSanitizer.sanitize(request.description());
        if (!entity.getDescription().equals(sanitizedDescription)) {
            historyService.logFieldChanged(entity, user, "description", entity.getDescription(), sanitizedDescription);
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
        if (Objects.nonNull(request.ticketType()) && !entity.getTicketType().equals(request.ticketType())) {
            historyService.logFieldChanged(entity,
                                           user,
                                           "ticketType",
                                           entity.getTicketType().name(),
                                           request.ticketType().name());
        }
        if (!Objects.equals(entity.getDueDate(), request.dueDate())) {
            historyService.logFieldChanged(entity,
                                           user,
                                           "dueDate",
                                           formatDueDate(entity.getDueDate()),
                                           formatDueDate(request.dueDate()));
        }
        if (!Objects.equals(entity.getStoryPoints(), request.storyPoints())) {
            historyService.logFieldChanged(entity,
                                           user,
                                           "storyPoints",
                                           formatStoryPoints(entity.getStoryPoints()),
                                           formatStoryPoints(request.storyPoints()));
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
        entity.setDescription(sanitizedDescription);
        entity.setCategory(newCategory);
        entity.setPriority(request.priority());
        if (Objects.nonNull(request.ticketType())) {
            entity.setTicketType(request.ticketType());
        }
        entity.setDueDate(request.dueDate());
        entity.setStoryPoints(request.storyPoints());
        entity.setUpdatedAt(LocalDateTime.now());

        if (request.customFields() != null) {
            customFieldService.validateRequiredForUpdate(entity.getId(),
                                                         entity.getProject().getId(),
                                                         workflowId,
                                                         request.customFields());
            var changes = customFieldService.applyValuesToTicket(entity.getId(),
                                                                 entity.getProject().getId(),
                                                                 workflowId,
                                                                 request.customFields(),
                                                                 false);
            for (var change : changes) {
                historyService.logFieldChanged(entity, user, change.key(), change.oldValue(), change.newValue());
            }
        }

        return toResponse(entity);
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
        return toResponse(entity);
    }

    @Transactional
    public void delete(long id, String username) {
        var ticket = requireTicket(id);
        var user = requireUserByUsername(username);
        historyService.logTicketDeleted(ticket, user);
        repository.delete(id);
    }

    @Transactional
    public TicketResponse restore(long id, String username) {
        var ticket = repository.findByIdIncludingDeleted(id)
                               .orElseThrow(() -> ticketNotFound(id));
        if (!ticket.isDeleted()) {
            throw new BadRequestException("Ticket is not deleted");
        }
        var user = requireUserByUsername(username);
        repository.restore(id);
        ticket.setDeleted(false);
        historyService.logTicketRestored(ticket, user);
        return toResponse(ticket);
    }

    public List<CommentResponse> listComments(long id) {
        return listComments(id, java.util.Optional.empty());
    }

    public List<CommentResponse> listComments(long id, java.util.Optional<String> username) {
        var ticket = requireTicket(id);
        projectAccessService.requireRead(ticket.getProject().getId(), username);
        return repository.findCommentsByTicketId(id)
                         .map(CommentResponse::load)
                         .toList();
    }

    @Transactional
    public CommentResponse addComment(long id, CommentRequest request, String username) {
        var ticket = requireTicket(id);
        var user = requireUserByUsername(username);
        var content = htmlSanitizer.sanitize(request.content());
        var comment = new Comment(ticket, user, content);
        comment.setViaAgent(Boolean.TRUE.equals(securityIdentity.getAttribute(ApiTokenIdentityProvider.VIA_AGENT_ATTRIBUTE)));
        var savedComment = repository.saveComment(comment);
        notifyMentionedMembers(ticket, user, request.content());
        return CommentResponse.load(savedComment);
    }

    private void notifyMentionedMembers(Ticket ticket, User author, String rawContent) {
        var mentionedUsernames = MentionParser.extractUsernames(rawContent);
        if (mentionedUsernames.isEmpty()) {
            return;
        }
        var mentionedMembers = memberRepository.findMembersByProjectId(ticket.getProject().getId())
                                               .stream()
                                               .filter(member -> mentionedUsernames.contains(member.getUsername()))
                                               .collect(Collectors.toSet());
        if (!mentionedMembers.isEmpty()) {
            notificationService.notifyMentions(ticket,
                                               author,
                                               mentionedMembers,
                                               "%s mencionou você em um comentário no ticket %s".formatted(author.getName(),
                                                                                                           ticket.getIdentifier()));
        }
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

        if (!Objects.equals(ticket.getStatus().getId(), to.getId())) {
            var wipLimit = ticket.getProject()
                                 .getWorkflow()
                                 .getWipLimits()
                                 .stream()
                                 .filter(wip -> Objects.equals(wip.getStatus().getId(), to.getId()))
                                 .findFirst();
            if (wipLimit.isPresent()) {
                var count = repository.countByProjectIdAndStatusId(ticket.getProject().getId(), to.getId());
                if (count >= wipLimit.get().getWipLimit()) {
                    throw new BadRequestException("WIP limit reached for status %s (limit %d)".formatted(to.getName(),
                                                                                                         wipLimit.get().getWipLimit()));
                }
            }
        }

        var fromStatus = ticket.getStatus().getName();
        var toStatus = to.getName();
        var workflowId = ticket.getProject().getWorkflow().getId();
        customFieldService.validateStatusRequired(ticket.getId(),
                                                  ticket.getProject().getId(),
                                                  workflowId,
                                                  to.getId());
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
        return toResponse(ticket);
    }

    public List<TicketHistoryResponse> getHistory(long id) {
        return getHistory(id, java.util.Optional.empty());
    }

    public List<TicketHistoryResponse> getHistory(long id, java.util.Optional<String> username) {
        var ticket = requireTicket(id);
        projectAccessService.requireRead(ticket.getProject().getId(), username);
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
            return toExpandedResponse(ticket, loadHistory(id), actor);
        }
        ticket.getSubscribers()
              .add(subscriber);
        historyService.logSubscribed(ticket, actor, subscriber.getName());
        return toExpandedResponse(repository.save(ticket), loadHistory(id), actor);
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
        return toExpandedResponse(repository.save(ticket), loadHistory(id), actor);
    }

    public List<Ticket> findTicketsByProjectId(long projectId) {
        return repository.findByProjectId(projectId).toList();
    }

    /**
     * Workflow restructure: move all tickets (including soft-deleted) on this
     * workflow from {@code from} to {@code to} without transition or WIP checks and
     * without notifications.
     */
    @Transactional
    public void remapWorkflowStatus(long workflowId, WorkflowStatus from, WorkflowStatus to, String username) {
        if (Objects.equals(from.getId(), to.getId())) {
            return;
        }
        var user = requireUserByUsername(username);
        var fromFinishOutcome = workflowRepository.findFinishOutcome(workflowId, from.getId());
        var toFinishOutcome = workflowRepository.findFinishOutcome(workflowId, to.getId());
        var tickets = repository.findByWorkflowIdAndStatusIdIncludingDeleted(workflowId, from.getId())
                                .toList();
        for (var ticket : tickets) {
            var fromStatus = ticket.getStatus().getName();
            ticket.setStatus(to);
            applyFinishDate(ticket, fromFinishOutcome, toFinishOutcome, user);
            historyService.logStatusChanged(ticket, user, fromStatus, to.getName());
        }
    }

    public long countTicketsOnWorkflowStatusIncludingDeleted(long workflowId, long statusId) {
        return repository.countByWorkflowIdAndStatusIdIncludingDeleted(workflowId, statusId);
    }

    private TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.load(ticket, loadCustomFields(ticket));
    }

    private TicketExpandedResponse toExpandedResponse(Ticket ticket, List<TicketHistory> history, User user) {
        var linkService = ticketLinkService.get();
        return TicketExpandedResponse.load(ticket,
                                           history,
                                           loadCustomFields(ticket),
                                           linkService.listLinksForExpand(ticket, user),
                                           linkService.childrenSummary(ticket.getId()),
                                           gitCommitService.listForTicket(ticket.getId()));
    }

    private List<CustomFieldValueResponse> loadCustomFields(Ticket ticket) {
        return customFieldService.readValues(ticket.getId(),
                                             ticket.getProject().getId(),
                                             ticket.getProject().getWorkflow().getId());
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

    private Ticket requireTicketForView(long id, String username) {
        var user = requireUserByUsername(username);
        if (canViewDeletedTicket(user)) {
            return repository.findByIdIncludingDeleted(id)
                             .orElseThrow(() -> ticketNotFound(id));
        }
        return requireTicket(id);
    }

    private Ticket requireTicketByIdentifierForView(String identifier, String username) {
        var user = requireUserByUsername(username);
        if (canViewDeletedTicket(user)) {
            return repository.findByIdentifierIncludingDeleted(identifier)
                             .orElseThrow(() -> ticketNotFound(identifier));
        }
        return requireTicketByIdentifier(identifier);
    }

    private boolean canViewDeletedTicket(User user) {
        return user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.PROJECT_MANAGER);
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
        var to = toOutcome.orElse(null);
        var from = fromOutcome.orElse(null);

        if (to == FinishOutcome.DONE) {
            var previous = ticket.getFinishedAt();
            ticket.setFinishedAt(LocalDateTime.now());
            historyService.logFieldChanged(ticket,
                                           user,
                                           "finishedAt",
                                           formatDateTime(previous),
                                           formatDateTime(ticket.getFinishedAt()));
            clearCanceledAt(ticket, user);
        } else if (from == FinishOutcome.DONE && to != FinishOutcome.CANCELED) {
            var previous = ticket.getFinishedAt();
            ticket.setFinishedAt(null);
            historyService.logFieldChanged(ticket, user, "finishedAt", formatDateTime(previous), null);
        }

        if (to == FinishOutcome.CANCELED) {
            var previous = ticket.getCanceledAt();
            ticket.setCanceledAt(LocalDateTime.now());
            historyService.logFieldChanged(ticket,
                                           user,
                                           "canceledAt",
                                           formatDateTime(previous),
                                           formatDateTime(ticket.getCanceledAt()));
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

    private static String formatDueDate(LocalDate value) {
        return Objects.nonNull(value) ? value.toString() : null;
    }

    private static String formatStoryPoints(Integer value) {
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

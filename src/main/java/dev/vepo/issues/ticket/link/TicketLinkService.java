package dev.vepo.issues.ticket.link;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.TicketType;
import dev.vepo.issues.ticket.backlog.BacklogService;
import dev.vepo.issues.ticket.history.TicketHistoryService;
import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class TicketLinkService {

    private static final Logger logger = LoggerFactory.getLogger(TicketLinkService.class);

    private final TicketLinkRepository linkRepository;
    private final TicketRepository ticketRepository;
    private final ProjectAccessService projectAccessService;
    private final TicketHistoryService historyService;
    private final BacklogService backlogService;

    @Inject
    public TicketLinkService(TicketLinkRepository linkRepository,
                             TicketRepository ticketRepository,
                             ProjectAccessService projectAccessService,
                             TicketHistoryService historyService,
                             BacklogService backlogService) {
        this.linkRepository = linkRepository;
        this.ticketRepository = ticketRepository;
        this.projectAccessService = projectAccessService;
        this.historyService = historyService;
        this.backlogService = backlogService;
    }

    @Transactional
    public List<TicketLinkResponse> listLinks(long ticketId, String username) {
        var user = projectAccessService.requireUser(username);
        var ticket = requireActiveTicket(ticketId);
        requireView(user, ticket);
        return listLinksForTicket(ticket, user);
    }

    public List<TicketLinkResponse> listLinksForExpand(Ticket ticket, User user) {
        return listLinksForTicket(ticket, user);
    }

    public ChildrenSummaryResponse childrenSummary(long ticketId) {
        return new ChildrenSummaryResponse(linkRepository.countChildren(ticketId),
                                           linkRepository.countDoneChildren(ticketId));
    }

    @Transactional
    public TicketLinkResponse createLink(long sourceTicketId, CreateTicketLinkRequest request, String username) {
        var user = projectAccessService.requireUser(username);
        var source = requireActiveTicket(sourceTicketId);
        var target = requireActiveTicket(request.targetTicketId());
        requireView(user, source);
        requireView(user, target);
        validateNewLink(source, target, request.linkType());

        var link = linkRepository.save(new TicketLink(source, target, request.linkType(), user));
        historyService.logLinkAdded(source, user, request.linkType().name(), target.getIdentifier(), link.getId());
        historyService.logLinkAdded(target, user, request.linkType().name(), source.getIdentifier(), link.getId());
        logger.info("Ticket link created: {} {} {}", source.getIdentifier(), request.linkType(), target.getIdentifier());
        return TicketLinkResponse.load(link, sourceTicketId);
    }

    @Transactional
    public void deleteLink(long ticketId, long linkId, String username) {
        var user = projectAccessService.requireUser(username);
        var perspective = requireActiveTicket(ticketId);
        requireView(user, perspective);
        var link = linkRepository.findById(linkId)
                                 .orElseThrow(() -> new NotFoundException("Ticket link does not found! linkId=%d".formatted(linkId)));
        if (!link.getSource().getId().equals(ticketId) && !link.getTarget().getId().equals(ticketId)) {
            throw new BadRequestException("Link %d does not belong to ticket %d".formatted(linkId, ticketId));
        }
        requireView(user, link.getSource());
        requireView(user, link.getTarget());

        historyService.logLinkRemoved(link.getSource(),
                                      user,
                                      link.getLinkType().name(),
                                      link.getTarget().getIdentifier(),
                                      link.getId());
        historyService.logLinkRemoved(link.getTarget(),
                                      user,
                                      link.getLinkType().name(),
                                      link.getSource().getIdentifier(),
                                      link.getId());
        linkRepository.delete(link);
        logger.info("Ticket link removed: linkId={}", linkId);
    }

    @Transactional
    public TicketResponse createChild(long epicId, CreateChildTicketRequest request, String username) {
        var user = projectAccessService.requireUser(username);
        var epic = requireActiveTicket(epicId);
        requireView(user, epic);
        if (epic.getTicketType() != TicketType.EPIC) {
            throw new BadRequestException("Parent ticket must be an Epic");
        }
        if (linkRepository.findChildOf(epicId).isPresent()) {
            throw new BadRequestException("Epic cannot be a child of another ticket when creating children");
        }

        var project = epic.getProject();
        var projectTickets = ticketRepository.countProjectTickets(project.getId());
        var description = Objects.requireNonNullElse(request.description(), "");
        var child = new Ticket("%s-%03d".formatted(project.getPrefix(), projectTickets + 1),
                               request.title(),
                               description,
                               epic.getCategory(),
                               user,
                               null,
                               project,
                               project.getWorkflow().getStart());
        child.setTicketType(TicketType.TASK);
        child.setPriority(epic.getPriority());
        child.setBacklogRank(backlogService.nextRank(project.getId()));
        ticketRepository.save(child);
        historyService.logTicketCreated(child, user);

        var link = linkRepository.save(new TicketLink(child, epic, TicketLinkType.CHILD_OF, user));
        historyService.logLinkAdded(child, user, TicketLinkType.CHILD_OF.name(), epic.getIdentifier(), link.getId());
        historyService.logLinkAdded(epic, user, TicketLinkType.CHILD_OF.name(), child.getIdentifier(), link.getId());

        return TicketResponse.load(child);
    }

    private List<TicketLinkResponse> listLinksForTicket(Ticket ticket, User user) {
        var responses = new ArrayList<TicketLinkResponse>();
        linkRepository.findByTicketId(ticket.getId()).forEach(link -> {
            var outbound = link.getSource().getId().equals(ticket.getId());
            var other = outbound ? link.getTarget() : link.getSource();
            if (other.isDeleted()) {
                return;
            }
            if (!projectAccessService.canViewProject(user, other.getProject())) {
                return;
            }
            responses.add(TicketLinkResponse.fromOther(link, other, outbound));
        });
        return responses;
    }

    private void validateNewLink(Ticket source, Ticket target, TicketLinkType linkType) {
        if (source.getId().equals(target.getId())) {
            throw new BadRequestException("Cannot link a ticket to itself");
        }
        if (linkType == TicketLinkType.RELATES_TO) {
            if (linkRepository.existsEitherDirection(source.getId(), target.getId(), TicketLinkType.RELATES_TO)) {
                throw new BadRequestException("RELATES_TO link already exists between these tickets");
            }
        } else if (linkRepository.exists(source.getId(), target.getId(), linkType)) {
            throw new BadRequestException("Link already exists");
        }

        if (linkType == TicketLinkType.CHILD_OF) {
            validateChildOf(source, target);
        }
    }

    private void validateChildOf(Ticket child, Ticket parent) {
        if (parent.getTicketType() != TicketType.EPIC) {
            throw new BadRequestException("CHILD_OF parent must be an Epic");
        }
        if (linkRepository.findChildOf(child.getId()).isPresent()) {
            throw new BadRequestException("Ticket already has a parent");
        }
        if (linkRepository.isParent(child.getId())) {
            throw new BadRequestException("A parent ticket cannot become a child");
        }
        if (linkRepository.findChildOf(parent.getId()).isPresent()) {
            throw new BadRequestException("Parent Epic cannot itself be a child");
        }
        if (wouldCreateCycle(child.getId(), parent.getId())) {
            throw new BadRequestException("CHILD_OF link would create a cycle");
        }
    }

    private boolean wouldCreateCycle(long childId, long parentId) {
        var currentParentId = parentId;
        while (true) {
            if (currentParentId == childId) {
                return true;
            }
            var parentLink = linkRepository.findChildOf(currentParentId);
            if (parentLink.isEmpty()) {
                return false;
            }
            currentParentId = parentLink.get().getTarget().getId();
        }
    }

    private Ticket requireActiveTicket(long ticketId) {
        return ticketRepository.findById(ticketId)
                               .orElseThrow(() -> new NotFoundException("Ticket does not found! ticketId=%d".formatted(ticketId)));
    }

    private void requireView(User user, Ticket ticket) {
        if (!projectAccessService.canViewProject(user, ticket.getProject())) {
            throw new ForbiddenException("Access denied to project %d".formatted(ticket.getProject().getId()));
        }
    }
}

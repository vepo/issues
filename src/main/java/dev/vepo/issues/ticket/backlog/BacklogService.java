package dev.vepo.issues.ticket.backlog;

import java.util.ArrayList;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.history.TicketHistoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class BacklogService {

    private static final Logger logger = LoggerFactory.getLogger(BacklogService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TicketRepository ticketRepository;
    private final ProjectAccessService projectAccessService;
    private final TicketHistoryService historyService;

    @Inject
    public BacklogService(TicketRepository ticketRepository,
                          ProjectAccessService projectAccessService,
                          TicketHistoryService historyService) {
        this.ticketRepository = ticketRepository;
        this.projectAccessService = projectAccessService;
        this.historyService = historyService;
    }

    @Transactional
    public BacklogPageResponse list(long projectId, int page, int size, String username) {
        projectAccessService.requireView(projectId, username);
        var safePage = Math.max(page, 0);
        var safeSize = size < 1 || size > MAX_PAGE_SIZE ? DEFAULT_PAGE_SIZE : size;
        var total = ticketRepository.countBacklog(projectId);
        var items = ticketRepository.findBacklogPage(projectId, safePage, safeSize)
                                    .stream()
                                    .map(BacklogTicketResponse::load)
                                    .toList();
        var hasMore = (long) (safePage + 1) * safeSize < total;
        return new BacklogPageResponse(items, total, safePage, safeSize, hasMore);
    }

    public int nextRank(long projectId) {
        return ticketRepository.maxBacklogRank(projectId) + 1;
    }

    @Transactional
    public BacklogTicketResponse reorder(long projectId, ReorderBacklogRequest request, String username) {
        var user = projectAccessService.requireUser(username);
        projectAccessService.requireView(projectId, username);

        var ticket = ticketRepository.findBacklogEligibleById(projectId, request.ticketId())
                                     .orElseThrow(() -> new NotFoundException("Ticket %d is not in project backlog %d".formatted(request.ticketId(),
                                                                                                                                 projectId)));

        if (request.beforeTicketId() != null && Objects.equals(request.beforeTicketId(), request.ticketId())) {
            throw new BadRequestException("beforeTicketId cannot equal ticketId");
        }

        if (request.beforeTicketId() != null) {
            ticketRepository.findBacklogEligibleById(projectId, request.beforeTicketId())
                            .orElseThrow(() -> new BadRequestException("beforeTicketId %d is not in project backlog %d".formatted(request.beforeTicketId(),
                                                                                                                                  projectId)));
        }

        var ordered = new ArrayList<>(ticketRepository.findBacklogEligibleOrdered(projectId));
        ordered.removeIf(t -> Objects.equals(t.getId(), ticket.getId()));

        if (request.beforeTicketId() == null) {
            ordered.add(ticket);
        } else {
            var insertAt = -1;
            for (var i = 0; i < ordered.size(); i++) {
                if (Objects.equals(ordered.get(i).getId(), request.beforeTicketId())) {
                    insertAt = i;
                    break;
                }
            }
            if (insertAt < 0) {
                throw new BadRequestException("beforeTicketId %d is not in project backlog %d".formatted(request.beforeTicketId(),
                                                                                                         projectId));
            }
            ordered.add(insertAt, ticket);
        }

        var oldRank = ticket.getBacklogRank();
        reassignDenseRanks(ordered);
        var newRank = ticket.getBacklogRank();

        if (oldRank != newRank) {
            historyService.logFieldChanged(ticket,
                                           user,
                                           "backlogRank",
                                           String.valueOf(oldRank),
                                           String.valueOf(newRank));
        }

        logger.info("Reordered ticket {} in project {} backlog to rank {}", ticket.getIdentifier(), projectId, newRank);
        return BacklogTicketResponse.load(ticket);
    }

    private void reassignDenseRanks(java.util.List<Ticket> ordered) {
        for (var i = 0; i < ordered.size(); i++) {
            ordered.get(i).setBacklogRank(i + 1);
        }
    }
}

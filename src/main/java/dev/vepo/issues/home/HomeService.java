package dev.vepo.issues.home;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.comments.Comment;
import dev.vepo.issues.ticket.history.TicketHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class HomeService {

    private final ProjectAccessService accessService;
    private final TicketRepository ticketRepository;

    @Inject
    public HomeService(ProjectAccessService accessService, TicketRepository ticketRepository) {
        this.accessService = accessService;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public List<HomeTicketResponse> listCurrentTickets(String username) {
        var scope = scopeIds(username);
        return ticketRepository.findOpenTicketsInProjects(scope)
                               .map(HomeTicketResponse::load)
                               .toList();
    }

    @Transactional
    public List<HomeTicketResponse> listAssignedTickets(String username) {
        var user = accessService.requireUser(username);
        var scope = scopeIds(username);
        return ticketRepository.findOpenAssignedTicketsInProjects(user.getId(), scope)
                               .map(HomeTicketResponse::load)
                               .toList();
    }

    @Transactional
    public List<HomeActivityResponse> listActivity(String username) {
        var scope = scopeIds(username);
        var items = new ArrayList<HomeActivityResponse>();
        ticketRepository.findRecentCommentsInProjects(scope)
                        .forEach(comment -> items.add(HomeActivityResponse.fromComment(comment)));
        ticketRepository.findRecentStatusChangesInProjects(scope)
                        .forEach(history -> items.add(HomeActivityResponse.fromStatusChange(history)));
        items.sort(Comparator.comparing(HomeActivityResponse::occurredAt).reversed());
        return items;
    }

    private List<Long> scopeIds(String username) {
        var user = accessService.requireUser(username);
        return new ArrayList<>(accessService.projectScopeIds(user));
    }
}

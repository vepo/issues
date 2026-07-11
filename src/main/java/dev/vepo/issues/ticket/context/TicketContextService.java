package dev.vepo.issues.ticket.context;

import java.util.Objects;

import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.TicketService;
import dev.vepo.issues.workflow.WorkflowTransition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class TicketContextService {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final CustomFieldService customFieldService;

    @Inject
    public TicketContextService(TicketService ticketService,
                                TicketRepository ticketRepository,
                                CustomFieldService customFieldService) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.customFieldService = customFieldService;
    }

    @Transactional
    public TicketContextResponse getContext(long ticketId, String username) {
        var expanded = ticketService.findExpandedById(ticketId, username);
        var ticket = ticketRepository.findById(ticketId)
                                     .or(() -> ticketRepository.findByIdIncludingDeleted(ticketId))
                                     .orElseThrow(() -> new NotFoundException("Ticket not found! id=%d".formatted(ticketId)));
        var workflow = ticket.getProject().getWorkflow();
        var currentStatusId = ticket.getStatus().getId();
        var availableTransitions = workflow.getTransitions()
                                           .stream()
                                           .filter(t -> Objects.equals(t.getFrom().getId(), currentStatusId))
                                           .map(WorkflowTransition::getTo)
                                           .map(status -> new TicketAvailableTransitionResponse(status.getId(), status.getName()))
                                           .toList();
        var customFields = customFieldService.listInScope(ticket.getProject().getId(), workflow.getId());
        return new TicketContextResponse(expanded, availableTransitions, customFields);
    }
}

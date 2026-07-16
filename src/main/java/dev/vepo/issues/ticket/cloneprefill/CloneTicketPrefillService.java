package dev.vepo.issues.ticket.cloneprefill;

import java.util.ArrayList;

import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.CloneTicketPrefillResponse;
import dev.vepo.issues.ticket.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class CloneTicketPrefillService {

    private final TicketRepository ticketRepository;
    private final ProjectAccessService projectAccessService;
    private final CustomFieldService customFieldService;

    @Inject
    public CloneTicketPrefillService(TicketRepository ticketRepository,
                                     ProjectAccessService projectAccessService,
                                     CustomFieldService customFieldService) {
        this.ticketRepository = ticketRepository;
        this.projectAccessService = projectAccessService;
        this.customFieldService = customFieldService;
    }

    @Transactional
    public CloneTicketPrefillResponse getPrefill(long sourceTicketId, long targetProjectId, String username) {
        var sourceTicket = ticketRepository.findById(sourceTicketId)
                                           .orElseThrow(() -> new NotFoundException(
                                                                                    "Ticket with ID %d does not exist".formatted(sourceTicketId)));
        var sourceProject = sourceTicket.getProject();
        projectAccessService.requireRead(sourceProject.getId(), username);
        projectAccessService.requireWrite(targetProjectId, username);
        var targetProject = projectAccessService.requireProject(targetProjectId);
        var warnings = new ArrayList<String>();
        var customFields = customFieldService.copyCompatibleValues(sourceTicket.getId(),
                                                                   sourceProject.getId(),
                                                                   sourceProject.getWorkflow().getId(),
                                                                   targetProjectId,
                                                                   targetProject.getWorkflow().getId(),
                                                                   warnings);
        return new CloneTicketPrefillResponse(sourceTicket.getIdentifier(),
                                              targetProjectId,
                                              sourceTicket.getTitle(),
                                              sourceTicket.getDescription(),
                                              sourceTicket.getCategory().getId(),
                                              sourceTicket.getPriority().name(),
                                              sourceTicket.getTicketType().name(),
                                              customFields,
                                              warnings);
    }
}

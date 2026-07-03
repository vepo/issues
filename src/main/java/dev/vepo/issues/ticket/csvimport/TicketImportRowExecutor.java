package dev.vepo.issues.ticket.csvimport;

import java.util.Objects;

import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.project.ProjectRepository;
import dev.vepo.issues.ticket.CreateTicketRequest;
import dev.vepo.issues.ticket.MoveTicketRequest;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.TicketService;
import dev.vepo.issues.ticket.UpdateAssigneeRequest;
import dev.vepo.issues.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class TicketImportRowExecutor {

    private final TicketService ticketService;
    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Inject
    public TicketImportRowExecutor(TicketService ticketService,
                                   ProjectRepository projectRepository,
                                   CategoryRepository categoryRepository,
                                   UserRepository userRepository) {
        this.ticketService = ticketService;
        this.projectRepository = projectRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public TicketResponse importRow(long projectId, MappedImportRow row, String username) {
        var project = projectRepository.findById(projectId)
                                       .orElseThrow(() -> new NotFoundException("Project does not found! projectId=%d".formatted(projectId)));
        var category = categoryRepository.findByName(row.categoryName())
                                         .orElseThrow(() -> new NotFoundException("Category does not found! name=%s".formatted(row.categoryName())));

        var created = ticketService.create(new CreateTicketRequest(row.title(),
                                                                   row.description(),
                                                                   category.getId(),
                                                                   projectId,
                                                                   row.priority(),
                                                                   null,
                                                                   null),
                                           username);

        if (row.assigneeEmail() != null) {
            var assignee = userRepository.findByEmail(row.assigneeEmail()).orElseThrow();
            created = ticketService.updateAssignee(created.id(), new UpdateAssigneeRequest(assignee.getId()), username);
        }

        if (row.statusName() != null) {
            var targetStatus = project.getWorkflow()
                                      .getStatuses()
                                      .stream()
                                      .filter(s -> s.getName().equalsIgnoreCase(row.statusName().trim()))
                                      .findFirst()
                                      .orElseThrow();
            var start = project.getWorkflow().getStart();
            if (!Objects.equals(start.getId(), targetStatus.getId())) {
                created = ticketService.moveTicket(created.id(), new MoveTicketRequest(targetStatus.getId()), username);
            }
        }

        return created;
    }
}

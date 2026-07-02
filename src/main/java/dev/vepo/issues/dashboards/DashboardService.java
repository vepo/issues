package dev.vepo.issues.dashboards;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.vepo.issues.project.ProjectRepository;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketService;
import dev.vepo.issues.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class DashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final TicketService ticketService;
    private final ProjectRepository projectRepository;

    @Inject
    public DashboardService(TicketService ticketService, ProjectRepository projectRepository) {
        this.ticketService = ticketService;
        this.projectRepository = projectRepository;
    }

    public PieChartDataResponse loadPieData(long projectId, DashboardType type) {
        return switch (type) {
            case TICKETS_BY_PRIORITY -> loadTicketsByCategory(projectId);
            case TICKETS_BY_DAY -> loadTicketByDay(projectId);
            case TICKETS_BY_STATUS -> loadTicketByStatus(projectId);
            default -> throw new BadRequestException("Invalid chart type!!");
        };
    }

    public TableDataResponse loadTableData(long projectId, DashboardType type) {
        return switch (type) {
            case RECENT_TICKETS -> recentTickets(projectId);
            default -> throw new BadRequestException("Invalid chart type!!");
        };
    }

    public KpiDataResponse loadKpiData(long projectId, DashboardType type) {
        return switch (type) {
            case PERFORMANCE_KPI -> performanceKpi(projectId);
            default -> throw new BadRequestException("Invalid chart type!!");
        };
    }

    private KpiDataResponse performanceKpi(long projectId) {
        var projectTickets = ticketService.findTicketsByProjectId(projectId);
        if (projectTickets.isEmpty()) {
            return new KpiDataResponse(0, Collections.emptyMap());
        }
        return new KpiDataResponse(projectTickets.size(),
                                   projectTickets.stream()
                                                 .collect(Collectors.groupingBy(ticket -> ticket.getStatus().getName(),
                                                                                Collectors.summingInt(i -> 1))));
    }

    private TableDataResponse recentTickets(long projectId) {
        return new TableDataResponse(new String[] { "Identificador", "Título", "Última atualização" },
                                     ticketService.findTicketsByProjectId(projectId)
                                                  .stream()
                                                  .sorted(Comparator.comparing(Ticket::getUpdatedAt).reversed())
                                                  .map(ticket -> new TableRowData(new String[] { ticket.getIdentifier(), ticket.getTitle(), DATETIME_FORMATTER.format(ticket.getUpdatedAt()
                                                                                                                                                                            .atZone(ZoneId.systemDefault()))
                                                  }))
                                                  .toArray(TableRowData[]::new));
    }

    private PieChartDataResponse loadTicketByDay(long projectId) {
        return generatePieChart(projectId, ticket -> DATE_FORMATTER.format(ticket.getCreatedAt().atZone(ZoneId.systemDefault())));
    }

    private PieChartDataResponse loadTicketByStatus(long projectId) {
        return generatePieChart(projectId,
                                ((Function<Ticket, WorkflowStatus>) Ticket::getStatus).andThen(WorkflowStatus::getName));
    }

    private PieChartDataResponse loadTicketsByCategory(long projectId) {
        return generatePieChart(projectId, ticket -> ticket.getCategory().getName());
    }

    private PieChartDataResponse generatePieChart(long projectId, Function<Ticket, String> keyExtractor) {
        var projectTickets = ticketService.findTicketsByProjectId(projectId);
        if (projectTickets.isEmpty()) {
            var project = projectRepository.findById(projectId)
                                           .orElseThrow(() -> new NotFoundException("Project not found!"));
            return new PieChartDataResponse(new String[] {}, new Dataset[] { new Dataset(project.getName(), new Number[] {}, new String[] {}) });
        }
        var project = projectTickets.getFirst().getProject();
        var labels = projectTickets.stream()
                                   .map(keyExtractor)
                                   .distinct()
                                   .sorted()
                                   .toArray(String[]::new);
        Map<String, List<Ticket>> ticketsMap = projectTickets.stream()
                                                             .collect(Collectors.groupingBy(keyExtractor));
        return new PieChartDataResponse(labels,
                                        new Dataset[] { new Dataset(project.getName(),
                                                                    Stream.of(labels)
                                                                          .map(ticketsMap::get)
                                                                          .map(List::size)
                                                                          .toArray(Number[]::new),
                                                                    ColorGenerator.asArray(labels.length)) });
    }
}

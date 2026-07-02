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

import javax.xml.crypto.Data;

import dev.vepo.issues.project.Project;
import dev.vepo.issues.project.ProjectRepository;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.workflow.WorkflowStatus;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/projects/{projectId}/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
public class LoadDashboardDataEndpoint {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static String extractTicketDay(Ticket ticket) {
        return DATE_FORMATTER.format(ticket.getCreatedAt().atZone(ZoneId.systemDefault()));
    }

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;

    @Inject
    public LoadDashboardDataEndpoint(TicketRepository repository,
                                     ProjectRepository projectRepository) {
        this.ticketRepository = repository;
        this.projectRepository = projectRepository;
    }

    @GET
    @Path("pie/{dashboardType}")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    public PieChartDataResponse loadPieData(@PathParam("projectId") Long projectId,
                                            @PathParam("dashboardType") DashboardType type) {
        return switch (type) {
            case TICKETS_BY_PRIORITY -> loadTicketsByPriority(projectId);
            case TICKETS_BY_DAY -> loadTicketByDay(projectId);
            case TICKETS_BY_STATUS -> loadTicketByStatus(projectId);
            default -> throw new BadRequestException("Invalid chart type!!");
        };
    }

    @GET
    @Path("table/{dashboardType}")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    public TableDataResponse loadTableData(@PathParam("projectId") Long projectId,
                                           @PathParam("dashboardType") DashboardType type) {
        return switch (type) {
            case RECENT_TICKETS -> recentTickets(projectId);
            default -> throw new BadRequestException("Invalid chart type!!");
        };
    }

    @GET
    @Path("kpi/{dashboardType}")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    public KpiDataResponse loadKpiData(@PathParam("projectId") Long projectId,
                                       @PathParam("dashboardType") DashboardType type) {
        return switch (type) {
            case PERFORMANCE_KPI -> performanceKpi(projectId);
            default -> throw new BadRequestException("Invalid chart type!!");
        };
    }

    private KpiDataResponse performanceKpi(Long projectId) {
        var projectTickets = this.ticketRepository.findByProjectId(projectId)
                                                  .toList();
        if (projectTickets.isEmpty()) {
            return new KpiDataResponse(0, Collections.emptyMap());
        }

        return new KpiDataResponse(projectTickets.size(), projectTickets.stream()
                                                                        .collect(Collectors.groupingBy(ticket -> ticket.getStatus().getName(),
                                                                                                       Collectors.summingInt(i -> 1))));
    }

    private TableDataResponse recentTickets(Long projectId) {
        return new TableDataResponse(new String[] { "Identificador", "Título", "Última atualização" },
                                     this.ticketRepository.findByProjectId(projectId)
                                                          .sorted(Comparator.comparing(Ticket::getUpdatedAt).reversed())
                                                          .map(ticket -> new TableRowData(new String[] { //
                                                              ticket.getIdentifier(), //
                                                              ticket.getTitle(), //
                                                              DATETIME_FORMATTER.format(ticket.getUpdatedAt()
                                                                                              .atZone(ZoneId.systemDefault()))
                                                          }))
                                                          .toArray(TableRowData[]::new));
    }

    private PieChartDataResponse loadTicketByDay(Long projectId) {
        return generatePieChart(projectId, LoadDashboardDataEndpoint::extractTicketDay);
    }

    private PieChartDataResponse loadTicketByStatus(Long projectId) {
        return generatePieChart(projectId, ((Function<Ticket, WorkflowStatus>) Ticket::getStatus).andThen(WorkflowStatus::getName));
    }

    private PieChartDataResponse loadTicketsByPriority(long projectId) {
        return generatePieChart(projectId, ticket -> "Alta");
    }

    private PieChartDataResponse generatePieChart(Long projectId, Function<Ticket, String> keyExtractor) {
        var projectTickets = this.ticketRepository.findByProjectId(projectId)
                                                  .toList();
        if (projectTickets.isEmpty()) {
            var project = this.projectRepository.findById(projectId)
                                                .orElseThrow(() -> new NotFoundException("Project not found!"));
            return new PieChartDataResponse(new String[] {}, new Dataset[] { new Dataset(project.getName(), new Number[] {}, new String[] {}) });
        }
        var project = projectTickets.stream()
                                    .findFirst()
                                    .orElseThrow(() -> new NotFoundException("Project not found!"))
                                    .getProject();
        var labels = projectTickets.stream()
                                   .map(keyExtractor)
                                   .distinct()
                                   .sorted()
                                   .toArray(String[]::new);
        Map<String, List<Ticket>> ticketsMap = projectTickets.stream()
                                                             .collect(Collectors.groupingBy(keyExtractor));
        return new PieChartDataResponse(labels, new Dataset[] { new Dataset(project.getName(),
                                                                            Stream.of(labels)
                                                                                  .map(ticketsMap::get)
                                                                                  .map(List::size)
                                                                                  .toArray(Number[]::new),
                                                                            ColorGenerator.asArray(labels.length)) });
    }
}

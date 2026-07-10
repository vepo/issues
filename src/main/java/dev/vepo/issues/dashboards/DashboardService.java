package dev.vepo.issues.dashboards;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.issues.project.Project;
import dev.vepo.issues.project.ProjectRepository;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class DashboardService {

    static final List<String> DEFAULT_WIDGET_IDS = List.of("tickets-by-status",
                                                           "tickets-by-priority",
                                                           "performance-kpi",
                                                           "recent-tickets");

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DashboardLayoutRepository layoutRepository;
    private final DashboardRepository dashboardRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public DashboardService(ProjectRepository projectRepository,
                            UserRepository userRepository,
                            DashboardLayoutRepository layoutRepository,
                            DashboardRepository dashboardRepository,
                            ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.layoutRepository = layoutRepository;
        this.dashboardRepository = dashboardRepository;
        this.objectMapper = objectMapper;
    }

    public DashboardLayoutResponse getLayout(long projectId, String username) {
        requireProject(projectId);
        var user = requireUser(username);
        return layoutRepository.findByUserIdAndProjectId(user.getId(), projectId)
                               .map(layout -> new DashboardLayoutResponse(readWidgetIds(layout.getWidgets())))
                               .orElseGet(() -> new DashboardLayoutResponse(DEFAULT_WIDGET_IDS));
    }

    @Transactional
    public DashboardLayoutResponse saveLayout(long projectId, String username, SaveDashboardLayoutRequest request) {
        var project = requireProject(projectId);
        var user = requireUser(username);
        validateWidgetIds(request.widgetIds());
        var widgetsJson = writeWidgetIds(request.widgetIds());
        var layout = layoutRepository.findByUserIdAndProjectId(user.getId(), projectId)
                                     .orElseGet(() -> new DashboardLayout(user, project, widgetsJson));
        layout.setWidgets(widgetsJson);
        layout.setUpdatedAt(Instant.now());
        layoutRepository.save(layout);
        return new DashboardLayoutResponse(List.copyOf(request.widgetIds()));
    }

    public PieChartDataResponse loadPieData(long projectId, DashboardType type) {
        requireProject(projectId);
        return switch (type) {
            case TICKETS_BY_PRIORITY -> pieFromTuples(projectId, dashboardRepository.countTicketsByPriority(projectId));
            case TICKETS_BY_DAY -> pieFromDayCounts(projectId, dashboardRepository.countTicketsByCreatedDay(projectId));
            case TICKETS_BY_STATUS -> pieFromTuples(projectId, dashboardRepository.countTicketsByStatus(projectId));
            default -> throw new BadRequestException("Invalid chart type!!");
        };
    }

    public TableDataResponse loadTableData(long projectId, DashboardType type) {
        requireProject(projectId);
        return switch (type) {
            case RECENT_TICKETS -> recentTickets(projectId);
            default -> throw new BadRequestException("Invalid chart type!!");
        };
    }

    public KpiDataResponse loadKpiData(long projectId, DashboardType type) {
        requireProject(projectId);
        return switch (type) {
            case PERFORMANCE_KPI -> performanceKpi(projectId);
            default -> throw new BadRequestException("Invalid chart type!!");
        };
    }

    private KpiDataResponse performanceKpi(long projectId) {
        var total = dashboardRepository.countTickets(projectId);
        if (total == 0) {
            return new KpiDataResponse(0, Collections.emptyMap());
        }
        Map<String, Integer> perStatus = new LinkedHashMap<>();
        for (Tuple row : dashboardRepository.countTicketsByStatus(projectId)) {
            perStatus.put(labelValue(row.get("label")), ((Number) row.get("total")).intValue());
        }
        return new KpiDataResponse((int) total, perStatus);
    }

    private TableDataResponse recentTickets(long projectId) {
        return new TableDataResponse(new String[] { "Identificador", "Título", "Última atualização" },
                                     dashboardRepository.findRecentTickets(projectId)
                                                        .stream()
                                                        .map(ticket -> new TableRowData(new String[] { ticket.getIdentifier(), ticket.getTitle(), DATETIME_FORMATTER.format(ticket.getUpdatedAt()
                                                                                                                                                                                  .atZone(ZoneId.systemDefault()))
                                                        }))
                                                        .toArray(TableRowData[]::new));
    }

    private PieChartDataResponse pieFromTuples(long projectId, List<Tuple> rows) {
        var projectName = requireProject(projectId).getName();
        if (rows.isEmpty()) {
            return emptyPie(projectName);
        }
        var labels = rows.stream()
                         .map(row -> labelValue(row.get("label")))
                         .toArray(String[]::new);
        var values = rows.stream()
                         .map(row -> ((Number) row.get("total")).intValue())
                         .toArray(Number[]::new);
        return new PieChartDataResponse(labels,
                                        new Dataset[] { new Dataset(projectName, values, ColorGenerator.asArray(labels.length)) });
    }

    private String labelValue(Object label) {
        if (label instanceof Enum<?> enumLabel) {
            return enumLabel.name();
        }
        return String.valueOf(label);
    }

    private PieChartDataResponse pieFromDayCounts(long projectId, List<Object[]> rows) {
        var projectName = requireProject(projectId).getName();
        if (rows.isEmpty()) {
            return emptyPie(projectName);
        }
        var labels = rows.stream()
                         .map(row -> (String) row[0])
                         .toArray(String[]::new);
        var values = rows.stream()
                         .map(row -> ((Number) row[1]).intValue())
                         .toArray(Number[]::new);
        return new PieChartDataResponse(labels,
                                        new Dataset[] { new Dataset(projectName, values, ColorGenerator.asArray(labels.length)) });
    }

    private PieChartDataResponse emptyPie(String projectName) {
        return new PieChartDataResponse(new String[] {}, new Dataset[] { new Dataset(projectName, new Number[] {}, new String[] {}) });
    }

    private void validateWidgetIds(List<String> widgetIds) {
        if (widgetIds == null) {
            throw new BadRequestException("Widget ids must be provided");
        }
        for (var widgetId : widgetIds) {
            DashboardType.fromString(widgetId);
        }
    }

    private List<String> readWidgetIds(String widgetsJson) {
        try {
            return objectMapper.readValue(widgetsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid dashboard layout widgets JSON", e);
        }
    }

    private String writeWidgetIds(List<String> widgetIds) {
        try {
            return objectMapper.writeValueAsString(widgetIds);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize dashboard layout widgets", e);
        }
    }

    private Project requireProject(long projectId) {
        return projectRepository.findById(projectId)
                                .orElseThrow(() -> new NotFoundException("Project not found!"));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                             .orElseThrow(() -> new NotFoundException("User not found! username=%s".formatted(username)));
    }
}

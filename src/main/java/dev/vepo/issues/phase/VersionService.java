package dev.vepo.issues.phase;

import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.project.ProjectRepository;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class VersionService {

    private static final String SECTION_TARGET = "Planejado";
    private static final String SECTION_OBSERVED = "Entregue";
    private static final String SECTION_PHASE = "Via fase";

    private final VersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;
    private final TicketRepository ticketRepository;

    @Inject
    public VersionService(VersionRepository versionRepository,
                          ProjectRepository projectRepository,
                          ProjectAccessService projectAccessService,
                          TicketRepository ticketRepository) {
        this.versionRepository = versionRepository;
        this.projectRepository = projectRepository;
        this.projectAccessService = projectAccessService;
        this.ticketRepository = ticketRepository;
    }

    public List<VersionResponse> listByProject(long projectId, String username) {
        projectAccessService.requireRead(projectId, Optional.ofNullable(username));
        requireProject(projectId);
        return versionRepository.findByProjectId(projectId)
                                .map(VersionResponse::load)
                                .toList();
    }

    public VersionResponse findById(long projectId, long versionId, String username) {
        projectAccessService.requireRead(projectId, Optional.ofNullable(username));
        return VersionResponse.load(requireVersion(projectId, versionId));
    }

    @Transactional
    public VersionResponse create(long projectId, CreateVersionRequest request, String username) {
        projectAccessService.requireManage(projectId, username);
        var project = requireProject(projectId);
        var label = request.label().trim();
        SemVerValidator.requireValid(label);
        if (versionRepository.existsByProjectIdAndLabel(projectId, label)) {
            throw new BadRequestException("Version label already exists for this project: %s".formatted(label));
        }
        return VersionResponse.load(versionRepository.save(new Version(project, label, request.description())));
    }

    @Transactional
    public VersionResponse update(long projectId, long versionId, UpdateVersionRequest request, String username) {
        projectAccessService.requireManage(projectId, username);
        var version = requireVersion(projectId, versionId);
        var label = request.label().trim();
        SemVerValidator.requireValid(label);
        if (versionRepository.existsByProjectIdAndLabelExcludingId(projectId, label, versionId)) {
            throw new BadRequestException("Version label already exists for this project: %s".formatted(label));
        }
        version.setLabel(label);
        version.setDescription(request.description());
        return VersionResponse.load(version);
    }

    public VersionChangelogResponse changelog(long projectId, long versionId, String username) {
        projectAccessService.requireRead(projectId, Optional.ofNullable(username));
        var version = requireVersion(projectId, versionId);
        var targetTickets = ticketRepository.findForVersionChangelog(projectId, versionId, ChangelogAssociation.TARGET)
                                            .map(ticket -> toEntry(ticket, EnumSet.of(ChangelogAssociation.TARGET)))
                                            .sorted(changelogComparator())
                                            .toList();
        var observedTickets = ticketRepository.findForVersionChangelog(projectId, versionId, ChangelogAssociation.OBSERVED)
                                              .map(ticket -> toEntry(ticket, EnumSet.of(ChangelogAssociation.OBSERVED)))
                                              .sorted(changelogComparator())
                                              .toList();
        var phaseTickets = ticketRepository.findForVersionChangelog(projectId, versionId, ChangelogAssociation.PHASE_DELIVERABLE)
                                           .map(ticket -> toEntry(ticket, EnumSet.of(ChangelogAssociation.PHASE_DELIVERABLE)))
                                           .sorted(changelogComparator())
                                           .toList();
        var sections = new ArrayList<VersionChangelogSection>();
        sections.add(new VersionChangelogSection(SECTION_TARGET, targetTickets));
        sections.add(new VersionChangelogSection(SECTION_OBSERVED, observedTickets));
        sections.add(new VersionChangelogSection(SECTION_PHASE, phaseTickets));
        return new VersionChangelogResponse(version.getId(), version.getLabel(), version.getDescription(), sections);
    }

    public Version requireVersion(long projectId, long versionId) {
        return versionRepository.findByIdAndProjectId(versionId, projectId)
                                .orElseThrow(() -> new NotFoundException("Version not found! projectId=%d versionId=%d".formatted(projectId, versionId)));
    }

    public Version requireVersionForTicket(long projectId, Long versionId) {
        if (Objects.isNull(versionId)) {
            return null;
        }
        return requireVersion(projectId, versionId);
    }

    private dev.vepo.issues.project.Project requireProject(long projectId) {
        return projectRepository.findById(projectId)
                                .orElseThrow(() -> new NotFoundException("Project not found! projectId=%d".formatted(projectId)));
    }

    private VersionChangelogEntry toEntry(Ticket ticket, Set<ChangelogAssociation> associations) {
        return new VersionChangelogEntry(ticket.getId(),
                                         ticket.getIdentifier(),
                                         ticket.getTitle(),
                                         ticket.getStatus().getName(),
                                         ticket.getPriority().name(),
                                         ticket.getFinishedAt(),
                                         associations);
    }

    private Comparator<VersionChangelogEntry> changelogComparator() {
        return Comparator.comparing(VersionChangelogEntry::finishedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                         .thenComparing(VersionChangelogEntry::identifier);
    }
}

package dev.vepo.issues.dashboards.burndown;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import dev.vepo.issues.phase.Phase;
import dev.vepo.issues.phase.PhaseService;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BurndownService {

    private final ProjectAccessService projectAccessService;
    private final PhaseService phaseService;
    private final TicketRepository ticketRepository;

    @Inject
    public BurndownService(ProjectAccessService projectAccessService,
                           PhaseService phaseService,
                           TicketRepository ticketRepository) {
        this.projectAccessService = projectAccessService;
        this.phaseService = phaseService;
        this.ticketRepository = ticketRepository;
    }

    public BurndownResponse load(long projectId, long phaseId, String username) {
        projectAccessService.requireView(projectId, username);
        var phase = phaseService.requirePhase(projectId, phaseId);
        var tickets = ticketRepository.findByPhaseId(phaseId).toList();
        var warnings = buildWarnings(tickets);
        var datesComplete = phase.getStartDate() != null && phase.getEndDate() != null;
        if (!datesComplete) {
            return new BurndownResponse(phase.getId(),
                                        phase.getName(),
                                        phase.getStartDate(),
                                        phase.getEndDate(),
                                        false,
                                        List.of(),
                                        warnings,
                                        0,
                                        remainingToday(tickets, LocalDate.now()));
        }
        return buildSeries(phase, tickets, warnings);
    }

    private BurndownResponse buildSeries(Phase phase, List<Ticket> tickets, List<BurndownWarning> warnings) {
        var start = phase.getStartDate();
        var end = phase.getEndDate();
        var today = LocalDate.now();
        var seriesEnd = today.isBefore(end) ? today : end;
        var commitment = remainingOn(tickets, start);
        var series = new ArrayList<BurndownSeriesPoint>();
        for (var day = start; !day.isAfter(seriesEnd); day = day.plusDays(1)) {
            series.add(new BurndownSeriesPoint(day, idealOn(commitment, start, end, day), remainingOn(tickets, day)));
        }
        return new BurndownResponse(phase.getId(),
                                    phase.getName(),
                                    start,
                                    end,
                                    true,
                                    List.copyOf(series),
                                    warnings,
                                    commitment,
                                    remainingOn(tickets, today.isAfter(end) ? end : today));
    }

    private static List<BurndownWarning> buildWarnings(List<Ticket> tickets) {
        return tickets.stream()
                      .filter(ticket -> ticket.getStoryPoints() == null)
                      .sorted(Comparator.comparing(Ticket::getIdentifier))
                      .map(ticket -> new BurndownWarning(ticket.getId(),
                                                         ticket.getIdentifier(),
                                                         BurndownWarning.MISSING_STORY_POINTS))
                      .toList();
    }

    private static int remainingToday(List<Ticket> tickets, LocalDate today) {
        return remainingOn(tickets, today);
    }

    static int remainingOn(List<Ticket> tickets, LocalDate day) {
        return tickets.stream()
                      .filter(ticket -> {
                          var burnDay = burnDay(ticket);
                          return burnDay == null || burnDay.isAfter(day);
                      })
                      .mapToInt(ticket -> Objects.requireNonNullElse(ticket.getStoryPoints(), 0))
                      .sum();
    }

    static double idealOn(int commitment, LocalDate start, LocalDate end, LocalDate day) {
        if (start.equals(end)) {
            return 0;
        }
        var totalDays = ChronoUnit.DAYS.between(start, end);
        var elapsed = ChronoUnit.DAYS.between(start, day);
        if (elapsed <= 0) {
            return commitment;
        }
        if (elapsed >= totalDays) {
            return 0;
        }
        return commitment * (1.0 - ((double) elapsed / totalDays));
    }

    static LocalDate burnDay(Ticket ticket) {
        var finished = ticket.getFinishedAt();
        var canceled = ticket.getCanceledAt();
        if (finished == null && canceled == null) {
            return null;
        }
        if (finished == null) {
            return canceled.toLocalDate();
        }
        if (canceled == null) {
            return finished.toLocalDate();
        }
        var finishedDate = finished.toLocalDate();
        var canceledDate = canceled.toLocalDate();
        return finishedDate.isBefore(canceledDate) ? finishedDate : canceledDate;
    }
}

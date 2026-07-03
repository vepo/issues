package dev.vepo.issues.phase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PhaseResponse(long id,
                            long projectId,
                            String name,
                            String objective,
                            PhaseStatus status,
                            LocalDate startDate,
                            LocalDate endDate,
                            Long deliverableVersionId,
                            String deliverableVersionLabel,
                            LocalDateTime createdAt,
                            LocalDateTime completedAt,
                            List<PhaseDeliverableResponse> deliverables) {
    public static PhaseResponse load(Phase phase) {
        return new PhaseResponse(phase.getId(),
                                 phase.getProject().getId(),
                                 phase.getName(),
                                 phase.getObjective(),
                                 phase.getStatus(),
                                 phase.getStartDate(),
                                 phase.getEndDate(),
                                 phase.getDeliverableVersion() != null ? phase.getDeliverableVersion().getId() : null,
                                 phase.getDeliverableVersion() != null ? phase.getDeliverableVersion().getLabel() : null,
                                 phase.getCreatedAt(),
                                 phase.getCompletedAt(),
                                 phase.getDeliverables()
                                      .stream()
                                      .map(PhaseDeliverableResponse::load)
                                      .toList());
    }
}

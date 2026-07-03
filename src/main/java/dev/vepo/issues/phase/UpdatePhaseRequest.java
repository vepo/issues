package dev.vepo.issues.phase;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePhaseRequest(@NotBlank @Size(max = 128) String name,
                                 String objective,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 Long deliverableVersionId,
                                 List<@NotBlank String> deliverables) {}

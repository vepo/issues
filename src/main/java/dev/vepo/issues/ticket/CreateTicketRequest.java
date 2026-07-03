package dev.vepo.issues.ticket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTicketRequest(@NotNull @Size(min = 5, max = 255) String title,
                                  @NotNull @Size(min = 5, max = 1200) String description,
                                  @NotNull Long categoryId,
                                  @NotNull Long projectId,
                                  TicketPriority priority,
                                  LocalDate dueDate,
                                  Long phaseId) {}
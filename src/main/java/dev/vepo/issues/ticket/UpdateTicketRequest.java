package dev.vepo.issues.ticket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTicketRequest(@NotNull @Size(min = 5, max = 255) String title,
                                  @NotNull @Size(min = 5, max = 1200) String description,
                                  @NotNull Long categoryId,
                                  @NotNull TicketPriority priority,
                                  LocalDate dueDate,
                                  TicketPlanningFields planningFields) {}
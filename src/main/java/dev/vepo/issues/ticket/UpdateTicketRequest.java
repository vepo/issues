package dev.vepo.issues.ticket;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import dev.vepo.issues.customfield.CustomFieldValueRequest;
import dev.vepo.issues.infra.PlainTextSize;

public record UpdateTicketRequest(@NotNull @Size(min = 5, max = 255) String title,
                                  @NotNull @PlainTextSize(min = 5, max = 1200) String description,
                                  @NotNull Long categoryId,
                                  @NotNull TicketPriority priority,
                                  TicketType ticketType,
                                  LocalDate dueDate,
                                  TicketPlanningFields planningFields,
                                  @Min(0) Integer storyPoints,
                                  List<CustomFieldValueRequest> customFields) {}

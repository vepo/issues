package dev.vepo.issues.ticket.backlog;

import java.util.List;

public record BacklogPageResponse(List<BacklogTicketResponse> items, long total, int page, int size, boolean hasMore) {}

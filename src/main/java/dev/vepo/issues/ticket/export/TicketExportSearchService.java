package dev.vepo.issues.ticket.export;

import java.util.List;
import java.util.Set;

import dev.vepo.issues.ticket.search.query.TicketQueryLanguageService;
import dev.vepo.issues.ticket.search.saved.SavedQueryRepository;
import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class TicketExportSearchService {

    private final TicketExportRepository repository;
    private final TicketQueryLanguageService queryLanguageService;
    private final SavedQueryRepository savedQueryRepository;

    @Inject
    public TicketExportSearchService(TicketExportRepository repository,
                                     TicketQueryLanguageService queryLanguageService,
                                     SavedQueryRepository savedQueryRepository) {
        this.repository = repository;
        this.queryLanguageService = queryLanguageService;
        this.savedQueryRepository = savedQueryRepository;
    }

    public TicketExportSearchResult search(TicketExportCriteria criteria,
                                           Set<Long> readableProjectIds,
                                           int limit,
                                           boolean includeDeleted,
                                           User requestingUser) {
        if (readableProjectIds.isEmpty()) {
            return new TicketExportSearchResult(List.of(), false);
        }
        return switch (criteria) {
            case SimpleTicketExportCriteria simple -> new TicketExportSearchResult(repository.search(simple,
                                                                                                     readableProjectIds,
                                                                                                     limit,
                                                                                                     includeDeleted),
                                                                                   false);
            case AdvancedTicketExportCriteria(var query) -> searchQuery(query,
                                                                        readableProjectIds,
                                                                        limit,
                                                                        includeDeleted,
                                                                        requestingUser);
            case SavedTicketExportCriteria(var slug) -> {
                var savedQuery = savedQueryRepository.findBySlug(slug)
                                                     .orElseThrow(() -> new BadRequestException("Saved query not found"));
                yield searchQuery(savedQuery.getQueryText(),
                                  readableProjectIds,
                                  limit,
                                  includeDeleted,
                                  requestingUser);
            }
        };
    }

    private TicketExportSearchResult searchQuery(String queryText,
                                                 Set<Long> readableProjectIds,
                                                 int limit,
                                                 boolean includeDeleted,
                                                 User requestingUser) {
        var parsedQuery = queryLanguageService.parse(queryText);
        var tickets = queryLanguageService.execute(queryText, requestingUser, readableProjectIds, limit)
                                          .stream()
                                          .filter(ticket -> includeDeleted || !ticket.isDeleted())
                                          .toList();
        return new TicketExportSearchResult(tickets, parsedQuery.order().isPresent());
    }
}

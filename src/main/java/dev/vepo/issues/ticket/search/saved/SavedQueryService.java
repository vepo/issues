package dev.vepo.issues.ticket.search.saved;

import java.util.List;
import java.util.UUID;

import dev.vepo.issues.infra.IssuesException;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.search.query.InvalidQueryException;
import dev.vepo.issues.ticket.search.query.TicketQueryLanguageService;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SavedQueryService {

    private final SavedQueryRepository repository;
    private final UserRepository userRepository;
    private final TicketQueryLanguageService queryLanguageService;

    @Inject
    public SavedQueryService(SavedQueryRepository repository,
                             UserRepository userRepository,
                             TicketQueryLanguageService queryLanguageService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.queryLanguageService = queryLanguageService;
    }

    @Transactional
    public SavedQueryResponse create(CreateSavedQueryRequest request, String ownerUsername) {
        queryLanguageService.validate(request.query());
        var owner = requireUser(ownerUsername);
        var savedQuery = new SavedQuery();
        savedQuery.setSlug(UUID.randomUUID().toString());
        savedQuery.setName(request.name());
        savedQuery.setQueryText(request.query());
        savedQuery.setShowAtHome(request.showAtHome());
        savedQuery.setOwner(owner);
        repository.persist(savedQuery);
        return SavedQueryResponse.load(savedQuery);
    }

    public List<SavedQueryResponse> listForOwner(String ownerUsername) {
        var owner = requireUser(ownerUsername);
        return repository.findByOwnerId(owner.getId())
                         .stream()
                         .map(SavedQueryResponse::load)
                         .toList();
    }

    public SavedQueryWithResultsResponse findBySlug(String slug, String username) {
        var savedQuery = requireBySlug(slug);
        var user = requireUser(username);
        var tickets = queryLanguageService.execute(savedQuery.getQueryText(), user)
                                          .stream()
                                          .map(TicketResponse::load)
                                          .toList();
        return SavedQueryWithResultsResponse.load(savedQuery, tickets);
    }

    @Transactional
    public SavedQueryResponse update(long id, UpdateSavedQueryRequest request, String ownerUsername) {
        queryLanguageService.validate(request.query());
        var savedQuery = requireOwned(id, ownerUsername);
        savedQuery.setName(request.name());
        savedQuery.setQueryText(request.query());
        savedQuery.setShowAtHome(request.showAtHome());
        return SavedQueryResponse.load(repository.merge(savedQuery));
    }

    @Transactional
    public void delete(long id, String ownerUsername) {
        repository.delete(requireOwned(id, ownerUsername));
    }

    @Transactional
    public SavedQueryResponse clone(long id, String username) {
        var source = repository.findById(id)
                               .orElseThrow(() -> new IssuesException("Saved query not found"));
        var cloner = requireUser(username);
        if (source.getOwner().getId().equals(cloner.getId())) {
            throw new IssuesException("Cannot clone your own saved query");
        }
        queryLanguageService.validate(source.getQueryText());
        var copy = new SavedQuery();
        copy.setSlug(UUID.randomUUID().toString());
        copy.setName("%s (cópia)".formatted(source.getName()));
        copy.setQueryText(source.getQueryText());
        copy.setShowAtHome(false);
        copy.setOwner(cloner);
        repository.persist(copy);
        return SavedQueryResponse.load(copy);
    }

    public List<HomeSavedQuerySectionResponse> listHomeSections(String username) {
        var owner = requireUser(username);
        return repository.findShowAtHomeByOwnerId(owner.getId())
                         .stream()
                         .map(savedQuery -> {
                             var tickets = queryLanguageService.execute(savedQuery.getQueryText(), owner)
                                                               .stream()
                                                               .map(TicketResponse::load)
                                                               .toList();
                             return HomeSavedQuerySectionResponse.load(savedQuery, tickets);
                         })
                         .toList();
    }

    private SavedQuery requireOwned(long id, String ownerUsername) {
        var savedQuery = repository.findById(id)
                                   .orElseThrow(() -> new IssuesException("Saved query not found"));
        var owner = requireUser(ownerUsername);
        if (!savedQuery.getOwner().getId().equals(owner.getId())) {
            throw new IssuesException("Only the owner can modify this saved query");
        }
        return savedQuery;
    }

    private SavedQuery requireBySlug(String slug) {
        return repository.findBySlug(slug)
                         .orElseThrow(() -> new IssuesException("Saved query not found"));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                             .orElseThrow(() -> new IssuesException("User not found"));
    }
}

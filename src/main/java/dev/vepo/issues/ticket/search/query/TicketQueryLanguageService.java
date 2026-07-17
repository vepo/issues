package dev.vepo.issues.ticket.search.query;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.criteria.Order;

@ApplicationScoped
public class TicketQueryLanguageService {

    private final TicketQueryRepository ticketQueryRepository;
    private final ProjectAccessService projectAccessService;
    private final UserRepository userRepository;

    @Inject
    public TicketQueryLanguageService(TicketQueryRepository ticketQueryRepository,
                                      ProjectAccessService projectAccessService,
                                      UserRepository userRepository) {
        this.ticketQueryRepository = ticketQueryRepository;
        this.projectAccessService = projectAccessService;
        this.userRepository = userRepository;
    }

    public List<Ticket> execute(String queryText, String username) {
        return execute(queryText, requireUser(username));
    }

    public List<Ticket> execute(String queryText, User requestingUser) {
        var readableProjectIds = projectAccessService.readableProjectIds(requestingUser);
        return execute(queryText, requestingUser, readableProjectIds, Integer.MAX_VALUE);
    }

    public List<Ticket> execute(String queryText,
                                User requestingUser,
                                Set<Long> readableProjectIds,
                                int limit) {
        Objects.requireNonNull(queryText, "queryText cannot be null");
        if (queryText.isBlank()) {
            throw new InvalidQueryException("Query must not be blank");
        }
        var parsedQuery = parse(queryText.trim());
        return ticketQueryRepository.search(parsedQuery, requestingUser, readableProjectIds, limit);
    }

    public void validate(String queryText) {
        parse(queryText.trim());
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                             .orElseThrow(() -> new InvalidQueryException("User not found"));
    }

    public ParsedTicketQuery parse(String queryText) {
        var lexer = new TicketQueryLexer(CharStreams.fromString(queryText));
        var parser = new TicketQueryParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                throw new InvalidQueryException("Syntax error at line %d:%d — %s".formatted(line, charPositionInLine, msg));
            }
        });
        try {
            var tree = parser.query();
            var order = tree.sortField != null
                                               ? Optional.of(new TicketQueryOrder(normalizeOrderField(tree.sortField.getText()),
                                                                                  tree.sortDir == null || tree.sortDir.getType() != TicketQueryParser.DESC))
                                               : Optional.<TicketQueryOrder>empty();
            return new ParsedTicketQuery(order, tree.expression());
        } catch (ParseCancellationException ex) {
            throw new InvalidQueryException("Invalid query syntax", ex);
        }
    }

    private static String normalizeOrderField(String field) {
        return field.replace("_", "").toLowerCase(Locale.ROOT);
    }

    static Order buildOrder(jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Root<Ticket> ticket, TicketQueryOrder order) {
        var path = switch (order.field()) {
            case "created", "createdat" -> ticket.get("createdAt");
            case "updated", "updatedat" -> ticket.get("updatedAt");
            case "finished", "finishedat" -> ticket.get("finishedAt");
            case "title" -> ticket.get("title");
            case "identifier" -> ticket.get("identifier");
            case "priority" -> ticket.get("priority");
            default -> throw new InvalidQueryException("ORDER BY not supported for field: %s".formatted(order.field()));
        };
        return order.ascending() ? cb.asc(path) : cb.desc(path);
    }
}

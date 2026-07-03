package dev.vepo.issues.ticket.search.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.ticket.comments.Comment;
import dev.vepo.issues.user.User;
import dev.vepo.issues.ticket.search.query.TicketQueryParser.AndExprContext;
import dev.vepo.issues.ticket.search.query.TicketQueryParser.ClauseExprContext;
import dev.vepo.issues.ticket.search.query.TicketQueryParser.CompareClauseContext;
import dev.vepo.issues.ticket.search.query.TicketQueryParser.InClauseContext;
import dev.vepo.issues.ticket.search.query.TicketQueryParser.IsEmptyClauseContext;
import dev.vepo.issues.ticket.search.query.TicketQueryParser.IsNotEmptyClauseContext;
import dev.vepo.issues.ticket.search.query.TicketQueryParser.LiteralContext;
import dev.vepo.issues.ticket.search.query.TicketQueryParser.NotInClauseContext;
import dev.vepo.issues.ticket.search.query.TicketQueryParser.OrExprContext;
import dev.vepo.issues.ticket.search.query.TicketQueryParser.ParenExprContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

final class TicketQueryPredicateBuilder extends TicketQueryBaseVisitor<Predicate> {

    private final CriteriaBuilder cb;
    private final Root<Ticket> ticket;
    private final CriteriaQuery<?> cq;
    private final User currentUser;

    TicketQueryPredicateBuilder(CriteriaBuilder cb, Root<Ticket> ticket, CriteriaQuery<?> cq, User currentUser) {
        this.cb = cb;
        this.ticket = ticket;
        this.cq = cq;
        this.currentUser = currentUser;
    }

    @Override
    public Predicate visitAndExpr(AndExprContext ctx) {
        return cb.and(visit(ctx.expression(0)), visit(ctx.expression(1)));
    }

    @Override
    public Predicate visitOrExpr(OrExprContext ctx) {
        return cb.or(visit(ctx.expression(0)), visit(ctx.expression(1)));
    }

    @Override
    public Predicate visitParenExpr(ParenExprContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Predicate visitClauseExpr(ClauseExprContext ctx) {
        return visit(ctx.clause());
    }

    @Override
    public Predicate visitCompareClause(CompareClauseContext ctx) {
        var field = normalizeField(ctx.field.getText());
        var op = ctx.op.getText();
        var value = resolveLiteral(ctx.value);
        return compare(field, op, value);
    }

    @Override
    public Predicate visitInClause(InClauseContext ctx) {
        var field = normalizeField(ctx.field.getText());
        var values = literalValues(ctx.literals());
        return inClause(field, values, false);
    }

    @Override
    public Predicate visitNotInClause(NotInClauseContext ctx) {
        var field = normalizeField(ctx.field.getText());
        var values = literalValues(ctx.literals());
        return inClause(field, values, true);
    }

    @Override
    public Predicate visitIsEmptyClause(IsEmptyClauseContext ctx) {
        return isEmpty(normalizeField(ctx.field.getText()), true);
    }

    @Override
    public Predicate visitIsNotEmptyClause(IsNotEmptyClauseContext ctx) {
        return isEmpty(normalizeField(ctx.field.getText()), false);
    }

    private Predicate compare(String field, String op, Object value) {
        return switch (field) {
            case "project" -> stringCompare(ticket.join("project", JoinType.INNER).get("name"), op, (String) value);
            case "status" -> stringCompare(ticket.join("status", JoinType.INNER).get("name"), op, (String) value);
            case "category" -> stringCompare(ticket.join("category", JoinType.INNER).get("name"), op, (String) value);
            case "assignee" -> userCompare(ticket.join("assignee", JoinType.LEFT), op, value);
            case "author" -> userCompare(ticket.join("author", JoinType.INNER), op, value);
            case "priority" -> enumCompare(op, (String) value);
            case "title" -> textCompare("title", op, (String) value);
            case "description" -> textCompare("description", op, (String) value);
            case "identifier" -> textCompare("identifier", op, (String) value);
            case "comment" -> commentCompare(op, (String) value);
            case "phase" -> stringCompare(ticket.join("phase", JoinType.LEFT).get("name"), op, (String) value);
            case "targetversion" -> stringCompare(ticket.join("targetVersion", JoinType.LEFT).get("label"), op, (String) value);
            case "observedversion" -> stringCompare(ticket.join("observedVersion", JoinType.LEFT).get("label"), op, (String) value);
            case "created", "createdat" -> dateCompare(ticket.get("createdAt"), op, (String) value);
            case "updated", "updatedat" -> dateCompare(ticket.get("updatedAt"), op, (String) value);
            case "finished", "finishedat" -> dateCompare(ticket.get("finishedAt"), op, (String) value);
            case "duedate", "due" -> localDateCompare(ticket.get("dueDate"), op, (String) value);
            default -> throw new InvalidQueryException("Unknown field: %s".formatted(field));
        };
    }

    private Predicate inClause(String field, List<String> values, boolean negate) {
        if (!"project".equals(field)) {
            throw new InvalidQueryException("IN is supported for project field only");
        }
        var projectName = ticket.join("project", JoinType.INNER).<String>get("name");
        var lowered = values.stream().map(v -> v.toLowerCase(Locale.ROOT)).toList();
        var predicate = cb.lower(projectName).in(lowered);
        return negate ? cb.not(predicate) : predicate;
    }

    private Predicate isEmpty(String field, boolean empty) {
        var predicate = switch (field) {
            case "assignee" -> cb.isNull(ticket.get("assignee"));
            case "phase" -> cb.isNull(ticket.get("phase"));
            case "targetversion" -> cb.isNull(ticket.get("targetVersion"));
            case "observedversion" -> cb.isNull(ticket.get("observedVersion"));
            case "finished", "finishedat" -> cb.isNull(ticket.get("finishedAt"));
            case "duedate", "due" -> cb.isNull(ticket.get("dueDate"));
            case "description" -> cb.or(cb.isNull(ticket.get("description")), cb.equal(ticket.get("description"), ""));
            default -> throw new InvalidQueryException("IS EMPTY not supported for field: %s".formatted(field));
        };
        return empty ? predicate : cb.not(predicate);
    }

    private Predicate stringCompare(Expression<String> path, String op, String value) {
        if ("~".equals(op)) {
            var pattern = "%%%s%%".formatted(value.toLowerCase(Locale.ROOT));
            return cb.like(cb.lower(path), pattern);
        }
        var normalized = value.toLowerCase(Locale.ROOT);
        return switch (op) {
            case "=" -> cb.equal(cb.lower(path), normalized);
            case "!=" -> cb.notEqual(cb.lower(path), normalized);
            default -> throw new InvalidQueryException("Operator %s not supported for text field".formatted(op));
        };
    }

    private Predicate userCompare(jakarta.persistence.criteria.Path<User> userPath, String op, Object value) {
        if (value instanceof User user) {
            return switch (op) {
                case "=" -> cb.equal(userPath.get("id"), user.getId());
                case "!=" -> cb.or(cb.isNull(userPath), cb.notEqual(userPath.get("id"), user.getId()));
                default -> throw new InvalidQueryException("Operator %s not supported for user field".formatted(op));
            };
        }
        var text = ((String) value).toLowerCase(Locale.ROOT);
        var pattern = "%%%s%%".formatted(text);
        if ("~".equals(op)) {
            return cb.or(cb.like(cb.lower(userPath.get("email")), pattern), cb.like(cb.lower(userPath.get("name")), pattern));
        }
        return switch (op) {
            case "=" -> cb.or(cb.equal(cb.lower(userPath.get("email")), text), cb.equal(cb.lower(userPath.get("name")), text));
            case "!=" -> cb.not(cb.or(cb.equal(cb.lower(userPath.get("email")), text), cb.equal(cb.lower(userPath.get("name")), text)));
            default -> throw new InvalidQueryException("Operator %s not supported for user field".formatted(op));
        };
    }

    private Predicate enumCompare(String op, String value) {
        var priority = TicketPriority.valueOf(value.toUpperCase(Locale.ROOT));
        return switch (op) {
            case "=" -> cb.equal(ticket.get("priority"), priority);
            case "!=" -> cb.notEqual(ticket.get("priority"), priority);
            default -> throw new InvalidQueryException("Operator %s not supported for priority".formatted(op));
        };
    }

    private Predicate textCompare(String field, String op, String value) {
        if ("~".equals(op)) {
            if ("title".equals(field) || "description".equals(field) || "identifier".equals(field)) {
                return ticketTextMatch(value);
            }
            return stringCompare(ticket.get(field), op, value);
        }
        return stringCompare(ticket.get(field), op, value);
    }

    private Predicate ticketTextMatch(String value) {
        var pattern = "%%%s%%".formatted(value.toLowerCase(Locale.ROOT));
        return cb.or(cb.like(cb.lower(ticket.get("title")), pattern),
                     cb.like(cb.lower(ticket.get("description")), pattern),
                     cb.like(cb.lower(ticket.get("identifier")), pattern));
    }

    private Predicate commentCompare(String op, String value) {
        if (!"~".equals(op)) {
            throw new InvalidQueryException("comment field supports ~ operator only");
        }
        var sub = cq.subquery(Long.class);
        var comment = sub.from(Comment.class);
        sub.select(cb.literal(1L));
        var pattern = "%%%s%%".formatted(value.toLowerCase(Locale.ROOT));
        sub.where(cb.equal(comment.get("ticket"), ticket), cb.like(cb.lower(comment.get("content")), pattern));
        return cb.exists(sub);
    }

    private Predicate dateCompare(Expression<LocalDateTime> path, String op, String value) {
        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidQueryException("Invalid date value: %s".formatted(value), ex);
        }
        return switch (op) {
            case "=" -> cb.equal(path, dateTime);
            case "!=" -> cb.notEqual(path, dateTime);
            case ">" -> cb.greaterThan(path, dateTime);
            case "<" -> cb.lessThan(path, dateTime);
            case ">=" -> cb.greaterThanOrEqualTo(path, dateTime);
            case "<=" -> cb.lessThanOrEqualTo(path, dateTime);
            default -> throw new InvalidQueryException("Operator %s not supported for date field".formatted(op));
        };
    }

    private Predicate localDateCompare(Expression<LocalDate> path, String op, String value) {
        LocalDate date;
        try {
            date = LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidQueryException("Invalid date value: %s".formatted(value), ex);
        }
        return switch (op) {
            case "=" -> cb.equal(path, date);
            case "!=" -> cb.notEqual(path, date);
            case ">" -> cb.greaterThan(path, date);
            case "<" -> cb.lessThan(path, date);
            case ">=" -> cb.greaterThanOrEqualTo(path, date);
            case "<=" -> cb.lessThanOrEqualTo(path, date);
            default -> throw new InvalidQueryException("Operator %s not supported for date field".formatted(op));
        };
    }

    private Object resolveLiteral(LiteralContext ctx) {
        if (ctx.CURRENTUSER() != null || ctx.ME() != null) {
            return currentUser;
        }
        if (ctx.STRING() != null) {
            return unquote(ctx.STRING().getText());
        }
        if (ctx.NUMBER() != null) {
            return ctx.NUMBER().getText();
        }
        throw new InvalidQueryException("Unsupported literal");
    }

    private List<String> literalValues(TicketQueryParser.LiteralsContext ctx) {
        var values = new ArrayList<String>();
        for (var literal : ctx.literal()) {
            var resolved = resolveLiteral(literal);
            if (!(resolved instanceof String text)) {
                throw new InvalidQueryException("IN list requires string literals");
            }
            values.add(text);
        }
        return values;
    }

    private static String unquote(String token) {
        return token.substring(1, token.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String normalizeField(String field) {
        return field.replace("_", "").toLowerCase(Locale.ROOT);
    }
}

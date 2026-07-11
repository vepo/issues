package dev.vepo.issues.ticket.search.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.vepo.issues.customfield.CustomFieldType;
import dev.vepo.issues.customfield.TicketCustomFieldValue;
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

    private static final String CUSTOM_FIELD_PREFIX = "cf.";

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
        if (isCustomField(field)) {
            return customFieldCompare(customFieldKey(field), op, value);
        }
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
        if (isCustomField(field)) {
            return customFieldIn(customFieldKey(field), values, negate);
        }
        if (!"project".equals(field)) {
            throw new InvalidQueryException("IN is supported for project field only");
        }
        var projectName = ticket.join("project", JoinType.INNER).<String>get("name");
        var lowered = values.stream().map(v -> v.toLowerCase(Locale.ROOT)).toList();
        var predicate = cb.lower(projectName).in(lowered);
        return negate ? cb.not(predicate) : predicate;
    }

    private Predicate isEmpty(String field, boolean empty) {
        if (isCustomField(field)) {
            return customFieldIsEmpty(customFieldKey(field), empty);
        }
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

    private Predicate customFieldCompare(String key, String op, Object value) {
        var text = value instanceof String s ? s : String.valueOf(value);
        var sub = cq.subquery(Long.class);
        var cfv = sub.from(TicketCustomFieldValue.class);
        var cf = cfv.join("customField");
        sub.select(cb.literal(1L));

        var typePredicates = new ArrayList<Predicate>();
        if ("~".equals(op)) {
            typePredicates.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.STRING),
                                      stringCompare(cfv.get("stringValue"), op, text)));
            typePredicates.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.TEXT),
                                      stringCompare(cfv.get("textValue"), op, text)));
            typePredicates.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.ENUM),
                                      enumOptionCompare(cfv, op, text)));
        } else if (isIntegerOperator(op)) {
            typePredicates.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.INTEGER),
                                      integerCompare(cfv.get("integerValue"), op, text)));
        } else if ("=".equals(op) || "!=".equals(op)) {
            typePredicates.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.STRING),
                                      stringCompare(cfv.get("stringValue"), op, text)));
            typePredicates.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.TEXT),
                                      stringCompare(cfv.get("textValue"), op, text)));
            typePredicates.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.ENUM),
                                      enumOptionCompare(cfv, op, text)));
            if (isIntegerLiteral(text)) {
                typePredicates.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.INTEGER),
                                          integerCompare(cfv.get("integerValue"), op, text)));
            }
            if (isBooleanLiteral(text)) {
                typePredicates.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.BOOLEAN),
                                          booleanCompare(cfv.get("booleanValue"), op, text)));
            }
        } else {
            throw new InvalidQueryException("Operator %s not supported for custom field %s".formatted(op, key));
        }

        sub.where(cb.equal(cfv.get("id").get("ticketId"), ticket.get("id")),
                  cb.equal(cf.get("key"), key),
                  cb.or(typePredicates.toArray(Predicate[]::new)));
        return cb.exists(sub);
    }

    private Predicate customFieldIn(String key, List<String> values, boolean negate) {
        var sub = cq.subquery(Long.class);
        var cfv = sub.from(TicketCustomFieldValue.class);
        var cf = cfv.join("customField");
        sub.select(cb.literal(1L));

        var lowered = values.stream().map(v -> v.toLowerCase(Locale.ROOT)).toList();
        var integers = new ArrayList<Integer>();
        for (var value : values) {
            try {
                integers.add(Integer.parseInt(value.trim()));
            } catch (NumberFormatException ignored) {
                // not an integer literal for this value
            }
        }

        var option = cfv.join("enumOption", JoinType.LEFT);
        var matches = new ArrayList<Predicate>();
        matches.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.STRING), cb.lower(cfv.get("stringValue")).in(lowered)));
        matches.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.TEXT), cb.lower(cfv.get("textValue")).in(lowered)));
        matches.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.ENUM), cb.lower(option.get("value")).in(lowered)));
        if (!integers.isEmpty()) {
            matches.add(cb.and(cb.equal(cf.get("type"), CustomFieldType.INTEGER), cfv.get("integerValue").in(integers)));
        }

        sub.where(cb.equal(cfv.get("id").get("ticketId"), ticket.get("id")),
                  cb.equal(cf.get("key"), key),
                  cb.or(matches.toArray(Predicate[]::new)));
        var exists = cb.exists(sub);
        return negate ? cb.not(exists) : exists;
    }

    private Predicate customFieldIsEmpty(String key, boolean empty) {
        var sub = cq.subquery(Long.class);
        var cfv = sub.from(TicketCustomFieldValue.class);
        var cf = cfv.join("customField");
        sub.select(cb.literal(1L));
        sub.where(cb.equal(cfv.get("id").get("ticketId"), ticket.get("id")), cb.equal(cf.get("key"), key));
        var exists = cb.exists(sub);
        return empty ? cb.not(exists) : exists;
    }

    private Predicate enumOptionCompare(jakarta.persistence.criteria.From<?, TicketCustomFieldValue> cfv, String op, String value) {
        var option = cfv.join("enumOption", JoinType.LEFT).<String>get("value");
        return stringCompare(option, op, value);
    }

    private Predicate integerCompare(Expression<Integer> path, String op, String value) {
        int number;
        try {
            number = Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new InvalidQueryException("Invalid integer value: %s".formatted(value), ex);
        }
        return switch (op) {
            case "=" -> cb.equal(path, number);
            case "!=" -> cb.notEqual(path, number);
            case ">" -> cb.greaterThan(path, number);
            case "<" -> cb.lessThan(path, number);
            case ">=" -> cb.greaterThanOrEqualTo(path, number);
            case "<=" -> cb.lessThanOrEqualTo(path, number);
            default -> throw new InvalidQueryException("Operator %s not supported for integer custom field".formatted(op));
        };
    }

    private Predicate booleanCompare(Expression<Boolean> path, String op, String value) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new InvalidQueryException("Invalid boolean value: %s".formatted(value));
        }
        var bool = Boolean.parseBoolean(value);
        return switch (op) {
            case "=" -> cb.equal(path, bool);
            case "!=" -> cb.notEqual(path, bool);
            default -> throw new InvalidQueryException("Operator %s not supported for boolean custom field".formatted(op));
        };
    }

    private static boolean isIntegerOperator(String op) {
        return ">".equals(op) || "<".equals(op) || ">=".equals(op) || "<=".equals(op);
    }

    private static boolean isIntegerLiteral(String value) {
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean isBooleanLiteral(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean isCustomField(String field) {
        return field.startsWith(CUSTOM_FIELD_PREFIX);
    }

    private static String customFieldKey(String field) {
        return field.substring(CUSTOM_FIELD_PREFIX.length());
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
        if (field.regionMatches(true, 0, CUSTOM_FIELD_PREFIX, 0, CUSTOM_FIELD_PREFIX.length())) {
            return CUSTOM_FIELD_PREFIX + field.substring(CUSTOM_FIELD_PREFIX.length());
        }
        return field.replace("_", "").toLowerCase(Locale.ROOT);
    }
}

package dev.vepo.issues.ticket.comments;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class MentionParser {

    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<!\\w)@([A-Za-z0-9_-]+)");

    private MentionParser() {}

    public static Set<String> extractUsernames(String content) {
        if (content == null || content.isBlank()) {
            return Set.of();
        }
        var matcher = MENTION_PATTERN.matcher(content);
        var usernames = new LinkedHashSet<String>();
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        return usernames;
    }
}

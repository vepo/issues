package dev.vepo.issues.git;

final class CommitDeepLinkBuilder {

    private CommitDeepLinkBuilder() {}

    static String resolve(String commitUrl, String remoteUrl, GitProvider provider, String sha) {
        if (commitUrl != null && !commitUrl.isBlank()) {
            return commitUrl.trim();
        }
        if (remoteUrl == null || remoteUrl.isBlank() || sha == null || sha.isBlank()) {
            return null;
        }
        var base = normalizeRemote(remoteUrl);
        if (base == null) {
            return null;
        }
        var effective = provider != null ? provider : detectProvider(base);
        return switch (effective) {
            case GITHUB, GITEA -> base + "/commit/" + sha;
            case GITLAB -> base + "/-/commit/" + sha;
            case OTHER -> base + "/commit/" + sha;
        };
    }

    static GitProvider detectProvider(String remoteUrl) {
        var lower = remoteUrl.toLowerCase();
        if (lower.contains("github.com")) {
            return GitProvider.GITHUB;
        }
        if (lower.contains("gitlab")) {
            return GitProvider.GITLAB;
        }
        if (lower.contains("gitea") || lower.contains("codeberg.org")) {
            return GitProvider.GITEA;
        }
        return GitProvider.OTHER;
    }

    private static String normalizeRemote(String remoteUrl) {
        var url = remoteUrl.trim();
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.startsWith("git@")) {
            // git@host:org/repo → https://host/org/repo
            var at = url.indexOf('@');
            var colon = url.indexOf(':', at + 1);
            if (at > 0 && colon > at) {
                var host = url.substring(at + 1, colon);
                var path = url.substring(colon + 1);
                return "https://" + host + "/" + path;
            }
        }
        return url;
    }
}

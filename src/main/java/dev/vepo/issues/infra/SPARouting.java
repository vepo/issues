package dev.vepo.issues.infra;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class SPARouting {
    private static final String SPA_ROOT = "/";
    private static final String[] PASSTHROUGH_PATH_PREFIXES = { "/q/", "/api/", "/@" };
    private static final Predicate<String> IS_STATIC_RESOURCE_PATH = Pattern.compile(".+\\.[a-zA-Z0-9]+$")
                                                                            .asMatchPredicate();

    public void init(@Observes Router router) {
        router.get("/*").handler(this::handleSpaFallback);
    }

    private void handleSpaFallback(RoutingContext routingContext) {
        var path = routingContext.normalizedPath();
        if (shouldPassThrough(path)) {
            routingContext.next();
        } else {
            routingContext.reroute(SPA_ROOT);
        }
    }

    private static boolean shouldPassThrough(String path) {
        return SPA_ROOT.equals(path)
                || Stream.of(PASSTHROUGH_PATH_PREFIXES).anyMatch(path::startsWith)
                || IS_STATIC_RESOURCE_PATH.test(path);
    }
}

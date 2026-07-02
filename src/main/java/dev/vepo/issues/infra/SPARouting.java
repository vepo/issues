package dev.vepo.issues.infra;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class SPARouting {
    // Vite in dev mode requests /@vite/client and /@reactrefresh so add "/@" if you
    // use Vite
    private static final String[] PATH_PREFIXES = { "/q/", "/api/", "/@" };
    private static final Predicate<String> FILE_NAME_PREDICATE = Pattern.compile(".+\\.[a-zA-Z0-9]+$")
                                                                        .asMatchPredicate();

    public void init(@Observes Router router) {
        router.get("/*").handler(rc -> {
            final String path = rc.normalizedPath();
            if (!path.equals("/")
                    && Stream.of(PATH_PREFIXES).noneMatch(path::startsWith)
                    && !FILE_NAME_PREDICATE.test(path)) {
                rc.reroute("/");
            } else {
                rc.next();
            }
        });
    }
}
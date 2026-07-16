package dev.vepo.issues.infra;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.vepo.issues.user.UiLocale;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class SPARouting {
    private static final String[] PATH_PREFIXES = { "/q/", "/api/", "/@" };
    private static final Predicate<String> FILE_NAME_PREDICATE = Pattern.compile(".+\\.[a-zA-Z0-9]+$")
                                                                        .asMatchPredicate();

    public void init(@Observes Router router) {
        router.get("/*").handler(rc -> {
            final String path = rc.normalizedPath();
            if (path.equals("/")) {
                var locale = UiLocale.fromAcceptLanguage(rc.request().getHeader("Accept-Language"));
                rc.response()
                  .setStatusCode(302)
                  .putHeader("Location", "/" + locale + "/")
                  .end();
                return;
            }
            if (isLocaleRootOrPrefix(path)) {
                if (FILE_NAME_PREDICATE.test(path)) {
                    rc.next();
                } else {
                    var locale = path.startsWith("/en") ? "en" : "pt";
                    rc.reroute("/" + locale + "/");
                }
                return;
            }
            if (Stream.of(PATH_PREFIXES).noneMatch(path::startsWith) && !FILE_NAME_PREDICATE.test(path)) {
                var locale = UiLocale.fromAcceptLanguage(rc.request().getHeader("Accept-Language"));
                rc.response()
                  .setStatusCode(302)
                  .putHeader("Location", "/" + locale + path)
                  .end();
                return;
            }
            rc.next();
        });
    }

    private static boolean isLocaleRootOrPrefix(String path) {
        return path.equals("/pt") || path.equals("/en")
                || path.startsWith("/pt/") || path.startsWith("/en/");
    }
}

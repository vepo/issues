package dev.vepo.issues.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class SPARoutingTest {

    @Test
    void shouldPassThroughAtSpaRootToStopFallbackRecursion() {
        var routing = routingFor("/");

        routing.handle();

        assertThat(routing.nextCount()).isOne();
        assertThat(routing.reroutes()).isEmpty();
    }

    @Test
    void shouldRerouteOrdinaryDeepLinkToSpaRootOnce() {
        var routing = routingFor("/account/settings");

        routing.handle();

        assertThat(routing.nextCount()).isZero();
        assertThat(routing.reroutes()).containsExactly("/");
    }

    @Test
    void shouldContinueInfrastructureAndAssetRequests() {
        assertContinues("/api/");
        assertContinues("/api/tickets/ISS-1");
        assertContinues("/q/health");
        assertContinues("/@vite/client");
        assertContinues("/assets/i18n/en.json");
        assertContinues("/main.123abc.js");
    }

    @Test
    void shouldTreatFormerLocalePathsAsOrdinaryDeepLinks() {
        assertReroutesToSpaRoot("/pt/");
        assertReroutesToSpaRoot("/en/");
    }

    private static void assertContinues(String path) {
        var routing = routingFor(path);

        routing.handle();

        assertThat(routing.nextCount()).as(path).isOne();
        assertThat(routing.reroutes()).as(path).isEmpty();
    }

    private static void assertReroutesToSpaRoot(String path) {
        var routing = routingFor(path);

        routing.handle();

        assertThat(routing.nextCount()).as(path).isZero();
        assertThat(routing.reroutes()).as(path).containsExactly("/");
    }

    private static CapturedRouting routingFor(String path) {
        var routeHandler = new AtomicReference<Handler<RoutingContext>>();
        var route = proxy(Route.class, (proxy, method, arguments) -> {
            if (method.getName().equals("handler")) {
                @SuppressWarnings("unchecked")
                var handler = (Handler<RoutingContext>) arguments[0];
                routeHandler.set(handler);
            }
            return proxy;
        });
        var router = proxy(Router.class, (proxy, method, arguments) -> {
            if (method.getName().equals("get") && arguments != null && arguments.length == 1
                    && "/*".equals(arguments[0])) {
                return route;
            }
            return defaultValue(method.getReturnType());
        });
        new SPARouting().init(router);

        var nextCount = new AtomicInteger();
        var reroutes = new ArrayList<String>();
        var context = proxy(RoutingContext.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "normalizedPath" -> path;
            case "next" -> {
                nextCount.incrementAndGet();
                yield null;
            }
            case "reroute" -> {
                reroutes.add((String) arguments[0]);
                yield null;
            }
            default -> defaultValue(method.getReturnType());
        });
        return new CapturedRouting(routeHandler.get(), context, nextCount, reroutes);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private record CapturedRouting(Handler<RoutingContext> handler,
                                   RoutingContext context,
                                   AtomicInteger nextCounter,
                                   List<String> reroutes) {
        void handle() {
            handler.handle(context);
        }

        int nextCount() {
            return nextCounter.get();
        }
    }
}

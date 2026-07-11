package dev.vepo.issues.auth;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthRateLimiter {

    private final boolean enabled;
    private final int maxRequests;
    private final long windowMillis;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Inject
    public AuthRateLimiter(@ConfigProperty(name = "auth.rate-limit.enabled", defaultValue = "true") boolean enabled,
                           @ConfigProperty(name = "auth.rate-limit.requests", defaultValue = "30") int maxRequests,
                           @ConfigProperty(name = "auth.rate-limit.window-seconds", defaultValue = "60") int windowSeconds) {
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    public boolean tryAcquire(String key) {
        if (!enabled) {
            return true;
        }
        var now = Instant.now().toEpochMilli();
        var window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startedAt >= windowMillis) {
                return new Window(now, new AtomicInteger(0));
            }
            return existing;
        });
        return window.count.incrementAndGet() <= maxRequests;
    }

    void clear() {
        windows.clear();
    }

    private record Window(long startedAt, AtomicInteger count) {}
}

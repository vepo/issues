package dev.vepo.issues.notifications;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.SseEventSink;

@ApplicationScoped
public class NotificationChannelRegistry {

    private final Map<String, SseEventSink> openChannels = new HashMap<>();

    public void register(String username, SseEventSink eventSink) {
        openChannels.put(username, eventSink);
    }

    public SseEventSink get(String username) {
        return openChannels.get(username);
    }

    public void update(String username, SseEventSink sink) {
        if (sink == null) {
            openChannels.remove(username);
        } else {
            openChannels.put(username, sink);
        }
    }

    public void computeIfPresent(String username, java.util.function.BiFunction<String, SseEventSink, SseEventSink> remappingFunction) {
        openChannels.computeIfPresent(username, remappingFunction);
    }
}

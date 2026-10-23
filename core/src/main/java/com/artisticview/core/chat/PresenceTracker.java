package com.artisticview.core.chat;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who is online right now: the number of open WebSocket sessions per user (several tabs or devices count once).
 * In-memory, so with several server instances this becomes per-instance and should move to Redis.
 */
@Component
public class PresenceTracker {

    private final Map<Long, Integer> sessions = new ConcurrentHashMap<>();

    public void connected(Long userId) {
        sessions.merge(userId, 1, Integer::sum);
    }

    public void disconnected(Long userId) {
        sessions.computeIfPresent(userId, (id, n) -> n <= 1 ? null : n - 1);
    }

    public boolean isOnline(Long userId) {
        return sessions.containsKey(userId);
    }
}

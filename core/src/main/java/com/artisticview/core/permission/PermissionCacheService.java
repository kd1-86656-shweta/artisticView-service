package com.artisticview.core.permission;

import com.artisticview.persistence.rbac.PermissionRepository;
import com.artisticview.security.provider.PermissionProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Role -> privilege names, cached in Redis (one set per role), as agasty-service's PermissionCacheService does.
 * Warmed on startup, loaded lazily on a miss, and evicted whenever a role's permissions change.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService implements PermissionProvider {

    private static final String KEY_PREFIX = "av:permissions:role:";
    /** Stored for roles with no permissions so they are not reloaded on every request. */
    private static final String EMPTY_MARKER = "__none__";

    private final PermissionRepository permissionRepository;
    private final StringRedisTemplate redis;

    @EventListener(ApplicationReadyEvent.class)
    public void loadPermissionsIntoCache() {
        Map<Long, Set<String>> byRole = new HashMap<>();
        for (Object[] row : permissionRepository.findAllRolePermissionNames()) {
            byRole.computeIfAbsent((Long) row[0], k -> new HashSet<>()).add((String) row[1]);
        }
        try {
            byRole.forEach(this::store);
            log.info("Permission cache warmed for {} roles", byRole.size());
        } catch (RuntimeException e) {
            log.warn("Could not warm permission cache (Redis unavailable?): {}", e.getMessage());
        }
    }

    @Override
    public Set<String> getPermissionsForRole(Long roleId) {
        String key = KEY_PREFIX + roleId;
        try {
            Set<String> cached = redis.opsForSet().members(key);
            if (cached != null && !cached.isEmpty()) {
                Set<String> result = new HashSet<>(cached);
                result.remove(EMPTY_MARKER);
                return result;
            }
        } catch (RuntimeException e) {
            log.warn("Permission cache read failed, using database: {}", e.getMessage());
            return new HashSet<>(permissionRepository.findNamesForRole(roleId));
        }
        Set<String> fromDb = new HashSet<>(permissionRepository.findNamesForRole(roleId));
        try {
            store(roleId, fromDb);
        } catch (RuntimeException e) {
            log.warn("Permission cache write failed: {}", e.getMessage());
        }
        return fromDb;
    }

    public void evictRole(Long roleId) {
        redis.delete(KEY_PREFIX + roleId);
    }

    private void store(Long roleId, Set<String> names) {
        String key = KEY_PREFIX + roleId;
        redis.delete(key);
        List<String> values = names.isEmpty() ? List.of(EMPTY_MARKER) : List.copyOf(names);
        redis.opsForSet().add(key, values.toArray(String[]::new));
    }
}

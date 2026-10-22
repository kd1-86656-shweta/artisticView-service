package com.artisticview.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Fixed-window rate limit in Redis for the endpoints attackers hammer (login, sign-up, password reset).
 * Keyed by client IP so it works across all app instances.
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    /** Path -> max POST requests per IP per window. */
    private static final Map<String, Integer> LIMITS = Map.of(
            "/auth/login", 10,
            "/auth/sign-up", 5,
            "/auth/forgot-password", 3,
            "/auth/resend-verification", 3,
            "/auth/refresh-token", 30);

    private final StringRedisTemplate redis;
    private final JsonErrorWriter errorWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LIMITS.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String key = "av:rl:" + path + ":" + clientIp(request);
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, WINDOW);
            }
            if (count != null && count > LIMITS.get(path)) {
                response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
                errorWriter.write(response, 429, "RATE_LIMITED", "Too many attempts. Please wait a minute and try again.");
                return;
            }
        } catch (RuntimeException e) {
            // Fail open: a Redis outage must not lock everyone out of login.
            log.warn("Rate limiter unavailable: {}", e.getMessage());
        }
        chain.doFilter(request, response);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

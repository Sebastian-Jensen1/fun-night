package com.funnight.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-client limits on the two things a stranger could abuse: opening rooms (fills memory) and
 * trying join codes (guessing their way into someone's night). Gameplay itself is not limited.
 * <p>
 * The client is identified by {@code getRemoteAddr()}. Behind a reverse proxy, set
 * {@code server.forward-headers-strategy=native} so that is the real client and not the proxy.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MS = 60_000;
    private static final int MAX_TRACKED = 20_000;

    private record Window(long startedAt, AtomicInteger count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int roomsPerMinute;
    private final int joinsPerMinute;
    private final Clock clock;

    public RateLimitFilter(@Value("${funnight.rate-limit.rooms-per-minute:20}") int roomsPerMinute,
                           @Value("${funnight.rate-limit.joins-per-minute:60}") int joinsPerMinute,
                           Clock clock) {
        this.roomsPerMinute = roomsPerMinute;
        this.joinsPerMinute = joinsPerMinute;
        this.clock = clock;
    }

    /** Which limit applies to this request, or null when it isn't limited. */
    private String bucket(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        String method = request.getMethod();
        if ("POST".equals(method) && "/api/rooms".equals(path)) return "rooms";
        if ("POST".equals(method) && "/api/rooms/join".equals(path)) return "joins";
        if ("GET".equals(method) && "/api/rooms/lookup".equals(path)) return "joins";
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return bucket(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String bucket = bucket(request);
        int limit = "rooms".equals(bucket) ? roomsPerMinute : joinsPerMinute;
        long now = clock.millis();
        if (windows.size() > MAX_TRACKED) {
            windows.values().removeIf(w -> now - w.startedAt() >= WINDOW_MS);
            // Still full of live windows: someone is cycling addresses. Start over rather than grow without bound.
            if (windows.size() > MAX_TRACKED) windows.clear();
        }
        Window window = windows.compute(bucket + "|" + request.getRemoteAddr(), (key, w) ->
                w == null || now - w.startedAt() >= WINDOW_MS ? new Window(now, new AtomicInteger()) : w);
        if (window.count().incrementAndGet() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"status\":429,\"detail\":\"Slow down! Too many attempts, try again in a minute.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}

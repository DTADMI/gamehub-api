package ca.dtadmi.gamehubapi.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiter userRateLimiter;
    private final RateLimiter guestRateLimiter;
    private final List<String> excludedPaths = List.of("/actuator/health", "/error");

    public RateLimitFilter(RateLimiter userRateLimiter, RateLimiter guestRateLimiter) {
        this.userRateLimiter = userRateLimiter;
        this.guestRateLimiter = guestRateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        if (excludedPaths.stream().anyMatch(requestUri::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIP(request);

        RateLimiter limiter = selectLimiter();
        int limit = limiter.getRateLimiterConfig().getLimitForPeriod();

        boolean permission = limiter.acquirePermission();
        int remaining = Math.max(0, (int) limiter.getMetrics().getAvailablePermissions());
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (permission) {
            filterChain.doFilter(request, response);
        } else {
            rateLimitExceeded(response, clientIp, requestUri);
        }
    }

    private RateLimiter selectLimiter() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return userRateLimiter;
        }
        return guestRateLimiter;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void rateLimitExceeded(HttpServletResponse response, String clientIp, String requestUri) throws IOException {
        logger.warn("Rate limit exceeded for {} - {}", clientIp, requestUri);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(Duration.ofMinutes(1).toSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"error":"Too many requests","message":"Rate limit exceeded. Please try again later."}
                """);
    }

    // Let downstream exceptions propagate to Spring's exception handlers; avoid converting to 500 here.
}
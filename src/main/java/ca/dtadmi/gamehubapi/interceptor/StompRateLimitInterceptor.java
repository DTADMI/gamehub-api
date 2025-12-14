package ca.dtadmi.gamehubapi.interceptor;

import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Simple STOMP rate limiter using Redis counters (INCR + EXPIRE) per identity.
 * - Keys are segmented by minute window.
 * - Separate limits for authenticated users and guests.
 * - If Redis is unavailable, it degrades gracefully (no rate limit enforced).
 */
public class StompRateLimitInterceptor implements ChannelInterceptor {

    private final StringRedisTemplate redis;
    private final int userLimitPerMin;
    private final int guestLimitPerMin;

    public StompRateLimitInterceptor(StringRedisTemplate redis, Environment env) {
        this.redis = redis;
        this.userLimitPerMin = env.getProperty("stomp.ratelimit.user.perMinute", Integer.class, 300);
        this.guestLimitPerMin = env.getProperty("stomp.ratelimit.guest.perMinute", Integer.class, 120);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        // Apply only to SEND frames; allow CONNECT/SUBSCRIBE/UNSUBSCRIBE freely
        if (!StompCommand.SEND.equals(accessor.getCommand())) {
            return message;
        }

        String id = resolveIdentity(accessor);
        boolean authenticated = accessor.getUser() != null && StringUtils.hasText(accessor.getUser().getName());
        int limit = authenticated ? userLimitPerMin : guestLimitPerMin;

        try {
            String key = buildKey(id);
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                // First hit in this window: set expiry to remaining seconds in minute
                long ttl = secondsToWindowEnd();
                redis.expire(key, Duration.ofSeconds(Math.max(1, ttl)));
            }
            if (count != null && count > limit) {
                // Drop the message by returning null (frame not forwarded to handlers)
                return null;
            }
        } catch (Exception ignored) {
            // Redis unavailable → fail open (no throttling) to avoid breaking gameplay
        }
        return message;
    }

    private String resolveIdentity(StompHeaderAccessor accessor) {
        Principal p = accessor.getUser();
        if (p != null && StringUtils.hasText(p.getName())) {
            return "user:" + p.getName();
        }
        String sessionId = accessor.getSessionId();
        if (StringUtils.hasText(sessionId)) {
            return "sess:" + sessionId;
        }
        // Try x-forwarded-for / native ip header if provided by proxy
        String ip = accessor.getFirstNativeHeader("x-forwarded-for");
        if (!StringUtils.hasText(ip)) {
            ip = accessor.getFirstNativeHeader("X-Forwarded-For");
        }
        if (StringUtils.hasText(ip)) {
            return "ip:" + ip.split(",")[0].trim();
        }
        return "anon:unknown";
    }

    private String buildKey(String identity) {
        long minute = System.currentTimeMillis() / TimeUnit.MINUTES.toMillis(1);
        return "stomp:rate:" + minute + ":" + identity;
    }

    private long secondsToWindowEnd() {
        long nowMs = System.currentTimeMillis();
        long nextMinute = ((nowMs / 60_000) + 1) * 60_000;
        return (nextMinute - nowMs) / 1000;
    }
}

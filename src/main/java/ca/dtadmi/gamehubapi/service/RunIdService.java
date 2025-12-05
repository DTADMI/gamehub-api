package ca.dtadmi.gamehubapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class RunIdService {

    private final StringRedisTemplate redis;

    @Value("${anticheat.run.ttl.seconds:600}")
    private long runTtlSeconds;

    public RunIdService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String runId) {
        return "run:snake:" + runId;
    }

    /**
     * Creates a new runId for a client to use while playing. The runId is stored with TTL.
     */
    public String start(String userOrGuest) {
        String runId = UUID.randomUUID().toString();
        String value = userOrGuest == null ? "guest" : userOrGuest;
        redis.opsForValue().set(key(runId), value, Duration.ofSeconds(runTtlSeconds));
        return runId;
    }

    /**
     * Validates the runId exists (not expired) and consumes it (one time use). Returns true if accepted.
     */
    public boolean validateAndConsume(String runId) {
        if (runId == null || runId.isBlank()) return false;
        String k = key(runId);
        String exists = redis.opsForValue().get(k);
        if (exists == null) return false;
        redis.delete(k);
        return true;
    }

    public Optional<String> peek(String runId) {
        String v = redis.opsForValue().get(key(runId));
        return Optional.ofNullable(v);
    }
}

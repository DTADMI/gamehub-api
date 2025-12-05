package ca.dtadmi.gamehubapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class PresenceService {

    private final StringRedisTemplate redis;

    @Value("${presence.ttl.seconds:45}")
    private long ttlSeconds;

    public PresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String keyMember(String roomId, String memberId) {
        return "presence:" + roomId + ":member:" + memberId;
    }

    private String keyNamespace(String roomId) {
        return "presence:" + roomId + ":member:*";
    }

    public void join(String roomId, String memberId) {
        heartbeat(roomId, memberId);
    }

    public void leave(String roomId, String memberId) {
        try {
            redis.delete(keyMember(roomId, memberId));
        } catch (Exception ignored) {
        }
    }

    public void heartbeat(String roomId, String memberId) {
        try {
            String key = keyMember(roomId, memberId);
            redis.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
        } catch (Exception ignored) {
        }
    }

    public int count(String roomId) {
        try {
            Set<String> keys = redis.keys(keyNamespace(roomId));
            return keys == null ? 0 : keys.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public List<String> sample(String roomId, int limit) {
        List<String> result = new ArrayList<>();
        try {
            Set<String> keys = redis.keys(keyNamespace(roomId));
            if (keys == null) return result;
            int i = 0;
            for (String k : keys) {
                if (i++ >= limit) break;
                result.add(k.substring(k.lastIndexOf(':') + 1));
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}

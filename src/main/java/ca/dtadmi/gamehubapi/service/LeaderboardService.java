package ca.dtadmi.gamehubapi.service;

import ca.dtadmi.gamehubapi.realtime.dto.RealtimeDtos.Entry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class LeaderboardService {

    private final StringRedisTemplate redis;

    public LeaderboardService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String gameScope) {
        // e.g. lb:snake:global
        return "lb:" + gameScope;
    }

    public int submit(String gameScope, String nickname, int score) {
        String k = key(gameScope);
        // store per-user best: zadd with score, member nickname, but keep max
        Double existing = redis.opsForZSet().score(k, nickname);
        if (existing == null || score > existing.intValue()) {
            redis.opsForZSet().add(k, nickname, score);
        }
        Double after = redis.opsForZSet().score(k, nickname);
        return after == null ? score : after.intValue();
    }

    public List<Entry> topN(String gameScope, int n) {
        String k = key(gameScope);
        Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet().reverseRangeWithScores(k, 0, n - 1);
        List<Entry> result = new ArrayList<>();
        if (tuples == null) return result;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            if (t.getValue() == null || t.getScore() == null) continue;
            Entry e = new Entry();
            e.nickname = t.getValue();
            e.value = t.getScore().intValue();
            result.add(e);
        }
        return result;
    }

    public Integer rankOf(String gameScope, String nickname) {
        Long rank = redis.opsForZSet().reverseRank(key(gameScope), nickname);
        if (rank == null) return null;
        return rank.intValue() + 1;
    }
}

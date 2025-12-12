// src/main/java/ca/dtadmi/gamehubapi/service/GameService.java
package ca.dtadmi.gamehubapi.service;

import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.GameScoreRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameService {
    private static final int LEADERBOARD_SIZE = 10;
    private static final int USER_SCORES_LIMIT = 5;

    private final GameScoreRepository gameScoreRepository;

    public GameService(GameScoreRepository gameScoreRepository) {
        this.gameScoreRepository = gameScoreRepository;
    }

    @Transactional
    @CacheEvict(value = {"leaderboard", "userScores"}, allEntries = true)
    public GameScore saveScore(User user, String gameType, int score) {
        GameScore gameScore = new GameScore();
        gameScore.setUser(user);
        gameScore.setGameType(gameType);
        gameScore.setScore(score);
        return gameScoreRepository.save(gameScore);
    }

    @Cacheable(value = "leaderboard", key = "#gameType")
    public List<GameScore> getLeaderboard(String gameType) {
        return getLeaderboard(gameType, LEADERBOARD_SIZE);
    }

    public List<GameScore> getLeaderboard(String gameType, Integer limit) {
        int size = (limit == null || limit <= 0) ? LEADERBOARD_SIZE : Math.min(limit, 100);
        Pageable pageable = PageRequest.of(0, size, Sort.by("score").descending().and(Sort.by("createdAt").ascending()));
        return gameScoreRepository.findTopScoresByGameType(gameType, pageable);
    }

    // Legacy aggregate endpoint support
    @Cacheable(value = "leaderboard", key = "'ALL'")
    public Map<String, List<GameScore>> getLeaderboard() {
        Map<String, List<GameScore>> map = new LinkedHashMap<>();
        for (String game : List.of("snake", "memory", "breakout", "tetris")) {
            map.put(game, getLeaderboard(game));
        }
        return map;
    }

    @Cacheable(value = "userScores", key = "#userId + '_' + #gameType")
    public List<GameScore> getUserScores(Long userId, String gameType) {
        return getUserScores(userId, gameType, USER_SCORES_LIMIT);
    }

    public List<GameScore> getUserScores(Long userId, String gameType, Integer limit) {
        int size = (limit == null || limit <= 0) ? USER_SCORES_LIMIT : Math.min(limit, 100);
        Pageable pageable = PageRequest.of(0, size, Sort.by("score").descending().and(Sort.by("createdAt").ascending()));
        return gameScoreRepository.findUserScores(userId, gameType, pageable);
    }

    public List<GameScore> recentScores(String gameType, Integer limit) {
        return getLeaderboard(gameType, limit);
    }

    // Legacy aggregate endpoint support
    @Cacheable(value = "userScores", key = "#userId + '_ALL'")
    public Map<String, List<GameScore>> getUserScores(Long userId) {
        Map<String, List<GameScore>> map = new LinkedHashMap<>();
        for (String game : List.of("snake", "memory", "breakout", "tetris")) {
            map.put(game, getUserScores(userId, game));
        }
        return map;
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @CacheEvict(value = {"leaderboard", "userScores"}, allEntries = true)
    public void clearCache() {
        // Cache cleared by annotation
    }

    public boolean isFeatureEnabled(String featureName) {
        // FF4J removed; default to true for now (feature flags can be re-added later)
        return true;
    }
}

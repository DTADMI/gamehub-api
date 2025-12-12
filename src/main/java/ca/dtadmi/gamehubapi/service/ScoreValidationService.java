package ca.dtadmi.gamehubapi.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ScoreValidationService {

    // Temporary common caps per game; can be externalized later
    private static final Map<String, Integer> CAPS = Map.of(
            "snake", 1_000_000,
            "tetris", 2_000_000,
            "breakout", 1_000_000,
            "memory", 100_000,
            "bubble_pop", 500_000,
            "checkers", 100_000,
            "chess", 100_000,
            "platformer", 1_000_000,
            "tower_defense", 2_000_000,
            "knitzy", 500_000
    );

    public void validateOrThrow(String gameType, int score) {
        if (gameType == null || gameType.isBlank()) {
            throw new IllegalArgumentException("gameType is required");
        }
        if (score < 0) {
            throw new IllegalArgumentException("score must be >= 0");
        }
        int cap = CAPS.getOrDefault(gameType.toLowerCase(), 1_000_000);
        if (score > cap) {
            throw new IllegalArgumentException("score exceeds allowed maximum for " + gameType + ": " + cap);
        }
    }
}

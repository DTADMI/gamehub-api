package ca.dtadmi.gamehubapi.controller;

import java.util.Map;

/**
 * DTO used by integration tests and REST endpoint to submit a score.
 */
public record ScoreRequest(
        String gameType,
        int score,
        Map<String, Object> metadata
) {
}

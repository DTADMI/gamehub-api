package ca.dtadmi.gamehubapi.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO used by integration tests and REST endpoint to submit a score.
 */
public record ScoreRequest(
        @NotBlank(message = "gameType is required") String gameType,
        @Min(value = 0, message = "score must be >= 0") int score,
        Map<String, Object> metadata
) {
}

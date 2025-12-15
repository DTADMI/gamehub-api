package ca.dtadmi.gamehubapi.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO used by integration tests and REST endpoint to submit a score.
 * Converted from record to classic POJO for broad Jackson compatibility.
 */
public class ScoreRequest {

    @NotBlank(message = "gameType is required")
    private String gameType;

    @Min(value = 0, message = "score must be >= 0")
    private int score;

    private Map<String, Object> metadata;

    public ScoreRequest() {
    }

    public ScoreRequest(String gameType, int score, Map<String, Object> metadata) {
        this.gameType = gameType;
        this.score = score;
        this.metadata = metadata;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

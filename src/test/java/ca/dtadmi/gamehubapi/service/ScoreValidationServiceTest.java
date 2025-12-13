package ca.dtadmi.gamehubapi.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreValidationServiceTest {

    private final ScoreValidationService service = new ScoreValidationService();

    @Test
    @DisplayName("valid score within cap passes validation")
    void validateOrThrow_valid() {
        assertDoesNotThrow(() -> service.validateOrThrow("snake", 12345));
        assertDoesNotThrow(() -> service.validateOrThrow("tetris", 999_999));
    }

    @Test
    @DisplayName("negative score is rejected")
    void validateOrThrow_negative() {
        assertThrows(IllegalArgumentException.class, () -> service.validateOrThrow("snake", -1));
    }

    @Test
    @DisplayName("missing gameType is rejected")
    void validateOrThrow_missingGame() {
        assertThrows(IllegalArgumentException.class, () -> service.validateOrThrow(null, 10));
        assertThrows(IllegalArgumentException.class, () -> service.validateOrThrow(" ", 10));
    }

    @Test
    @DisplayName("score over per-game cap is rejected")
    void validateOrThrow_overCap() {
        // snake cap = 1_000_000 per current defaults
        assertThrows(IllegalArgumentException.class, () -> service.validateOrThrow("snake", 1_000_001));
    }
}

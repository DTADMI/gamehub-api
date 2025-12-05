package ca.dtadmi.gamehubapi.service;

import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.GameScoreRepository;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameScoreRepository gameScoreRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameService gameService;

    @Test
    void saveScore_ValidInput_ReturnsSavedScore() {
        // Given
        String username = "testuser";
        String gameType = "snake";
        int score = 100;

        User user = new User();
        user.setUsername(username);
        user.setEmail("test@example.com");

        GameScore savedScore = new GameScore();
        savedScore.setId(1L);
        savedScore.setUser(user);
        savedScore.setGameType(gameType);
        savedScore.setScore(score);

        when(gameScoreRepository.save(any(GameScore.class))).thenReturn(savedScore);

        // When
        GameScore result = gameService.saveScore(user, gameType, score);

        // Then
        assertNotNull(result);
        assertEquals(score, result.getScore());
        assertEquals(gameType, result.getGameType());
        assertEquals(user, result.getUser());

        verify(gameScoreRepository).save(any(GameScore.class));
    }
}

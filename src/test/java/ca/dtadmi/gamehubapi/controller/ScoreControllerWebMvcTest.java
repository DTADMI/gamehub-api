package ca.dtadmi.gamehubapi.controller;

import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import ca.dtadmi.gamehubapi.service.GameService;
import ca.dtadmi.gamehubapi.service.ScoreValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ScoreControllerWebMvcTest {

    private MockMvc mockMvc;

    private GameService gameService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private ScoreValidationService scoreValidationService;

    @BeforeEach
    void setup() {
        gameService = Mockito.mock(GameService.class);
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        scoreValidationService = Mockito.mock(ScoreValidationService.class);

        ScoreController controller = new ScoreController(gameService, userRepository, passwordEncoder, scoreValidationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthArgumentResolver())
                .setControllerAdvice() // none for now
                .build();
    }

    private User demoUser() {
        User u = new User();
        u.setId(1L);
        u.setUsername("user");
        u.setEmail("user@example.com");
        return u;
    }

    private GameScore score(User u, String game, int s) {
        GameScore gs = new GameScore();
        gs.setUser(u);
        gs.setGameType(game);
        gs.setScore(s);
        return gs;
    }

    @Test
    @DisplayName("POST /api/scores with valid payload returns 201")
    void postScore_valid_returns201() throws Exception {
        User u = demoUser();
        given(userRepository.findByUsername("user")).willReturn(java.util.Optional.of(u));
        doNothing().when(scoreValidationService).validateOrThrow(eq("snake"), eq(100));
        given(gameService.saveScore(eq(u), eq("snake"), eq(100))).willReturn(score(u, "snake", 100));

        String json = "{\"gameType\":\"snake\",\"score\":100}";

        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.gameType").value("snake"))
                .andExpect(jsonPath("$.score").value(100));
    }

    @Test
    @DisplayName("POST /api/scores with invalid score returns 400")
    void postScore_invalid_returns400() throws Exception {
        User u = demoUser();
        given(userRepository.findByUsername("user")).willReturn(java.util.Optional.of(u));
        // ScoreValidationService will throw IllegalArgumentException
        org.mockito.Mockito.doThrow(new IllegalArgumentException("score must be >= 0"))
                .when(scoreValidationService).validateOrThrow(eq("snake"), eq(-5));

        String json = "{\"gameType\":\"snake\",\"score\":-5}";

        mockMvc.perform(post("/api/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/scores returns 200 with list")
    void getScores_returns200() throws Exception {
        User u = demoUser();
        given(gameService.recentScores(eq("snake"), isNull())).willReturn(List.of(
                score(u, "snake", 10),
                score(u, "snake", 20)
        ));

        mockMvc.perform(get("/api/scores").param("gameType", "snake"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].gameType").value("snake"));
    }

    @Test
    @DisplayName("GET /api/scores/leaderboard returns 200 with map of lists")
    void getLeaderboard_returns200() throws Exception {
        User u = demoUser();
        Map<String, List<GameScore>> lb = Map.of(
                "snake", List.of(score(u, "snake", 100))
        );
        given(gameService.getLeaderboard()).willReturn(lb);

        mockMvc.perform(get("/api/scores/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.snake").isArray());
    }

    // Simple argument resolver to inject an Authentication for controller method parameters
    static class AuthArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return org.springframework.security.core.Authentication.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return new TestingAuthenticationToken("user", "password");
        }
    }
}

package ca.dtadmi.gamehubapi.controller;

import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(JwtTokenProvider.class)
class GameControllerIT {

    private static final boolean useDocker = Boolean.parseBoolean(System.getenv().getOrDefault("ENABLE_DOCKER_TESTS", "false"));

    @Container
    private static final PostgreSQLContainer<?> postgres = useDocker ?
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test") : null;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JwtTokenProvider tokenProvider;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (useDocker && postgres != null) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @Test
    void submitScore_AuthenticatedUser_ReturnsCreated() {
        // Given
        String token = createTestToken("testuser");
        ScoreRequest request = new ScoreRequest("snake", 100, null);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<ScoreRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<GameScore> response = restTemplate.exchange(
                "/api/scores",
                HttpMethod.POST,
                entity,
                GameScore.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100, response.getBody().getScore());
    }

    private String createTestToken(String username) {
        UserDetails userDetails = User.withUsername(username)
                .password("password")
                .roles("USER")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        return tokenProvider.generateToken(authentication);
    }
}

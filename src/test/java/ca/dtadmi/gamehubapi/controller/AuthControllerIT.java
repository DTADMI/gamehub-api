package ca.dtadmi.gamehubapi.controller;

import ca.dtadmi.gamehubapi.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.jwtSecret=test-secret",
        "app.jwtExpirationInMs=3600000"
})
class AuthControllerIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void signup_login_me_refresh_happyPath() {
        // Signup
        var signupBody = Map.of(
                "email", "it+auth@example.com",
                "username", "itauth",
                "password", "P@ssw0rd!"
        );
        ResponseEntity<Map> signupRes = rest.postForEntity("/api/auth/signup", signupBody, Map.class);
        assertThat(signupRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        String accessToken = (String) signupRes.getBody().get("accessToken");
        String refreshToken = (String) signupRes.getBody().get("refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // Login
        var loginBody = Map.of(
                "email", "it+auth@example.com",
                "password", "P@ssw0rd!"
        );
        ResponseEntity<Map> loginRes = rest.postForEntity("/api/auth/login", loginBody, Map.class);
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        String loginAccess = (String) loginRes.getBody().get("accessToken");
        assertThat(loginAccess).isNotBlank();

        // Me
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginAccess);
        ResponseEntity<Map> meRes = rest.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(meRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meRes.getBody().get("email")).isEqualTo("it+auth@example.com");

        // Refresh
        ResponseEntity<Map> refreshRes = rest.postForEntity("/api/auth/refresh", Map.of("refreshToken", refreshToken), Map.class);
        assertThat(refreshRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newAccess = (String) refreshRes.getBody().get("accessToken");
        assertThat(newAccess).isNotBlank();

        // Me with new access
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(newAccess);
        ResponseEntity<Map> meRes2 = rest.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers2), Map.class);
        assertThat(meRes2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meRes2.getBody().get("email")).isEqualTo("it+auth@example.com");
    }

    @Test
    void duplicateEmail_returnsBadRequest() {
        var signup = Map.of("email", "dup@example.com", "username", "dup1", "password", "pass");
        rest.postForEntity("/api/auth/signup", signup, Map.class);
        ResponseEntity<String> dup = rest.postForEntity("/api/auth/signup", Map.of(
                "email", "dup@example.com", "username", "dup2", "password", "pass"), String.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void badCredentials_returnsUnauthorized() {
        ResponseEntity<String> res = rest.postForEntity("/api/auth/login", Map.of(
                "email", "nobody@example.com",
                "password", "nope"
        ), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

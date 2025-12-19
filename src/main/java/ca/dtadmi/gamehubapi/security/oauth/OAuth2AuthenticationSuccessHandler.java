package ca.dtadmi.gamehubapi.security.oauth;

import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import ca.dtadmi.gamehubapi.security.JwtTokenProvider;
import ca.dtadmi.gamehubapi.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    private final Set<String> allowedRedirectUris;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            JwtTokenProvider tokenProvider,
            RefreshTokenService refreshTokenService,
            @Value("${app.oauth2.allowedRedirectUris:http://localhost:3000/oauth2/redirect}") String allowedUris
    ) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.allowedRedirectUris = new HashSet<>(Arrays.asList(allowedUris.split(",")));
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid authentication type");
            return;
        }

        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = (OAuth2User) oauthToken.getPrincipal();

        String email = CustomOAuth2UserService.extractEmail(registrationId, oAuth2User.getAttributes());
        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not available from provider");
            return;
        }

        String name = CustomOAuth2UserService.extractName(registrationId, oAuth2User.getAttributes());

        // Upsert user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setUsername(name != null ? name : email);
            // Social users do not have local password; store a sentinel value
            u.setPassword("{noop}oauth2");
            u.getRoles().add("ROLE_USER");
            return u;
        });
        if (user.getId() == null) {
            user = userRepository.save(user);
        }

        String accessToken = tokenProvider.generateTokenForSubject(email);
        String refreshToken = refreshTokenService.issue(user).getToken();

        String target = resolveRedirectTarget(request);
        if (target == null) {
            // fallback to the first allowed URI
            target = allowedRedirectUris.iterator().next();
        }

        String url = target + "?accessToken=" + urlEnc(accessToken) +
                "&refreshToken=" + urlEnc(refreshToken) +
                "&tokenType=Bearer";
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);
    }

    private String resolveRedirectTarget(HttpServletRequest request) {
        String redirectUri = request.getParameter("redirect_uri");
        if (redirectUri == null || redirectUri.isBlank()) return null;
        String sanitized = redirectUri.trim();
        Optional<String> allowed = allowedRedirectUris.stream()
                .filter(sanitized::equalsIgnoreCase)
                .findFirst();
        return allowed.orElse(null);
    }

    private String urlEnc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}

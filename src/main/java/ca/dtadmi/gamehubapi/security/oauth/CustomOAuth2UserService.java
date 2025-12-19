package ca.dtadmi.gamehubapi.security.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    public static String extractEmail(String registrationId, Map<String, Object> attributes) {
        // Common providers
        Object email = attributes.get("email");
        if (email instanceof String s && !s.isBlank()) return s;
        if ("github".equalsIgnoreCase(registrationId)) {
            // GitHub may not return public email without scope; attempt fallback keys
            Object username = attributes.get("login");
            if (username != null) {
                // As a last resort, synthesize a placeholder email; caller may reject if policy requires real email
                return username + "@users.noreply.github.com";
            }
        }
        return null;
    }

    public static String extractName(String registrationId, Map<String, Object> attributes) {
        Object name = attributes.get("name");
        if (name instanceof String s && !s.isBlank()) return s;
        if ("google".equalsIgnoreCase(registrationId)) {
            Object given = attributes.get("given_name");
            if (given instanceof String s && !s.isBlank()) return s;
        }
        if ("github".equalsIgnoreCase(registrationId)) {
            Object login = attributes.get("login");
            if (login instanceof String s && !s.isBlank()) return s;
        }
        return "User";
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        // No transformation yet; handlers/services will read standard attributes from this user
        return oAuth2User;
    }
}

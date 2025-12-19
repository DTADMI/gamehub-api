package ca.dtadmi.gamehubapi.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String defaultRedirect;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.failureRedirect:http://localhost:3000/oauth2/error}") String defaultRedirect
    ) {
        this.defaultRedirect = defaultRedirect;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String target = request.getParameter("redirect_uri");
        if (target == null || target.isBlank()) target = defaultRedirect;
        String url = target + "?error=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);
    }
}

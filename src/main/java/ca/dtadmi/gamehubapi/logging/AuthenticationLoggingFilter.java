package ca.dtadmi.gamehubapi.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enrich MDC with authentication context for better log correlation.
 * Adds keys: userId (when available) and email (when available). Avoids other PII.
 */
public class AuthenticationLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = null;
        String userId = null;
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof ca.dtadmi.gamehubapi.security.UserPrincipal up) {
                userId = up.getId() != null ? String.valueOf(up.getId()) : null;
                email = up.getEmail();
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                email = ud.getUsername();
            } else if (principal instanceof String s) {
                email = s;
            }
        }

        if (userId != null) MDC.put("userId", userId);
        if (email != null) MDC.put("email", email);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
            MDC.remove("email");
        }
    }
}
package ca.dtadmi.gamehubapi.graphql;

import ca.dtadmi.gamehubapi.graphql.types.GameType;
import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import ca.dtadmi.gamehubapi.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class MutationResolver {
    private final GameService gameService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public GameScore submitScore(@Argument ScoreInput input) {
        if (input == null || input.gameType() == null || input.score() == null) {
            throw new IllegalArgumentException("Invalid score input");
        }
        User user = resolveCurrentOrGuestUser();
        return gameService.saveScore(user, input.gameType().toSlug(), input.score());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public User updateUserProfile(@Argument UserProfileInput input) {
        User user = resolveCurrentOrGuestUser();
        if (input != null && input.username() != null && !input.username().isBlank()) {
            // Update username if it's different and not taken
            String newName = input.username().trim();
            if (!newName.equals(user.getUsername())) {
                if (userRepository.existsByUsername(newName)) {
                    throw new IllegalArgumentException("Username already taken");
                }
                user.setUsername(newName);
                // Keep email as-is for MVP
            }
        }
        // Avatar is ignored in MVP as User entity doesn't store it yet
        return userRepository.save(user);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public CheckoutSession createCheckout(@Argument CreateCheckoutInput input) {
        // Stub: return a fake checkout URL
        String id = UUID.randomUUID().toString();
        String url = input.returnUrl() + "?session_id=" + id;
        return new CheckoutSession(id, url);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Subscription cancelSubscription() {
        User user = resolveCurrentOrGuestUser();
        return new Subscription(UUID.randomUUID().toString(), String.valueOf(user.getId()), Plan.FREE, "canceled", OffsetDateTime.now().plusDays(0).toString());
    }

    private User resolveCurrentOrGuestUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? String.valueOf(auth.getName()) : "guest";
        if (username == null || username.isBlank()) username = "guest";
        String uname = username;
        Optional<User> existing = userRepository.findByUsername(uname);
        if (existing.isPresent()) return existing.get();
        // Provision minimal guest user
        User u = new User();
        u.setUsername(uname);
        u.setEmail(uname + "@local.test");
        u.setPassword(passwordEncoder.encode("test"));
        u.getRoles().add("ROLE_USER");
        return userRepository.save(u);
    }

    public enum Plan {FREE, PRO}

    public record ScoreInput(GameType gameType, Integer score) {
    }

    public record UserProfileInput(String username, String avatar) {
    }

    public record CreateCheckoutInput(Plan plan, String returnUrl, String cancelUrl) {
    }

    public record CheckoutSession(String id, String url) {
    }

    public record Subscription(String id, String userId, Plan plan, String status, String currentPeriodEnd) {
    }
}

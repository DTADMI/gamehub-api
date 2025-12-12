package ca.dtadmi.gamehubapi.graphql;

import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import ca.dtadmi.gamehubapi.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MutationResolver {
    private final GameService gameService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @MutationMapping
    public GameScore submitScore(@Argument ScoreInput input) {
        if (input == null || input.gameType() == null || input.gameType().isBlank() || input.score() == null) {
            throw new IllegalArgumentException("Invalid score input");
        }
        User user = resolveCurrentOrGuestUser();
        return gameService.saveScore(user, input.gameType(), input.score());
    }

    @MutationMapping
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

    public record ScoreInput(String gameType, Integer score) {
    }

    public record UserProfileInput(String username, String avatar) {
    }
}

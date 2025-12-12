// src/main/java/ca/dtadmi/gamehubapi/config/DevDataSeeder.java
package ca.dtadmi.gamehubapi.config;

import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.GameScoreRepository;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@Profile("dev")
public class DevDataSeeder {

    @Bean
    ApplicationRunner seedDevData(UserRepository users,
                                  GameScoreRepository scores,
                                  PasswordEncoder encoder) {
        return args -> {
            // Idempotency: if there are already some scores, skip heavy seeding
            if (scores.count() > 0) {
                return;
            }

            // Create demo users if missing
            User alice = users.findByUsername("alice").orElseGet(() -> {
                User u = new User();
                u.setUsername("alice");
                u.setEmail("alice@example.com");
                u.setPassword(encoder.encode("password"));
                u.getRoles().add("ROLE_USER");
                return users.save(u);
            });

            User bob = users.findByUsername("bob").orElseGet(() -> {
                User u = new User();
                u.setUsername("bob");
                u.setEmail("bob@example.com");
                u.setPassword(encoder.encode("password"));
                u.getRoles().add("ROLE_USER");
                return users.save(u);
            });

            User chloe = users.findByUsername("chloe").orElseGet(() -> {
                User u = new User();
                u.setUsername("chloe");
                u.setEmail("chloe@example.com");
                u.setPassword(encoder.encode("password"));
                u.getRoles().add("ROLE_USER");
                return users.save(u);
            });

            // Helper to create a score record
            java.util.function.BiConsumer<User, Integer> snake = (user, s) -> {
                GameScore gs = new GameScore();
                gs.setUser(user);
                gs.setGameType("snake");
                gs.setScore(s);
                scores.save(gs);
            };

            java.util.function.BiConsumer<User, Integer> tetris = (user, s) -> {
                GameScore gs = new GameScore();
                gs.setUser(user);
                gs.setGameType("tetris");
                gs.setScore(s);
                scores.save(gs);
            };

            java.util.function.BiConsumer<User, Integer> breakout = (user, s) -> {
                GameScore gs = new GameScore();
                gs.setUser(user);
                gs.setGameType("breakout");
                gs.setScore(s);
                scores.save(gs);
            };

            java.util.function.BiConsumer<User, Integer> memory = (user, s) -> {
                GameScore gs = new GameScore();
                gs.setUser(user);
                gs.setGameType("memory");
                gs.setScore(s);
                scores.save(gs);
            };

            // Seed a few records per game for leaderboards and user views
            List.of(100, 220, 340, 480).forEach(s -> snake.accept(alice, s));
            List.of(150, 260, 390).forEach(s -> snake.accept(bob, s));
            List.of(90, 305).forEach(s -> snake.accept(chloe, s));

            List.of(1200, 800, 600).forEach(s -> tetris.accept(alice, s));
            List.of(900, 700).forEach(s -> tetris.accept(bob, s));
            List.of(500).forEach(s -> tetris.accept(chloe, s));

            List.of(20, 45, 60).forEach(s -> breakout.accept(alice, s));
            List.of(30, 55).forEach(s -> breakout.accept(bob, s));
            List.of(15, 25).forEach(s -> breakout.accept(chloe, s));

            List.of(8, 10, 12).forEach(s -> memory.accept(alice, s));
            List.of(6, 9).forEach(s -> memory.accept(bob, s));
            List.of(7).forEach(s -> memory.accept(chloe, s));
        };
    }
}

package ca.dtadmi.gamehubapi.config;

import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class AdminRoleSeeder {
    private static final Logger log = LoggerFactory.getLogger(AdminRoleSeeder.class);

    @Value("${app.admin.emails:}")
    private String adminEmailsCsv;

    @Bean
    ApplicationRunner seedAdmins(UserRepository users) {
        return args -> {
            if (adminEmailsCsv == null || adminEmailsCsv.isBlank()) {
                return; // nothing to seed
            }
            Set<String> emails = Arrays.stream(adminEmailsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            if (emails.isEmpty()) return;

            for (String email : emails) {
                users.findByEmail(email).ifPresent(u -> ensureAdminRole(users, u));
            }
        };
    }

    private void ensureAdminRole(UserRepository users, User u) {
        if (!u.getRoles().contains("ROLE_ADMIN")) {
            u.getRoles().add("ROLE_ADMIN");
            users.save(u);
            log.info("Granted ROLE_ADMIN to {}", u.getEmail());
        }
    }
}

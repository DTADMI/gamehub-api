package ca.dtadmi.gamehubapi.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads variables from .env and .env.local (if present) into Spring's Environment
 * before the application context refreshes.
 * <p>
 * Precedence:
 * - .env.local overrides .env
 * - Real OS environment variables and JVM system properties still take precedence
 * over .env values (because those property sources are ordered before ours).
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, org.springframework.boot.SpringApplication application) {
        // Load from working directory. Ignore if files are missing.
        Map<String, Object> baseEnv = loadFile(".env");
        Map<String, Object> localEnv = loadFile(".env.local");

        if (!baseEnv.isEmpty()) {
            PropertySource<?> dotenv = new MapPropertySource("dotenv", baseEnv);
            // Place at the beginning so it overrides application.yml defaults
            environment.getPropertySources().addFirst(dotenv);
        }
        if (!localEnv.isEmpty()) {
            PropertySource<?> dotenvLocal = new MapPropertySource("dotenvLocal", localEnv);
            // Ensure .env.local has higher precedence than .env
            environment.getPropertySources().addFirst(dotenvLocal);
        }
    }

    private Map<String, Object> loadFile(String filename) {
        Map<String, Object> map = new HashMap<>();
        try {
            Dotenv d = Dotenv.configure()
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .filename(filename)
                    .load();
            for (DotenvEntry e : d.entries()) {
                String key = e.getKey();
                String val = stripQuotes(e.getValue());
                if (key != null && !key.isBlank() && val != null) {
                    map.put(key, val);
                }
            }
        } catch (Throwable ignored) {
            // If dotenv cannot load (e.g., running from a restricted environment), just skip.
        }
        return map;
    }

    private String stripQuotes(String v) {
        if (v == null) return null;
        String s = v.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    @Override
    public int getOrder() {
        // Run after system properties/env are added (default is LOWEST_PRECEDENCE),
        // but before configuration files so .env can override YAML defaults.
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}

package ca.dtadmi.gamehubapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    // Default to '*' for MVP/local if not provided; override via env/property in prod
    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = parseOrigins(allowedOrigins);
                boolean hasWildcard = false;
                for (String o : origins) {
                    if ("*".equals(o)) {
                        hasWildcard = true;
                        break;
                    }
                }

                var mapping = registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600L);

                if (hasWildcard) {
                    // When credentials are allowed, Spring forbids allowedOrigins("*").
                    // Use allowedOriginPatterns("*") so the actual Origin gets reflected.
                    mapping.allowedOriginPatterns("*")
                            .allowCredentials(true);
                } else {
                    mapping.allowedOrigins(origins)
                            .allowCredentials(true);
                }
            }
        };
    }

    private String[] parseOrigins(String csv) {
        if (csv == null || csv.isBlank()) return new String[0];
        String[] parts = csv.split(",");
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
                s = s.substring(1, s.length() - 1);
            }
            parts[i] = s;
        }
        return parts;
    }
}

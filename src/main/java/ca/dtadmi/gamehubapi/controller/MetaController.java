package ca.dtadmi.gamehubapi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @Value("${APP_GITHUB_URL:}")
    private String githubUrl;

    @Value("${APP_LINKEDIN_URL:}")
    private String linkedinUrl;

    @Value("${APP_CONTACT_EMAIL:}")
    private String contactEmail;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMeta() {
        return ResponseEntity.ok(
                Map.of(
                        "app", Map.of(
                                "name", "GameHub",
                                "description", "Portfolio hub for web-playable games and other projects"
                        ),
                        "links", Map.of(
                                "github", githubUrl,
                                "linkedin", linkedinUrl,
                                "email", contactEmail
                        )
                )
        );
    }
}

package ca.dtadmi.gamehubapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        // Placeholder static list; replace with DB-backed service later
        return ResponseEntity.ok(List.of(
                Map.of(
                        "id", "proj-001",
                        "name", "Portfolio Website",
                        "description", "My personal portfolio built with Next.js",
                        "github", System.getenv().getOrDefault("APP_GITHUB_URL", ""),
                        "updatedAt", Instant.now().toString()
                ),
                Map.of(
                        "id", "proj-002",
                        "name", "GameHub Frontend",
                        "description", "Web hub for playing my games and viewing projects",
                        "github", System.getenv().getOrDefault("APP_GITHUB_URL", ""),
                        "updatedAt", Instant.now().toString()
                )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        // Placeholder single project; in future, lookup by id
        return ResponseEntity.ok(Map.of(
                "id", id,
                "name", "Sample Project",
                "description", "Detailed description of the project",
                "github", System.getenv().getOrDefault("APP_GITHUB_URL", ""),
                "demoUrl", "",
                "updatedAt", Instant.now().toString()
        ));
    }
}

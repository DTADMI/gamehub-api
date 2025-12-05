package ca.dtadmi.gamehubapi.controller;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PortfolioController {

    @GetMapping("/featured")
    @Cacheable("featuredGames")
    public List<Map<String, Object>> getFeaturedGames() {
        // Simulate heavier computation by including a timestamp; cached responses will keep same timestamp
        return List.of(
                Map.of("id", 1, "title", "Snake Game", "updatedAt", Instant.now().toString()),
                Map.of("id", 2, "title", "Tetris Clone", "updatedAt", Instant.now().toString())
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/cache/featured/evict")
    @CacheEvict(value = "featuredGames", allEntries = true)
    public ResponseEntity<?> evictFeaturedCache() {
        return ResponseEntity.ok().build();
    }
}

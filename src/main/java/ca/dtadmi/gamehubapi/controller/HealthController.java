package ca.dtadmi.gamehubapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    /**
     * Lightweight public health-like endpoint for external uptime checks.
     * Does not require authentication (explicitly permitted in SecurityConfig).
     */
    @GetMapping("/healthz")
    public ResponseEntity<Map<String, Object>> healthz() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "ok",
                        "time", Instant.now().toString()
                )
        );
    }

    /**
     * Frontend-friendly health endpoint under /api namespace.
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> apiHealth() {
        return healthz();
    }
}

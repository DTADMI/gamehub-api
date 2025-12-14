package ca.dtadmi.gamehubapi.controller;

import ca.dtadmi.gamehubapi.realtime.dto.RealtimeDtos.Entry;
import ca.dtadmi.gamehubapi.service.LeaderboardService;
import ca.dtadmi.gamehubapi.service.RunIdService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/games/snake")
public class SnakeRestController {

    private static final String SCOPE = "snake:global";

    private final RunIdService runIdService;
    private final LeaderboardService leaderboardService;

    public SnakeRestController(RunIdService runIdService, LeaderboardService leaderboardService) {
        this.runIdService = runIdService;
        this.leaderboardService = leaderboardService;
    }

    /**
     * Start an anti-cheat run by issuing a short-lived runId.
     */
    @PostMapping("/run/start")
    public ResponseEntity<Map<String, Object>> startRun(@AuthenticationPrincipal UserDetails principal) {
        String userOrGuest = principal != null ? principal.getUsername() : "guest";
        String runId = runIdService.start(userOrGuest);
        Map<String, Object> body = new HashMap<>();
        body.put("runId", runId);
        return ResponseEntity.ok(body);
    }

    /**
     * Get a snapshot of the leaderboard. Cached for a few seconds via Caffeine.
     */
    @GetMapping("/leaderboard")
    @Cacheable(value = "lb_snake_global", key = "#limit")
    public ResponseEntity<Map<String, Object>> leaderboard(@RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.max(1, Math.min(50, limit));
        List<Entry> top = leaderboardService.topN(SCOPE, safeLimit);
        Map<String, Object> body = new HashMap<>();
        body.put("scope", SCOPE);
        body.put("top", top);
        return ResponseEntity.ok(body);
    }

    /**
     * Submit a score via REST as a fallback when realtime is disabled or unavailable.
     * Requires a valid runId issued from /run/start to mitigate trivial replay/forgery.
     */
    @PostMapping("/score")
    @CacheEvict(value = "lb_snake_global", allEntries = true)
    public ResponseEntity<Map<String, Object>> submitScore(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody ScoreSubmit body
    ) {
        String runId = body.runId;
        Integer score = body.score;
        if (runId == null || runId.isBlank() || score == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing runId or score"));
        }
        // Validate and consume the run once
        boolean ok = runIdService.validateAndConsume(runId);
        if (!ok) {
            return ResponseEntity.status(422).body(Map.of("error", "Invalid or expired runId"));
        }
        // Clamp score to a reasonable range
        int clamped = Math.max(0, Math.min(1_000_000, score));
        String nickname = principal != null ? principal.getUsername() : (Objects.toString(body.nickname, "guest"));
        // Persist best score per user/nickname
        leaderboardService.submit(SCOPE, nickname, clamped);
        Map<String, Object> res = new HashMap<>();
        res.put("accepted", true);
        res.put("score", clamped);
        res.put("nickname", nickname);
        return ResponseEntity.ok(res);
    }

    public static class ScoreSubmit {
        public String runId;
        public Integer score;
        public String nickname;
    }
}

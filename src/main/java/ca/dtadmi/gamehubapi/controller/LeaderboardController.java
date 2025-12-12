package ca.dtadmi.gamehubapi.controller;

import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final GameService gameService;

    @GetMapping
    public ResponseEntity<List<GameScore>> leaderboard(
            @RequestParam String gameType,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(gameService.getLeaderboard(gameType, limit));
    }
}

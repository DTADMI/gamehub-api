package ca.dtadmi.gamehubapi.controller;

import ca.dtadmi.gamehubapi.dto.GameStatsDto;
import ca.dtadmi.gamehubapi.dto.UserGameStatsDto;
import ca.dtadmi.gamehubapi.repository.GameScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final GameScoreRepository repo;

    @GetMapping("/game/{gameType}")
    public ResponseEntity<GameStatsDto> gameStats(@PathVariable String gameType) {
        long total = repo.countByGameType(gameType);
        Double avg = repo.averageScoreByGameType(gameType);
        Integer max = repo.maxScoreByGameType(gameType);
        // High scorer lookup omitted in MVP to keep it simple
        return ResponseEntity.ok(new GameStatsDto(total, avg, max, null));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserGameStatsDto> userStats(
            @PathVariable Long userId,
            @RequestParam(required = false) String gameType
    ) {
        long total;
        Double avg;
        Integer high;
        if (gameType == null || gameType.isBlank()) {
            total = repo.countByUser_Id(userId);
            avg = repo.averageScoreByUserId(userId);
            high = repo.maxScoreByUserId(userId);
        } else {
            total = repo.countByUser_IdAndGameType(userId, gameType);
            avg = repo.averageScoreByUserAndGameType(userId, gameType);
            high = repo.maxScoreByUserAndGameType(userId, gameType);
        }
        return ResponseEntity.ok(new UserGameStatsDto(total, high, avg, null));
    }
}

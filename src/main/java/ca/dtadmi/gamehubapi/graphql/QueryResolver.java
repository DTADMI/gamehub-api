package ca.dtadmi.gamehubapi.graphql;

import ca.dtadmi.gamehubapi.dto.GameStatsDto;
import ca.dtadmi.gamehubapi.dto.LeaderboardEntryDto;
import ca.dtadmi.gamehubapi.dto.UserGameStatsDto;
import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.repository.GameScoreRepository;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class QueryResolver {
    private final GameScoreRepository gameScoreRepository;
    private final UserRepository userRepository;

    @QueryMapping
    public List<GameScore> gameScores(
            @Argument String gameType,
            @Argument Integer limit) {
        Pageable pageable = limit != null && limit > 0 ?
                PageRequest.of(0, Math.min(limit, 100), Sort.by("score").descending().and(Sort.by("createdAt").ascending())) :
                Pageable.unpaged();
        return gameScoreRepository.findTopScoresByGameType(gameType, pageable);
    }

    @QueryMapping
    public List<GameScore> userScores(
            @Argument Long userId,
            @Argument String gameType,
            @Argument Integer limit) {
        if (userId == null || gameType == null || gameType.isBlank()) {
            return List.of();
        }
        int size = (limit == null || limit <= 0) ? 5 : Math.min(limit, 100);
        Pageable pageable = PageRequest.of(0, size, Sort.by("score").descending().and(Sort.by("createdAt").ascending()));
        return gameScoreRepository.findUserScores(userId, gameType, pageable);
    }

    @QueryMapping
    public List<LeaderboardEntryDto> leaderboard(
            @Argument String gameType,
            @Argument Integer limit) {
        int size = (limit == null || limit <= 0) ? 10 : Math.min(limit, 100);
        Pageable pageable = PageRequest.of(0, size, Sort.by("score").descending().and(Sort.by("createdAt").ascending()));
        List<GameScore> top = gameScoreRepository.findTopScoresByGameType(gameType, pageable);
        List<LeaderboardEntryDto> list = new ArrayList<>(top.size());
        for (int i = 0; i < top.size(); i++) {
            GameScore gs = top.get(i);
            list.add(new LeaderboardEntryDto(i + 1, gs.getUser(), gs.getScore(), gs.getGameType()));
        }
        return list;
    }

    @QueryMapping
    public GameStatsDto gameStats(@Argument String gameType) {
        long total = gameScoreRepository.countByGameType(gameType);
        Double avg = gameScoreRepository.averageScoreByGameType(gameType);
        Integer max = gameScoreRepository.maxScoreByGameType(gameType);
        return new GameStatsDto(total, avg, max, null);
    }

    @QueryMapping
    public UserGameStatsDto userStats(
            @Argument Long userId,
            @Argument String gameType) {
        long total;
        Double avg;
        Integer high;
        if (gameType == null || gameType.isBlank()) {
            total = gameScoreRepository.countByUser_Id(userId);
            avg = gameScoreRepository.averageScoreByUserId(userId);
            high = gameScoreRepository.maxScoreByUserId(userId);
        } else {
            total = gameScoreRepository.countByUser_IdAndGameType(userId, gameType);
            avg = gameScoreRepository.averageScoreByUserAndGameType(userId, gameType);
            high = gameScoreRepository.maxScoreByUserAndGameType(userId, gameType);
        }
        return new UserGameStatsDto(total, high, avg, null);
    }
}

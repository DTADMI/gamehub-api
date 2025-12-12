package ca.dtadmi.gamehubapi.graphql;

import ca.dtadmi.gamehubapi.dto.GameStatsDto;
import ca.dtadmi.gamehubapi.dto.LeaderboardEntryDto;
import ca.dtadmi.gamehubapi.dto.UserGameStatsDto;
import ca.dtadmi.gamehubapi.graphql.pagination.CursorUtil;
import ca.dtadmi.gamehubapi.graphql.types.GameType;
import ca.dtadmi.gamehubapi.graphql.types.LeaderboardScope;
import ca.dtadmi.gamehubapi.graphql.types.TimeWindow;
import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.repository.GameScoreRepository;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class QueryResolver {
    private final GameScoreRepository gameScoreRepository;
    private final UserRepository userRepository;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<GameScore> gameScores(
            @Argument GameType gameType,
            @Argument Integer first,
            @Argument String after) {
        int page = CursorUtil.decodeOffset(after);
        int size = (first == null || first <= 0) ? 100 : Math.min(first, 200);
        Pageable pageable = (size > 0) ?
                PageRequest.of(page, size, Sort.by("score").descending().and(Sort.by("createdAt").ascending())) :
                Pageable.unpaged();
        return gameScoreRepository.findTopScoresByGameType(gameType.toSlug(), pageable);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<GameScore> userScores(
            @Argument Long userId,
            @Argument GameType gameType,
            @Argument Integer first,
            @Argument String after) {
        if (userId == null || gameType == null) {
            return List.of();
        }
        int page = CursorUtil.decodeOffset(after);
        int size = (first == null || first <= 0) ? 20 : Math.min(first, 200);
        Pageable pageable = PageRequest.of(page, size, Sort.by("score").descending().and(Sort.by("createdAt").ascending()));
        return gameScoreRepository.findUserScores(userId, gameType.toSlug(), pageable);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public LeaderboardConnection leaderboard(
            @Argument GameType gameType,
            @Argument LeaderboardScope scope,
            @Argument TimeWindow window,
            @Argument Integer first,
            @Argument String after) {
        int page = CursorUtil.decodeOffset(after);
        int size = (first == null || first <= 0) ? 25 : Math.min(first, 200);
        Pageable pageable = PageRequest.of(page, size, Sort.by("score").descending().and(Sort.by("createdAt").ascending()));

        // MVP: ignore scope and window filters (TODO)
        List<GameScore> top = gameScoreRepository.findTopScoresByGameType(gameType.toSlug(), pageable);
        long total = gameScoreRepository.countByGameType(gameType.toSlug());

        List<LeaderboardEdge> edges = new ArrayList<>(top.size());
        int startRank = page * size;
        for (int i = 0; i < top.size(); i++) {
            GameScore gs = top.get(i);
            String cursor = CursorUtil.encodeOffset(page);
            edges.add(new LeaderboardEdge(
                    new LeaderboardEntryDto(
                            startRank + i + 1,
                            gs.getUser(),
                            gs.getScore(),
                            GameType.fromSlug(gs.getGameType())
                    ),
                    cursor));
        }
        boolean hasNext = (long) ((page + 1) * size) < total;
        PageInfo pageInfo = new PageInfo(hasNext, hasNext ? CursorUtil.encodeOffset(page + 1) : null);
        return new LeaderboardConnection(edges, pageInfo);
    }

    @QueryMapping
    public GameStatsDto gameStats(@Argument GameType gameType) {
        String gt = gameType.toSlug();
        long total = gameScoreRepository.countByGameType(gt);
        Double avg = gameScoreRepository.averageScoreByGameType(gt);
        Integer max = gameScoreRepository.maxScoreByGameType(gt);
        return new GameStatsDto(total, avg, max, null);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public UserGameStatsDto userStats(
            @Argument Long userId,
            @Argument GameType gameType) {
        long total;
        Double avg;
        Integer high;
        if (gameType == null) {
            total = gameScoreRepository.countByUser_Id(userId);
            avg = gameScoreRepository.averageScoreByUserId(userId);
            high = gameScoreRepository.maxScoreByUserId(userId);
        } else {
            String gt = gameType.toSlug();
            total = gameScoreRepository.countByUser_IdAndGameType(userId, gt);
            avg = gameScoreRepository.averageScoreByUserAndGameType(userId, gt);
            high = gameScoreRepository.maxScoreByUserAndGameType(userId, gt);
        }
        return new UserGameStatsDto(total, high, avg, null);
    }

    // Connection DTOs for GraphQL mapping
    @Data
    public static class PageInfo {
        private final boolean hasNextPage;
        private final String endCursor;
    }

    @Data
    public static class LeaderboardEdge {
        private final LeaderboardEntryDto node;
        private final String cursor;
    }

    @Data
    public static class LeaderboardConnection {
        private final List<LeaderboardEdge> edges;
        private final PageInfo pageInfo;
    }
}

package ca.dtadmi.gamehubapi.repository;

import ca.dtadmi.gamehubapi.model.GameScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameScoreRepository extends JpaRepository<GameScore, Long> {
    @Query("SELECT gs FROM GameScore gs WHERE gs.gameType = :gameType ORDER BY gs.score DESC, gs.createdAt ASC")
    List<GameScore> findTopScoresByGameType(@Param("gameType") String gameType, Pageable pageable);

    Page<GameScore> findByGameTypeOrderByScoreDescCreatedAtAsc(String gameType, Pageable pageable);

    @Query("SELECT gs FROM GameScore gs WHERE gs.user.id = :userId AND gs.gameType = :gameType ORDER BY gs.score DESC, gs.createdAt ASC")
    List<GameScore> findUserScores(@Param("userId") Long userId, @Param("gameType") String gameType, Pageable pageable);

    // Stats helpers (game-level)
    long countByGameType(String gameType);

    @Query("SELECT AVG(gs.score) FROM GameScore gs WHERE gs.gameType = :gameType")
    Double averageScoreByGameType(@Param("gameType") String gameType);

    @Query("SELECT MAX(gs.score) FROM GameScore gs WHERE gs.gameType = :gameType")
    Integer maxScoreByGameType(@Param("gameType") String gameType);

    // Stats helpers (user-level, across all games)
    long countByUser_Id(Long userId);

    @Query("SELECT AVG(gs.score) FROM GameScore gs WHERE gs.user.id = :userId")
    Double averageScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT MAX(gs.score) FROM GameScore gs WHERE gs.user.id = :userId")
    Integer maxScoreByUserId(@Param("userId") Long userId);

    // Stats helpers (user-level, per game)
    long countByUser_IdAndGameType(Long userId, String gameType);

    @Query("SELECT AVG(gs.score) FROM GameScore gs WHERE gs.user.id = :userId AND gs.gameType = :gameType")
    Double averageScoreByUserAndGameType(@Param("userId") Long userId, @Param("gameType") String gameType);

    @Query("SELECT MAX(gs.score) FROM GameScore gs WHERE gs.user.id = :userId AND gs.gameType = :gameType")
    Integer maxScoreByUserAndGameType(@Param("userId") Long userId, @Param("gameType") String gameType);
}

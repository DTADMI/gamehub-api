package ca.dtadmi.gamehubapi.repository;

import ca.dtadmi.gamehubapi.model.GameScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameScoreRepository extends JpaRepository<GameScore, Long> {
    @Query("SELECT gs FROM GameScore gs WHERE gs.gameType = :gameType ORDER BY gs.score DESC, gs.createdAt ASC")
    List<GameScore> findTopScoresByGameType(@Param("gameType") String gameType, Pageable pageable);

    @Query("SELECT gs FROM GameScore gs WHERE gs.user.id = :userId AND gs.gameType = :gameType ORDER BY gs.score DESC, gs.createdAt ASC")
    List<GameScore> findUserScores(@Param("userId") Long userId, @Param("gameType") String gameType, Pageable pageable);
}

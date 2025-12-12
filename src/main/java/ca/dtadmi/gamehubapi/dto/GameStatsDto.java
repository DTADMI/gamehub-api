package ca.dtadmi.gamehubapi.dto;

import ca.dtadmi.gamehubapi.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStatsDto {
    private long totalGames;
    private Double averageScore;
    private Integer highScore;
    private User highScorer; // optional, may be null in MVP
}

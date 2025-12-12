package ca.dtadmi.gamehubapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGameStatsDto {
    private long totalGames;
    private Integer highScore;
    private Double averageScore;
    private Integer rank; // optional, not computed in MVP
}

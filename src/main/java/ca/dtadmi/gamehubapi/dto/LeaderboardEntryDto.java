package ca.dtadmi.gamehubapi.dto;

import ca.dtadmi.gamehubapi.graphql.types.GameType;
import ca.dtadmi.gamehubapi.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDto {
    private int rank;
    private User user;
    private int score;
    private GameType gameType;
}

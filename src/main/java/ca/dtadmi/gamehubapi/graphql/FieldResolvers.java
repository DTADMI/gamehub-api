package ca.dtadmi.gamehubapi.graphql;

import ca.dtadmi.gamehubapi.graphql.types.GameType;
import ca.dtadmi.gamehubapi.model.GameScore;
import ca.dtadmi.gamehubapi.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.List;

@Controller
public class FieldResolvers {

    // Map GameScore.gameType (String) -> GameType enum
    @SchemaMapping(typeName = "GameScore", field = "gameType")
    public GameType gameScoreGameType(GameScore score) {
        return GameType.fromSlug(score.getGameType());
    }

    // Stub friends graph
    @SchemaMapping(typeName = "User", field = "friends")
    public Friends friends(User user) {
        // MVP: return empty friends list
        return new Friends(List.of(), 0);
    }

    @SchemaMapping(typeName = "User", field = "subscription")
    public Subscription subscription(User user) {
        // MVP: everyone is FREE without an active subscription
        return new Subscription("sub_" + user.getId(), String.valueOf(user.getId()), Plan.FREE, "inactive", OffsetDateTime.now().toString());
    }

    @SchemaMapping(typeName = "User", field = "premium")
    public PremiumFeatures premium(User user) {
        // MVP: no premium features enabled
        return new PremiumFeatures(false, false, false);
    }

    // Records to match GraphQL types
    public enum Plan {FREE, PRO}

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FriendEdge {
        private User user;
        private String since;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Friends {
        private List<FriendEdge> edges;
        private int count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Subscription {
        private String id;
        private String userId;
        private Plan plan;
        private String status;
        private String currentPeriodEnd;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PremiumFeatures {
        private boolean advancedLeaderboards;
        private boolean cosmetics;
        private boolean earlyAccess;
    }
}

package ca.dtadmi.gamehubapi.features;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight feature flags service.
 * Defaults come from environment; values can be toggled at runtime (dev only) via admin endpoint.
 * This is an interim step; can be swapped to OpenFeature provider later without changing call sites.
 */
@Service
public class FeatureFlagsService {

    private final ConcurrentHashMap<String, Boolean> overrides = new ConcurrentHashMap<>();

    @Value("${features.realtimeEnabled:true}")
    private boolean realtimeDefault;
    @Value("${features.chatEnabled:true}")
    private boolean chatDefault;
    @Value("${features.snakeLeaderboardEnabled:true}")
    private boolean snakeLeaderboardDefault;
    @Value("${features.antiCheatEnabled:false}")
    private boolean antiCheatDefault;
    @Value("${features.snake3dMode:false}")
    private boolean snake3dDefault;
    @Value("${features.breakoutMultiplayerBeta:false}")
    private boolean breakoutBetaDefault;

    public boolean isEnabled(String flag) {
        Boolean o = overrides.get(flag);
        if (o != null) return o;
        return defaultFor(flag);
    }

    public Map<String, Boolean> evaluateAll() {
        return Map.of(
                "realtime_enabled", isEnabled("realtime_enabled"),
                "chat_enabled", isEnabled("chat_enabled"),
                "snake_leaderboard_enabled", isEnabled("snake_leaderboard_enabled"),
                "anti_cheat_enabled", isEnabled("anti_cheat_enabled"),
                "snake_3d_mode", isEnabled("snake_3d_mode"),
                "breakout_multiplayer_beta", isEnabled("breakout_multiplayer_beta")
        );
    }

    public void toggle(String flag, boolean enable) {
        overrides.put(flag, enable);
    }

    private boolean defaultFor(String flag) {
        return switch (flag) {
            case "realtime_enabled" -> realtimeDefault;
            case "chat_enabled" -> chatDefault;
            case "snake_leaderboard_enabled" -> snakeLeaderboardDefault;
            case "anti_cheat_enabled" -> antiCheatDefault;
            case "snake_3d_mode" -> snake3dDefault;
            case "breakout_multiplayer_beta" -> breakoutBetaDefault;
            default -> false;
        };
    }

    public Set<String> knownFlags() {
        return Set.of(
                "realtime_enabled",
                "chat_enabled",
                "snake_leaderboard_enabled",
                "anti_cheat_enabled",
                "snake_3d_mode",
                "breakout_multiplayer_beta"
        );
    }
}

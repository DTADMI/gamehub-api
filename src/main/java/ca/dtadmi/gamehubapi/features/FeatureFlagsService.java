package ca.dtadmi.gamehubapi.features;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight feature flags service.
 * Defaults come from environment; values can be toggled at runtime (dev only) via admin endpoint.
 * This is an interim step; can be swapped to OpenFeature provider later without changing call sites.
 */
@Service
public class FeatureFlagsService {

    private final ConcurrentHashMap<String, Boolean> overrides = new ConcurrentHashMap<>();
    private final Environment env;

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

    public FeatureFlagsService(Environment env) {
        this.env = env;
    }

    public boolean isEnabled(String flag) {
        Boolean o = overrides.get(flag);
        if (o != null) return o;
        return defaultFor(flag);
    }

    public Map<String, Boolean> evaluateAll() {
        Map<String, Boolean> base = new LinkedHashMap<>();
        for (String f : knownFlags()) {
            base.put(f, isEnabled(f));
        }
        return base;
    }

    /**
     * Context-aware evaluation for the current user: applies optional segmentation
     * rules (role, email allowlist/domain) and gradual rollout percentage.
     * Env keys per-flag (examples for flag "chat_enabled"):
     * - features.chat_enabled.allowRoles=ROLE_ADMIN,ROLE_USER
     * - features.chat_enabled.allowEmails=a@b.com,c@d.com
     * - features.chat_enabled.allowEmailDomains=example.com,company.org
     * - features.chat_enabled.rolloutPercent=25
     */
    public Map<String, Boolean> evaluateAll(Authentication auth) {
        Map<String, Boolean> res = new LinkedHashMap<>();
        for (String f : knownFlags()) {
            res.put(f, isEnabledFor(f, auth));
        }
        return res;
    }

    public boolean isEnabledFor(String flag, Authentication auth) {
        boolean base = isEnabled(flag);
        if (!base) return false;

        // Segments
        Set<String> allowRoles = csvToSet(env.getProperty("features." + flag + ".allowRoles", ""));
        Set<String> allowEmails = csvToSet(env.getProperty("features." + flag + ".allowEmails", ""));
        Set<String> allowDomains = csvToSet(env.getProperty("features." + flag + ".allowEmailDomains", ""));

        String email = null;
        Set<String> roles = Collections.emptySet();
        if (auth != null && auth.isAuthenticated()) {
            // Try to resolve email from principal types we use
            Object principal = auth.getPrincipal();
            if (principal instanceof ca.dtadmi.gamehubapi.security.UserPrincipal up && up.getEmail() != null) {
                email = up.getEmail();
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                email = ud.getUsername();
            } else if (principal instanceof String s) {
                email = s;
            }
            Set<String> r = new HashSet<>();
            for (GrantedAuthority ga : auth.getAuthorities()) {
                r.add(ga.getAuthority());
            }
            roles = r;
        }

        if (!allowRoles.isEmpty() && roles.stream().noneMatch(allowRoles::contains)) {
            return false;
        }
        if (!allowEmails.isEmpty() && (email == null || !allowEmails.contains(email))) {
            return false;
        }
        if (!allowDomains.isEmpty()) {
            boolean domainOk = false;
            if (email != null && email.contains("@")) {
                String dom = email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
                domainOk = allowDomains.stream().anyMatch(d -> dom.equalsIgnoreCase(d) || dom.endsWith("." + d));
            }
            if (!domainOk) return false;
        }

        // Percentage rollout (0..100)
        int percent = env.getProperty("features." + flag + ".rolloutPercent", Integer.class, 100);
        percent = Math.max(0, Math.min(100, percent));
        if (percent >= 100) return true;
        if (percent <= 0) return false;

        String key = (email != null && !email.isBlank()) ? email : "guest";
        int bucket = stableBucket(key);
        return bucket < percent;
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

    private Set<String> csvToSet(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptySet();
        String[] parts = csv.split(",");
        Set<String> set = new HashSet<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) set.add(s);
        }
        return set;
    }

    private int stableBucket(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes(StandardCharsets.UTF_8));
            // use first two bytes for a 0..65535 int, then scale to 0..99
            int v = ((hash[0] & 0xFF) << 8) | (hash[1] & 0xFF);
            return (int) Math.floor((v / 65535.0) * 100);
        } catch (NoSuchAlgorithmException e) {
            // Fallback: simple hashCode
            int v = Math.abs(key.hashCode());
            return v % 100;
        }
    }
}

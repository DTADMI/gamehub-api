package ca.dtadmi.gamehubapi.feature;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Central place to evaluate feature flags by merging application.yml values
 * with an in-memory overlay that can be changed at runtime (e.g., from admin UI).
 * <p>
 * Note: This keeps state in-memory only. For persistence across restarts, back it
 * with a database table (e.g., key/value) and load it on startup.
 */
@Service
public class FeatureService {

    private final Environment env;
    private final Map<String, Object> overlay = Collections.synchronizedMap(new HashMap<>());

    public FeatureService(Environment env) {
        this.env = env;
    }

    public Map<String, Object> listAll() {
        Map<String, Object> res = new HashMap<>();
        // Base values from application.yml (with defaults)
        put(res, "realtime_enabled", getBool("features.realtime_enabled", true));
        put(res, "chess_enabled", getBool("features.chess_enabled", true));
        put(res, "checkers_enabled", getBool("features.checkers_enabled", true));
        put(res, "payments.stripe_enabled", getBool("features.payments.stripe_enabled", false));
        put(res, "cache.redis_enabled", getBool("features.cache.redis_enabled", false));
        put(res, "kv.redis_enabled", getBool("features.kv.redis_enabled", false));
        put(res, "db.external_enabled", getBool("features.db.external_enabled", true));
        put(res, "mail.provider", env.getProperty("features.mail.provider", "smtp"));

        // Apply overlay (runtime changes win)
        synchronized (overlay) {
            for (Map.Entry<String, Object> e : overlay.entrySet()) {
                res.put(e.getKey(), e.getValue());
            }
        }
        return res;
    }

    public Map<String, Object> upsert(String key, Object value) {
        overlay.put(key, value);
        return listAll();
    }

    public boolean isEnabled(String key, boolean def) {
        // Check overlay first
        Object v;
        synchronized (overlay) {
            v = overlay.get(key);
        }
        if (v instanceof Boolean b) {
            return b;
        }
        // Fall back to env/yaml
        return getBool(mapKeyToYaml(key), def);
    }

    private String mapKeyToYaml(String key) {
        // Keys like "payments.stripe_enabled" map to "features.payments.stripe_enabled"
        if (key.startsWith("features.")) {
            return key;
        }
        return "features." + key;
    }

    private void put(Map<String, Object> map, String key, Object value) {
        map.put(key, value);
    }

    private boolean getBool(String key, boolean def) {
        return env.getProperty(key, Boolean.class, def);
    }
}

package ca.dtadmi.gamehubapi.controller;

import ca.dtadmi.gamehubapi.features.FeatureFlagsService;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FeatureController {

    private final FeatureFlagsService flags;
    private final Environment env;

    public FeatureController(FeatureFlagsService flags, Environment env) {
        this.flags = flags;
        this.env = env;
    }

    @GetMapping("/features")
    public ResponseEntity<Map<String, Object>> all() {
        // Start with the centralized feature flags (service-backed)
        Map<String, Object> res = new HashMap<>(flags.evaluateAll());

        // Merge additional environment-driven feature/config flags that existed in the
        // former FeaturesController. Keep keys distinct from the service set to avoid
        // ambiguity; where names overlap, prefer the service-backed values already present.
        putIfAbsent(res, "chess_enabled", getBool("features.chess_enabled", true));
        putIfAbsent(res, "checkers_enabled", getBool("features.checkers_enabled", true));
        putIfAbsent(res, "payments.stripe_enabled", getBool("features.payments.stripe_enabled", false));
        putIfAbsent(res, "cache.redis_enabled", getBool("features.cache.redis_enabled", false));
        putIfAbsent(res, "kv.redis_enabled", getBool("features.kv.redis_enabled", false));
        putIfAbsent(res, "db.external_enabled", getBool("features.db.external_enabled", true));
        // Not strictly a boolean flag; keep as string
        putIfAbsent(res, "mail.provider", env.getProperty("features.mail.provider", "smtp"));

        return ResponseEntity.ok(res);
    }

    @PostMapping("/admin/features/{flag}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable String flag, @RequestParam(defaultValue = "true") boolean enable) {
        if (!flags.knownFlags().contains(flag)) {
            return ResponseEntity.badRequest().body(Map.of("error", "unknown flag: " + flag));
        }
        flags.toggle(flag, enable);
        return ResponseEntity.ok(Map.of("flag", flag, "enabled", flags.isEnabled(flag)));
    }

    private boolean getBool(String key, boolean def) {
        return env.getProperty(key, Boolean.class, def);
    }

    private void putIfAbsent(Map<String, Object> res, String key, Object val) {
        res.putIfAbsent(key, val);
    }
}

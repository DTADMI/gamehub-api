package ca.dtadmi.gamehubapi.feature;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Minimal admin endpoints to review and update feature flags at runtime.
 * NOTE: Secure these endpoints with proper authentication/authorization in production
 * (e.g., Spring Security roles). For now, this is intentionally minimal to unblock
 * the admin UI work and can be guarded behind network policies or dev profiles.
 */
@RestController
@RequestMapping("/api/admin/features")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeaturesController {

    private final FeatureService featureService;

    public AdminFeaturesController(FeatureService featureService) {
        this.featureService = featureService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        return ResponseEntity.ok(featureService.listAll());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> upsert(@RequestBody UpsertRequest req) {
        Object v = switch (req.value.toLowerCase()) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> req.value;
        };
        Map<String, Object> all = featureService.upsert(req.key, v);
        return ResponseEntity.ok(all);
    }

    public static class UpsertRequest {
        @NotBlank
        public String key;
        @NotBlank
        public String value; // accept string; backend will store as string/bool depending on value
    }
}

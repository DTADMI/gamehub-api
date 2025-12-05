package ca.dtadmi.gamehubapi.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/admin/geo")
public class GeoAdminController {

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> users(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String country,
            @RequestParam(required = false, defaultValue = "lastActiveTs") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order
    ) {
        // Mock data for now; replace with DB lookup later
        List<UserGeo> data = new ArrayList<>();
        data.add(new UserGeo("u1", "Ari", 48.8566, 2.3522, "FR", "Paris", Instant.now().minusSeconds(600).toEpochMilli(), 1200, List.of("snake", "breakout"), List.of("played", "scored")));
        data.add(new UserGeo("u2", "Bo", 37.7749, -122.4194, "US", "San Francisco", Instant.now().minusSeconds(60).toEpochMilli(), 5050, List.of("snake"), List.of("played", "chat")));
        data.add(new UserGeo("u3", "Chen", 35.6762, 139.6503, "JP", "Tokyo", Instant.now().minusSeconds(3600).toEpochMilli(), 9999, List.of("breakout"), List.of("played")));

        // Filter
        if (game != null && !game.isBlank()) {
            data.removeIf(d -> d.games() == null || !d.games().contains(game));
        }
        if (country != null && !country.isBlank()) {
            data.removeIf(d -> !country.equalsIgnoreCase(d.country()));
        }

        // Sort
        Comparator<UserGeo> cmp = switch (sort) {
            case "totalScore" -> Comparator.comparingInt(UserGeo::totalScore);
            case "nickname" -> Comparator.comparing(UserGeo::nickname);
            default -> Comparator.comparingLong(UserGeo::lastActiveTs);
        };
        if ("desc".equalsIgnoreCase(order)) cmp = cmp.reversed();
        data.sort(cmp);

        Map<String, Object> body = new HashMap<>();
        body.put("count", data.size());
        body.put("items", data);
        return ResponseEntity.ok(body);
    }

    public record UserGeo(
            String id,
            String nickname,
            double lat,
            double lng,
            String country,
            String city,
            long lastActiveTs,
            int totalScore,
            List<String> games,
            List<String> activities
    ) {
    }
}

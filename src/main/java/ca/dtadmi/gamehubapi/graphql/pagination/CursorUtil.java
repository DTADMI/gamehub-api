package ca.dtadmi.gamehubapi.graphql.pagination;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CursorUtil {
    private CursorUtil() {
    }

    public static String encodeOffset(int offset) {
        String raw = String.valueOf(Math.max(0, offset));
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static int decodeOffset(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0;
        try {
            String raw = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            return Math.max(0, Integer.parseInt(raw));
        } catch (Exception e) {
            return 0;
        }
    }
}

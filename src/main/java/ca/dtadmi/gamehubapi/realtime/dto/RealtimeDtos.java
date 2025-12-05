package ca.dtadmi.gamehubapi.realtime.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RealtimeDtos {
    public static class Envelope<T> {
        @NotBlank
        public String eventId = UUID.randomUUID().toString();
        @NotBlank
        public String type;
        @Positive
        public long ts = Instant.now().toEpochMilli();
        @NotNull
        public Room room;
        @NotNull
        public User user;
        @NotNull
        public T payload;
    }

    public static class Room {
        @NotBlank
        @Size(max = 128)
        public String id;      // e.g. "snake:global"
        @NotBlank
        @Size(max = 32)
        public String game;    // e.g. "snake"
        @Size(max = 16)
        public String visibility = "public";
    }

    public static class User {
        @Size(max = 64)
        public String id;
        @NotBlank
        @Pattern(regexp = "guest|user|mod|admin")
        public String role = "guest"; // guest|user|mod|admin
        @NotBlank
        @Size(min = 1, max = 32)
        public String nickname;
        @NotBlank
        @Pattern(regexp = "free|monthly|yearly|lifetime")
        public String subscription = "free"; // free|monthly|yearly|lifetime
    }

    // Presence
    public static class PresenceIn {
        @NotBlank
        @Pattern(regexp = "join|leave|heartbeat")
        public String status; // join|leave|heartbeat
    }

    public static class PresenceOut {
        @Min(0)
        public int count;
        public List<PublicUser> users;
    }

    public static class PublicUser {
        @Size(max = 96)
        public String id;
        @Size(max = 32)
        public String nickname;
    }

    // Score
    public static class ScoreIn {
        @Min(0)
        @Max(1000000)
        public int value;
        @Size(max = 128)
        public String runId;
        @Size(max = 256)
        public String proof;
    }

    public static class LeaderboardOut {
        public List<Entry> top;
        public Integer yourRank;
    }

    public static class Entry {
        @Size(max = 32)
        public String nickname;
        public int value;
    }

    // Chat
    public static class ChatIn {
        @NotBlank
        @Size(min = 1, max = 300)
        public String text;
    }

    public static class ChatOut {
        public String nickname;
        public String text;
    }

    // Generic move DTOs for board games (Chess/Checkers)
    public static class MoveIn {
        // Algebraic coordinates like e2e4 for chess, or simple from/to indexes
        @Size(max = 8)
        public String from;
        @Size(max = 8)
        public String to;
        // Optional promotion piece for chess: q/r/b/n
        @Size(max = 1)
        public String promo;
        // Optional raw notation provided by client (SAN, PDN, etc.)
        @Size(max = 32)
        public String notation;
    }

    public static class MoveOut {
        public String from;
        public String to;
        public String promo;
        public String notation;
        public String side; // which player moved (WHITE/BLACK or RED/BLACK)
    }
}

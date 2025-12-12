package ca.dtadmi.gamehubapi.graphql.types;

public enum GameType {
    SNAKE,
    BUBBLE_POP,
    TETRIS,
    BREAKOUT,
    KNITZY,
    MEMORY,
    CHECKERS,
    CHESS,
    PLATFORMER,
    TOWER_DEFENSE;

    public static GameType fromSlug(String value) {
        if (value == null || value.isBlank()) return null;
        String norm = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return GameType.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            // attempt mapping common slugs
            switch (value.toLowerCase()) {
                case "snake":
                    return SNAKE;
                case "bubble_pop":
                case "bubble-pop":
                    return BUBBLE_POP;
                case "tetris":
                    return TETRIS;
                case "breakout":
                    return BREAKOUT;
                case "knitzy":
                    return KNITZY;
                case "memory":
                    return MEMORY;
                case "checkers":
                    return CHECKERS;
                case "chess":
                    return CHESS;
                case "platformer":
                    return PLATFORMER;
                case "tower_defense":
                case "tower-defense":
                    return TOWER_DEFENSE;
                default:
                    return null;
            }
        }
    }

    public String toSlug() {
        return this.name().toLowerCase();
    }
}

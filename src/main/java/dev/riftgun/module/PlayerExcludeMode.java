package dev.riftgun.module;

/** Which side of a Player Target portal excludes the selected target. */
public enum PlayerExcludeMode {
    OFF(0),
    ENTRY_AND_EXIT(1),
    EXIT_ONLY(2);

    private final int id;

    PlayerExcludeMode(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public PlayerExcludeMode step(int amount) {
        PlayerExcludeMode[] values = values();
        return values[Math.floorMod(ordinal() + amount, values.length)];
    }

    public static PlayerExcludeMode byId(int id) {
        for (PlayerExcludeMode mode : values()) {
            if (mode.id == id) return mode;
        }
        return ENTRY_AND_EXIT;
    }
}

package dev.riftgun.relocation;

/** Persisted per-gun switches for explicit and SMART entity relocation routes. */
public record EntityRelocationSettings(boolean enabled, boolean smartRouting) {
    public static EntityRelocationSettings defaults() {
        return new EntityRelocationSettings(true, false);
    }

    public EntityRelocationSettings withEnabled(boolean value) {
        return new EntityRelocationSettings(value, smartRouting);
    }

    public EntityRelocationSettings withSmartRouting(boolean value) {
        return new EntityRelocationSettings(enabled, value);
    }
}

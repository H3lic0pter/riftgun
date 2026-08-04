package dev.riftgun.data;

import net.minecraft.nbt.CompoundTag;

public record PortalPlayerSettings(
    boolean safetyCheckEnabled,
    boolean animationsEnabled,
    boolean soundsEnabled,
    DestinationSort sort
) {
    public static PortalPlayerSettings defaults() {
        return new PortalPlayerSettings(true, true, true, DestinationSort.RECENT);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("SafetyCheck", safetyCheckEnabled);
        tag.putBoolean("Animations", animationsEnabled);
        tag.putBoolean("Sounds", soundsEnabled);
        tag.putString("Sort", sort.name());
        return tag;
    }

    public static PortalPlayerSettings load(CompoundTag tag) {
        if (tag.isEmpty()) return defaults();
        DestinationSort sort;
        try {
            sort = DestinationSort.valueOf(tag.getString("Sort"));
        } catch (IllegalArgumentException ignored) {
            sort = DestinationSort.RECENT;
        }
        return new PortalPlayerSettings(
            !tag.contains("SafetyCheck") || tag.getBoolean("SafetyCheck"),
            !tag.contains("Animations") || tag.getBoolean("Animations"),
            !tag.contains("Sounds") || tag.getBoolean("Sounds"),
            sort
        );
    }
}


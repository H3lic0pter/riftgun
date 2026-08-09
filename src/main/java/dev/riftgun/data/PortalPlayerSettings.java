package dev.riftgun.data;

import dev.riftgun.sound.PortalSoundSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record PortalPlayerSettings(
    boolean safetyCheckEnabled,
    boolean confirmDeletion,
    boolean confirmDiscardedChanges,
    boolean confirmClearFluid,
    boolean animationsEnabled,
    boolean soundsEnabled,
    DestinationSort sort,
    PortalPlacementMode placementMode,
    int smartDistance,
    PortalPredictionMode predictionMode,
    PortalSoundSettings portalSounds
) {
    public static final int DEFAULT_SMART_DISTANCE = 8;

    public PortalPlayerSettings {
        if (portalSounds == null) portalSounds = PortalSoundSettings.defaults();
    }

    public static PortalPlayerSettings defaults() {
        return new PortalPlayerSettings(true, true, true, true, true, true, DestinationSort.RECENT,
            PortalPlacementMode.SMART, DEFAULT_SMART_DISTANCE, PortalPredictionMode.OFF,
            PortalSoundSettings.defaults());
    }

    public PortalPlayerSettings withPortalSounds(PortalSoundSettings value) {
        return new PortalPlayerSettings(safetyCheckEnabled, confirmDeletion, confirmDiscardedChanges,
            confirmClearFluid, animationsEnabled, soundsEnabled, sort, placementMode, smartDistance,
            predictionMode, value);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("SafetyCheck", safetyCheckEnabled);
        tag.putBoolean("ConfirmDeletion", confirmDeletion);
        tag.putBoolean("ConfirmDiscardedChanges", confirmDiscardedChanges);
        tag.putBoolean("ConfirmClearFluid", confirmClearFluid);
        tag.putBoolean("Animations", animationsEnabled);
        tag.putBoolean("Sounds", soundsEnabled);
        tag.putString("Sort", sort.name());
        tag.putString("PlacementMode", placementMode.name());
        tag.putInt("SmartDistance", smartDistance);
        tag.putString("MotionPrediction", predictionMode.name());
        tag.put("PortalSounds", portalSounds.save());
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
        PortalPredictionMode predictionMode;
        if (tag.contains("MotionPrediction", Tag.TAG_STRING)) {
            predictionMode = PortalPredictionMode.parse(tag.getString("MotionPrediction"),
                PortalPredictionMode.OFF);
        } else if (tag.contains("MotionPrediction")) {
            predictionMode = tag.getBoolean("MotionPrediction")
                ? PortalPredictionMode.TRAJECTORY : PortalPredictionMode.OFF;
        } else {
            predictionMode = PortalPredictionMode.OFF;
        }
        return new PortalPlayerSettings(
            !tag.contains("SafetyCheck") || tag.getBoolean("SafetyCheck"),
            !tag.contains("ConfirmDeletion") || tag.getBoolean("ConfirmDeletion"),
            !tag.contains("ConfirmDiscardedChanges") || tag.getBoolean("ConfirmDiscardedChanges"),
            !tag.contains("ConfirmClearFluid") || tag.getBoolean("ConfirmClearFluid"),
            !tag.contains("Animations") || tag.getBoolean("Animations"),
            !tag.contains("Sounds") || tag.getBoolean("Sounds"),
            sort,
            PortalPlacementMode.parse(tag.getString("PlacementMode")),
            tag.contains("SmartDistance") ? Math.max(1, tag.getInt("SmartDistance")) : DEFAULT_SMART_DISTANCE,
            predictionMode,
            tag.contains("PortalSounds", Tag.TAG_COMPOUND)
                ? PortalSoundSettings.load(tag.getCompound("PortalSounds"))
                : PortalSoundSettings.defaults()
        );
    }
}

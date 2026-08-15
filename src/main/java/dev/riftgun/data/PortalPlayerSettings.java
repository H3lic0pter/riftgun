package dev.riftgun.data;
import dev.riftgun.core.nbt.Nbt;

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
            PortalPlacementMode.SMART, DEFAULT_SMART_DISTANCE, PortalPredictionMode.PROJECTION,
            PortalSoundSettings.defaults());
    }

    public PortalPlayerSettings withPortalSounds(PortalSoundSettings value) {
        return new PortalPlayerSettings(safetyCheckEnabled, confirmDeletion, confirmDiscardedChanges,
            confirmClearFluid, animationsEnabled, soundsEnabled, sort, placementMode, smartDistance,
            predictionMode, value);
    }

    public PortalPlayerSettings withPlacementMode(PortalPlacementMode value) {
        return new PortalPlayerSettings(safetyCheckEnabled, confirmDeletion, confirmDiscardedChanges,
            confirmClearFluid, animationsEnabled, soundsEnabled, sort, value, smartDistance,
            predictionMode, portalSounds);
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
            sort = DestinationSort.valueOf(Nbt.getString(tag, "Sort"));
        } catch (IllegalArgumentException ignored) {
            sort = DestinationSort.RECENT;
        }
        PortalPredictionMode predictionMode;
        if (Nbt.contains(tag, "MotionPrediction")) {
            predictionMode = PortalPredictionMode.parse(Nbt.getString(tag, "MotionPrediction"),
                PortalPredictionMode.PROJECTION);
        } else if (tag.contains("MotionPrediction")) {
            predictionMode = Nbt.getBoolean(tag, "MotionPrediction")
                ? PortalPredictionMode.TRAJECTORY : PortalPredictionMode.OFF;
        } else {
            predictionMode = PortalPredictionMode.PROJECTION;
        }
        return new PortalPlayerSettings(
            !tag.contains("SafetyCheck") || Nbt.getBoolean(tag, "SafetyCheck"),
            !tag.contains("ConfirmDeletion") || Nbt.getBoolean(tag, "ConfirmDeletion"),
            !tag.contains("ConfirmDiscardedChanges") || Nbt.getBoolean(tag, "ConfirmDiscardedChanges"),
            !tag.contains("ConfirmClearFluid") || Nbt.getBoolean(tag, "ConfirmClearFluid"),
            !tag.contains("Animations") || Nbt.getBoolean(tag, "Animations"),
            !tag.contains("Sounds") || Nbt.getBoolean(tag, "Sounds"),
            sort,
            PortalPlacementMode.parse(Nbt.getString(tag, "PlacementMode")),
            tag.contains("SmartDistance") ? Math.max(1, Nbt.getInt(tag, "SmartDistance")) : DEFAULT_SMART_DISTANCE,
            predictionMode,
            Nbt.contains(tag, "PortalSounds")
                ? PortalSoundSettings.load(Nbt.getCompound(tag, "PortalSounds"))
                : PortalSoundSettings.defaults()
        );
    }
}

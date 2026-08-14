package dev.riftgun.module;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.portal.PortalOpenDuration;
import net.minecraft.nbt.CompoundTag;

public record PortalModuleRules(
    int baseCapacity,
    int reservoirBonus,
    int maximumReservoirModules,
    int baseSurfaceRange,
    int surfaceRangeBonus,
    int maximumSurfaceRangeModules,
    int maximumDurationExtensionModules,
    int durationExtensionSecondsPerModule,
    int basePortalDurationSeconds,
    boolean matterAnchorPreventsDespawn
) {
    public static final int DEFAULT_BASE_CAPACITY = 8000;
    public static final int DEFAULT_RESERVOIR_BONUS = 8000;
    public static final int DEFAULT_MAXIMUM_RESERVOIR_MODULES = 2;
    public static final int DEFAULT_BASE_SURFACE_RANGE = 32;
    public static final int DEFAULT_SURFACE_RANGE_BONUS = 16;
    public static final int DEFAULT_MAXIMUM_SURFACE_RANGE_MODULES = 3;
    public static final int DEFAULT_MAXIMUM_DURATION_EXTENSION_MODULES = 1;
    public static final int DEFAULT_DURATION_EXTENSION_SECONDS_PER_MODULE = 45;
    public static final int DEFAULT_BASE_PORTAL_DURATION_SECONDS = 15;

    public PortalModuleRules {
        baseCapacity = Math.max(1, baseCapacity);
        reservoirBonus = Math.max(1, reservoirBonus);
        maximumReservoirModules = Math.max(0, maximumReservoirModules);
        baseSurfaceRange = Math.max(1, baseSurfaceRange);
        surfaceRangeBonus = Math.max(1, surfaceRangeBonus);
        maximumSurfaceRangeModules = Math.max(0, maximumSurfaceRangeModules);
        maximumDurationExtensionModules = Math.max(0, maximumDurationExtensionModules);
        durationExtensionSecondsPerModule = Math.max(1, durationExtensionSecondsPerModule);
        basePortalDurationSeconds = Math.clamp(basePortalDurationSeconds,
            PortalOpenDuration.MINIMUM_SECONDS, PortalOpenDuration.MAXIMUM_CONFIGURABLE_SECONDS);
    }

    public static PortalModuleRules current() {
        return new PortalModuleRules(
            DEFAULT_BASE_CAPACITY,
            RiftConfigs.server().modules().reservoirCapacityPerModule(),
            RiftConfigs.server().modules().maximumReservoirModules(),
            DEFAULT_BASE_SURFACE_RANGE,
            RiftConfigs.server().modules().surfaceRangePerModule(),
            RiftConfigs.server().modules().maximumSurfaceRangeModules(),
            RiftConfigs.server().modules().maximumDurationExtensionModules(),
            RiftConfigs.server().modules().durationExtensionSecondsPerModule(),
            RiftConfigs.server().portal().maximumDurationSeconds(),
            RiftConfigs.server().modules().matterAnchorPreventsDespawn()
        );
    }

    public static PortalModuleRules defaults() {
        return new PortalModuleRules(
            DEFAULT_BASE_CAPACITY,
            DEFAULT_RESERVOIR_BONUS,
            DEFAULT_MAXIMUM_RESERVOIR_MODULES,
            DEFAULT_BASE_SURFACE_RANGE,
            DEFAULT_SURFACE_RANGE_BONUS,
            DEFAULT_MAXIMUM_SURFACE_RANGE_MODULES,
            DEFAULT_MAXIMUM_DURATION_EXTENSION_MODULES,
            DEFAULT_DURATION_EXTENSION_SECONDS_PER_MODULE,
            DEFAULT_BASE_PORTAL_DURATION_SECONDS,
            true
        );
    }

    public int capacityFor(int activeReservoirModules) {
        long result = (long) baseCapacity + (long) Math.max(0, activeReservoirModules) * reservoirBonus;
        return (int) Math.min(Integer.MAX_VALUE, result);
    }

    public int maximumSurfaceRangeFor(int activeRangeModules) {
        long result = (long) baseSurfaceRange + (long) Math.max(0, activeRangeModules) * surfaceRangeBonus;
        return (int) Math.min(Integer.MAX_VALUE, result);
    }

    public int maximumPortalDurationSeconds(int installedExtensionModules) {
        long result = (long) basePortalDurationSeconds
            + (long) Math.max(0, Math.min(maximumDurationExtensionModules, installedExtensionModules))
                * durationExtensionSecondsPerModule;
        return (int) Math.min(PortalOpenDuration.MAXIMUM_CONFIGURABLE_SECONDS, result);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("BaseCapacity", baseCapacity);
        tag.putInt("ReservoirBonus", reservoirBonus);
        tag.putInt("MaximumReservoirModules", maximumReservoirModules);
        tag.putInt("BaseSurfaceRange", baseSurfaceRange);
        tag.putInt("SurfaceRangeBonus", surfaceRangeBonus);
        tag.putInt("MaximumSurfaceRangeModules", maximumSurfaceRangeModules);
        tag.putInt("MaximumDurationExtensionModules", maximumDurationExtensionModules);
        tag.putInt("DurationExtensionSecondsPerModule", durationExtensionSecondsPerModule);
        tag.putInt("BasePortalDurationSeconds", basePortalDurationSeconds);
        tag.putBoolean("MatterAnchorPreventsDespawn", matterAnchorPreventsDespawn);
        return tag;
    }

    public static PortalModuleRules load(CompoundTag tag) {
        if (tag.isEmpty()) return defaults();
        return new PortalModuleRules(
            tag.getInt("BaseCapacity"),
            tag.getInt("ReservoirBonus"),
            tag.getInt("MaximumReservoirModules"),
            tag.getInt("BaseSurfaceRange"),
            tag.getInt("SurfaceRangeBonus"),
            tag.getInt("MaximumSurfaceRangeModules"),
            tag.getInt("MaximumDurationExtensionModules"),
            tag.getInt("DurationExtensionSecondsPerModule"),
            tag.contains("BasePortalDurationSeconds")
                ? tag.getInt("BasePortalDurationSeconds") : DEFAULT_BASE_PORTAL_DURATION_SECONDS,
            !tag.contains("MatterAnchorPreventsDespawn") || tag.getBoolean("MatterAnchorPreventsDespawn")
        );
    }
}

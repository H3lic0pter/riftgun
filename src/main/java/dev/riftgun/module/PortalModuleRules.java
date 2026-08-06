package dev.riftgun.module;

import dev.riftgun.config.ServerConfig;
import net.minecraft.nbt.CompoundTag;

public record PortalModuleRules(
    int baseCapacity,
    int reservoirBonus,
    int maximumReservoirModules,
    int baseSurfaceRange,
    int surfaceRangeBonus,
    int maximumSurfaceRangeModules
) {
    public static final int DEFAULT_BASE_CAPACITY = 8000;
    public static final int DEFAULT_RESERVOIR_BONUS = 8000;
    public static final int DEFAULT_MAXIMUM_RESERVOIR_MODULES = 2;
    public static final int DEFAULT_BASE_SURFACE_RANGE = 32;
    public static final int DEFAULT_SURFACE_RANGE_BONUS = 16;
    public static final int DEFAULT_MAXIMUM_SURFACE_RANGE_MODULES = 3;

    public PortalModuleRules {
        baseCapacity = Math.max(1, baseCapacity);
        reservoirBonus = Math.max(1, reservoirBonus);
        maximumReservoirModules = Math.max(0, maximumReservoirModules);
        baseSurfaceRange = Math.max(1, baseSurfaceRange);
        surfaceRangeBonus = Math.max(1, surfaceRangeBonus);
        maximumSurfaceRangeModules = Math.max(0, maximumSurfaceRangeModules);
    }

    public static PortalModuleRules current() {
        return new PortalModuleRules(
            DEFAULT_BASE_CAPACITY,
            ServerConfig.VALUES.reservoirModuleCapacity.get(),
            ServerConfig.VALUES.maxReservoirModules.get(),
            DEFAULT_BASE_SURFACE_RANGE,
            ServerConfig.VALUES.surfaceRangePerModule.get(),
            ServerConfig.VALUES.maxSurfaceRangeModules.get()
        );
    }

    public static PortalModuleRules defaults() {
        return new PortalModuleRules(
            DEFAULT_BASE_CAPACITY,
            DEFAULT_RESERVOIR_BONUS,
            DEFAULT_MAXIMUM_RESERVOIR_MODULES,
            DEFAULT_BASE_SURFACE_RANGE,
            DEFAULT_SURFACE_RANGE_BONUS,
            DEFAULT_MAXIMUM_SURFACE_RANGE_MODULES
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

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("BaseCapacity", baseCapacity);
        tag.putInt("ReservoirBonus", reservoirBonus);
        tag.putInt("MaximumReservoirModules", maximumReservoirModules);
        tag.putInt("BaseSurfaceRange", baseSurfaceRange);
        tag.putInt("SurfaceRangeBonus", surfaceRangeBonus);
        tag.putInt("MaximumSurfaceRangeModules", maximumSurfaceRangeModules);
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
            tag.getInt("MaximumSurfaceRangeModules")
        );
    }
}

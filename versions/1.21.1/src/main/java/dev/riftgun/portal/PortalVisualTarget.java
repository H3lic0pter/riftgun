package dev.riftgun.portal;

import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Client-synced destination geometry used by visual backends only. */
public record PortalVisualTarget(
    UUID portalId,
    ResourceKey<Level> dimension,
    Vec3 position,
    Vec3 right,
    Vec3 up
) {}

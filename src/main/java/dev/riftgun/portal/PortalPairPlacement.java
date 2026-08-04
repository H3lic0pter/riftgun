package dev.riftgun.portal;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record PortalPairPlacement(
    ResourceKey<Level> exitDimension,
    PortalPlacement entry,
    PortalPlacement exit
) {}

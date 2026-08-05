package dev.riftgun.service;

import dev.riftgun.data.Destination;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Position-only cache key; presentation metadata intentionally does not invalidate safety. */
public record DestinationSafetyFingerprint(
    UUID destinationId,
    ResourceKey<Level> dimension,
    long x,
    long y,
    long z
) {
    public static DestinationSafetyFingerprint of(Destination destination) {
        return new DestinationSafetyFingerprint(
            destination.id(),
            destination.dimension(),
            Double.doubleToLongBits(destination.x()),
            Double.doubleToLongBits(destination.y()),
            Double.doubleToLongBits(destination.z())
        );
    }
}

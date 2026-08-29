package dev.riftgun.service;

import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.portal.PortalChunkGuard;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Revalidates a persisted portal footprint after loading its chunk on demand. */
public final class PortalStoredPlacementValidator {
    public static boolean valid(ServerPlayer player, ServerLevel level, PortalPlacement placement) {
        BlockPos center = BlockPos.containing(placement.center());
        if (!PortalChunkGuard.inWorldBounds(level, center)) return false;
        level.getChunk(center.getX() >> 4, center.getZ() >> 4);

        if (placement.anchored()) {
            BlockPos anchor = placement.anchor();
            if (!PortalChunkGuard.inWorldBounds(level, anchor)
                || level.getBlockState(anchor).getCollisionShape(level, anchor).isEmpty()) return false;
            if (placement.geometry().expanded()
                && !PortalSupportArea.hasFullExpandedSupport(level, placement)) return false;
            return !level.getBlockCollisions(null,
                placement.bounds().deflate(0.002)).iterator().hasNext();
        }

        double minimumExposure = placement.geometry().expanded()
            ? PortalAperturePolicy.EXPANDED_MINIMUM_EXPOSURE
            : RiftRuntime.current().placementCapabilities().minimumFloatingPortalExposure(player);
        return PortalFaceExposure.hasMinimumExposure(level, placement, minimumExposure);
    }

    private PortalStoredPlacementValidator() {}
}

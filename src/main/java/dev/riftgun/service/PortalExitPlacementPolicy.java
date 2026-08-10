package dev.riftgun.service;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalExitTarget;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Chooses horizontal exit geometry without applying entry-only support requirements. */
final class PortalExitPlacementPolicy {
    private static final double SURFACE_OFFSET = PortalPlacement.DEPTH * 0.5 + 0.002;

    static PortalPlacement resolveHorizontal(PortalExitTarget destination, PortalOrientation orientation,
                                             PortalAperture aperture, SpaceProbe space) {
        if (orientation != PortalOrientation.TOP && orientation != PortalOrientation.BOTTOM) {
            throw new IllegalArgumentException("Horizontal exits require TOP or BOTTOM orientation");
        }

        Vec3 desiredCenter = orientation == PortalOrientation.TOP
            ? destination.position() : destination.position().add(0.0, 3.0, 0.0);
        Direction face = orientation == PortalOrientation.TOP ? Direction.UP : Direction.DOWN;
        BlockPos reference = orientation == PortalOrientation.TOP
            ? BlockPos.containing(destination.position().x, destination.position().y - 0.01,
                destination.position().z)
            : BlockPos.containing(desiredCenter.x, desiredCenter.y + 0.01, desiredCenter.z);

        if (PortalAperturePolicy.expanded(aperture)) {
            List<PortalPlacement> expanded = expandedCandidates(reference, face, destination.yaw(), space);
            if (!expanded.isEmpty()) {
                return ExpandedPortalCandidateSelector.choose(
                    expanded, desiredCenter, destination.position());
            }
        }

        if (orientation == PortalOrientation.TOP && !space.hasTopSupport(reference)) {
            return verticalFallback(destination);
        }
        PortalPlacement standard = orientation == PortalOrientation.TOP
            ? new PortalPlacement(new Vec3(reference.getX() + 0.5,
                reference.getY() + 1.0 + SURFACE_OFFSET, reference.getZ() + 0.5),
                orientation, PortalGeometry.HORIZONTAL, destination.yaw(), null, null)
            : new PortalPlacement(desiredCenter, orientation, PortalGeometry.HORIZONTAL,
                destination.yaw(), null, null);
        return space.available(standard) ? standard : verticalFallback(destination);
    }

    private static List<PortalPlacement> expandedCandidates(BlockPos reference, Direction face,
                                                             float yaw, SpaceProbe space) {
        List<PortalPlacement> candidates = new ArrayList<>(4);
        PortalOrientation orientation = face == Direction.UP ? PortalOrientation.TOP : PortalOrientation.BOTTOM;
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        for (int xOffset = -1; xOffset <= 0; xOffset++) {
            for (int zOffset = -1; zOffset <= 0; zOffset++) {
                BlockPos origin = reference.offset(xOffset, 0, zOffset);
                Vec3 center = Vec3.atCenterOf(origin)
                    .add(0.5, 0.0, 0.5)
                    .add(normal.scale(0.5 + SURFACE_OFFSET));
                PortalPlacement placement = new PortalPlacement(center, orientation,
                    PortalAperturePolicy.horizontal(), yaw, null, null);
                if (space.available(placement)) candidates.add(placement);
            }
        }
        return candidates;
    }

    private static PortalPlacement verticalFallback(PortalExitTarget destination) {
        Vec3 normal = Vec3.directionFromRotation(0.0F, destination.yaw()).normalize();
        Vec3 center = destination.position().subtract(normal.scale(0.85))
            .add(0.0, PortalGeometry.SURFACE_VERTICAL.height() * 0.5, 0.0);
        return new PortalPlacement(center, PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL,
            destination.yaw(), null, null);
    }

    interface SpaceProbe {
        boolean available(PortalPlacement placement);

        boolean hasTopSupport(BlockPos support);
    }

    private PortalExitPlacementPolicy() {}
}

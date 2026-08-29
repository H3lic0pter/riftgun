package dev.riftgun.service;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

public final class PortalSupportArea {
    public static boolean hasFullExpandedSupport(BlockGetter level, PortalPlacement placement) {
        BlockPos origin = placement.anchor();
        Direction face = placement.anchorFace();
        if (origin == null || face == null || !placement.geometry().expanded()) return false;

        if (placement.orientation() == PortalOrientation.VERTICAL
            && placement.geometry() == PortalGeometry.SURFACE_EXPANDED) {
            Direction lateral = face.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
            return fullFace(level, origin, face)
                && fullFace(level, origin.relative(lateral), face)
                && fullFace(level, origin.above(), face)
                && fullFace(level, origin.relative(lateral).above(), face);
        }

        if (placement.geometry() == PortalGeometry.HORIZONTAL_EXPANDED) {
            return fullFace(level, origin, face)
                && fullFace(level, origin.east(), face)
                && fullFace(level, origin.south(), face)
                && fullFace(level, origin.east().south(), face);
        }
        return false;
    }

    static boolean fullFace(BlockGetter level, BlockPos position, Direction face) {
        return Block.isFaceFull(level.getBlockState(position).getCollisionShape(level, position), face);
    }

    private PortalSupportArea() {}
}

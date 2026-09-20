package dev.riftgun.service;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

public final class PortalSupportArea {
    /** Adaptive size for an already validated anchor: a horizontal 1x1 face or a vertical 1x2 pair. */
    static boolean prefersStandardAperture(BlockPos anchor, Direction face,
                                           java.util.function.Predicate<BlockPos> supported) {
        if (!face.getAxis().isVertical()) return isVerticalPair(anchor, face, supported);
        // These eight cells cover every 2x2 footprint containing the clicked top/bottom face.
        // Blocks above or below the support do not enlarge its horizontal footprint.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if ((x != 0 || z != 0) && supported.test(anchor.offset(x, 0, z))) return false;
            }
        }
        return true;
    }

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

    /** Recognizes a vertical pair of exposed backing blocks, excluding wider/L-shaped support. */
    public static boolean isVerticalPair(BlockPos anchor, Direction face,
                                         java.util.function.Predicate<BlockPos> supported) {
        // The floor below a pillar is solid too, but its covered side is not part of the
        // visible support surface. Count only blocks whose outward neighbor is clear.
        java.util.function.Predicate<BlockPos> exposed = position -> supported.test(position)
            && !supported.test(position.relative(face));
        if (face.getAxis().isVertical() || !exposed.test(anchor)) return false;
        boolean above = exposed.test(anchor.above());
        boolean below = exposed.test(anchor.below());
        if (above == below) return false;
        BlockPos bottom = above ? anchor : anchor.below();
        if (exposed.test(bottom.below()) || exposed.test(bottom.above(2))) return false;
        Direction lateral = face.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        for (int y = 0; y < 2; y++) {
            BlockPos block = bottom.above(y);
            if (exposed.test(block.relative(lateral)) || exposed.test(block.relative(lateral.getOpposite()))) return false;
        }
        return true;
    }

    /** Keep geometry and the actual clicked anchor together for partially supported portals. */
    public static PortalPlacement allowOverhang(PortalPlacement candidate, BlockPos anchor) {
        return new PortalPlacement(candidate.center(), candidate.orientation(),
            candidate.orientation() == PortalOrientation.VERTICAL
                ? PortalGeometry.SURFACE_OVERHANG : PortalGeometry.HORIZONTAL_OVERHANG,
            candidate.yaw(), anchor.immutable(), candidate.anchorFace());
    }

    /** Score the candidate's four backing cells before its anchor is changed to the clicked block. */
    static int expandedBackingBlocks(BlockPos origin, Direction face,
                                     java.util.function.ToIntFunction<BlockPos> backingBlocks) {
        Direction across = face.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        Direction along = face.getAxis().isVertical() ? Direction.EAST : Direction.UP;
        return backingBlocks.applyAsInt(origin)
            + backingBlocks.applyAsInt(origin.relative(across))
            + backingBlocks.applyAsInt(origin.relative(along))
            + backingBlocks.applyAsInt(origin.relative(across).relative(along));
    }

    static boolean fullFace(BlockGetter level, BlockPos position, Direction face) {
        return Block.isFaceFull(level.getBlockState(position).getCollisionShape(level, position), face);
    }

    private PortalSupportArea() {}
}

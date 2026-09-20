package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;
import java.util.Set;

final class SurfaceFacePlacementPlannerTest {
    private static final BlockPos ANCHOR = new BlockPos(4, 63, -2);
    private static final AABB PLAYER = new AABB(0, 62, 0, 1, 64, 1);

    @Test
    void adaptiveSingleBlockTopAndBottomUseOneByOneDoors() {
        for (Direction face : new Direction[] {Direction.UP, Direction.DOWN}) {
            var result = onSupport(face, PortalAperture.EXPANDED_ADAPTIVE, Set.of(ANCHOR), false);
            assertTrue(result.successful());
            assertEquals(PortalGeometry.HORIZONTAL, result.placement().geometry());
            assertEquals(ANCHOR, result.placement().anchor());
            assertEquals(face, result.placement().anchorFace());
            assertEquals(face == Direction.UP ? PortalOrientation.TOP : PortalOrientation.BOTTOM,
                result.placement().orientation());
            assertEquals(ANCHOR.getX() + 0.5, result.placement().center().x, 1.0E-9);
            assertEquals(ANCHOR.getZ() + 0.5, result.placement().center().z, 1.0E-9);
        }
    }

    @Test
    void adaptiveHorizontalNeighborsKeepExistingExpandedSelection() {
        for (Direction face : new Direction[] {Direction.UP, Direction.DOWN}) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    var result = onSupport(face, PortalAperture.EXPANDED_ADAPTIVE,
                        Set.of(ANCHOR, ANCHOR.offset(x, 0, z)), false);
                    assertTrue(result.successful());
                    assertEquals(PortalGeometry.HORIZONTAL_OVERHANG, result.placement().geometry());
                }
            }
        }
    }

    @Test
    void preferLargeStillExpandsOnSingleBlockAndAdaptiveStillRejectsObstruction() {
        for (Direction face : new Direction[] {Direction.UP, Direction.DOWN}) {
            var large = onSupport(face, PortalAperture.EXPANDED_PREFER_LARGE, Set.of(ANCHOR), false);
            assertEquals(PortalGeometry.HORIZONTAL_OVERHANG, large.placement().geometry());
            var fullSupport = onSupport(face, PortalAperture.EXPANDED, Set.of(ANCHOR), false);
            assertEquals(PortalGeometry.HORIZONTAL, fullSupport.placement().geometry());
            var blocked = onSupport(face, PortalAperture.EXPANDED_ADAPTIVE, Set.of(ANCHOR), true);
            assertFalse(blocked.successful());
            assertEquals("message.riftgun.surface_obstructed", blocked.errorKey());
        }
    }

    @Test
    void adaptiveSideFacesKeepSingleBlockExpansionAndVerticalPairAdaptation() {
        var single = onSupport(Direction.NORTH, PortalAperture.EXPANDED_ADAPTIVE, Set.of(ANCHOR), false);
        assertEquals(PortalGeometry.SURFACE_OVERHANG, single.placement().geometry());
        var pair = onSupport(Direction.NORTH, PortalAperture.EXPANDED_ADAPTIVE,
            Set.of(ANCHOR, ANCHOR.above()), false);
        assertEquals(PortalGeometry.SURFACE_VERTICAL, pair.placement().geometry());
    }

    private static SurfaceFacePlacementPlanner.Result onSupport(Direction face, PortalAperture aperture,
                                                                Set<BlockPos> support, boolean obstructed) {
        return SurfaceFacePlacementPlanner.resolve(new SurfaceFaceSelection(ANCHOR, face), aperture,
            0.0F, PLAYER, new SurfaceFacePlacementPlanner.Probe() {
                @Override public boolean anchorSolid(BlockPos position) { return support.contains(position); }
                @Override public boolean blocked(PortalPlacement placement) { return obstructed; }
                @Override public int backingBlocks(BlockPos position) { return support.contains(position) ? 1 : 0; }
                @Override public boolean expandedSupport(PortalPlacement placement) { return false; }
            }, new SurfaceFacePlacementPlanner.Validation(4.0, 8.0, true));
    }

    @Test
    void selectedTopFaceProducesAnAnchoredHorizontalPlacement() {
        SurfaceFacePlacementPlanner.Result result = SurfaceFacePlacementPlanner.resolve(
            new SurfaceFaceSelection(ANCHOR, Direction.UP), PortalAperture.STANDARD,
            35.0F, PLAYER, new ClearProbe(),
            new SurfaceFacePlacementPlanner.Validation(4.0, 8.0, true));

        assertTrue(result.successful());
        PortalPlacement placement = result.placement();
        assertEquals(ANCHOR, placement.anchor());
        assertEquals(Direction.UP, placement.anchorFace());
        assertEquals(PortalOrientation.TOP, placement.orientation());
        assertEquals(64.062, placement.center().y, 0.0001);
    }

    @Test
    void rejectsOutOfRangeOrInvisibleAnchorBeforePlanning() {
        SurfaceFaceSelection request = new SurfaceFaceSelection(ANCHOR, Direction.NORTH);

        assertFalse(SurfaceFacePlacementPlanner.resolve(request, PortalAperture.STANDARD,
            0.0F, PLAYER, new ClearProbe(),
            new SurfaceFacePlacementPlanner.Validation(9.0, 8.0, true)).successful());
        assertFalse(SurfaceFacePlacementPlanner.resolve(request, PortalAperture.STANDARD,
            0.0F, PLAYER, new ClearProbe(),
            new SurfaceFacePlacementPlanner.Validation(4.0, 8.0, false)).successful());
    }

    private static final class ClearProbe implements SurfaceFacePlacementPlanner.Probe {
        @Override public boolean anchorSolid(BlockPos position) { return true; }
        @Override public boolean blocked(PortalPlacement placement) { return false; }
        @Override public int backingBlocks(BlockPos position) { return 1; }
        @Override public boolean expandedSupport(PortalPlacement placement) { return true; }
    }
}

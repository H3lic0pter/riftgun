package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

final class SurfaceFacePlacementPlannerTest {
    private static final BlockPos ANCHOR = new BlockPos(4, 63, -2);
    private static final AABB PLAYER = new AABB(0, 62, 0, 1, 64, 1);

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

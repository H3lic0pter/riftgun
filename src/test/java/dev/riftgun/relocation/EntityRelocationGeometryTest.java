package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.portal.PortalLifecycle;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class EntityRelocationGeometryTest {
    @Test
    void visibleSquareClearsTheLargestHorizontalBoundingBoxDimensionAndCaps() {
        assertEquals(1.0F, EntityRelocationGeometry.sideLength(0.4F, 0.5F));
        assertEquals(1.4375F, EntityRelocationGeometry.sideLength(0.9F, 0.7F));
        assertTrue(EntityRelocationGeometry.sideLength(1.95F, 1.95F)
            * EntityRelocationGeometry.MINIMUM_VISIBLE_COVERAGE > 1.95F);
        assertEquals(8.0F, EntityRelocationGeometry.sideLength(20.0F, 4.0F));
    }

    @Test
    void portalVolumeSitsAboveTheEntityFeetPlane() {
        assertEquals(64.01, EntityRelocationGeometry.centerY(64.0, 0.02), 0.00001);
    }

    @Test
    void playerDestinationExitIsOneBlockAboveTheLiveBoundingBox() {
        Vec3 center = EntityRelocationGeometry.playerDestinationExitCenter(
            new Vec3(12.5, 64.0, -3.25), 65.8);

        assertEquals(12.5, center.x);
        assertEquals(66.8, center.y);
        assertEquals(-3.25, center.z);
    }

    @Test
    void bottomExitUsesThreeBlocksUnlessATallEntityNeedsMoreClearance() {
        Vec3 destination = new Vec3(5.0, 70.0, -2.0);
        Vec3 normalExit = EntityRelocationGeometry.savedDestinationBottomExitCenter(
            destination, 1.8);
        assertEquals(73.0, normalExit.y);
        assertEquals(71.05,
            EntityRelocationGeometry.bottomOutputPosition(normalExit, 1.8).y, 0.00001);

        Vec3 tallExit = EntityRelocationGeometry.savedDestinationBottomExitCenter(
            destination, 4.0);
        assertEquals(74.35, tallExit.y, 0.00001);
        assertEquals(70.2,
            EntityRelocationGeometry.bottomOutputPosition(tallExit, 4.0).y, 0.00001);
    }

    @Test
    void lifecycleUsesNormalPortalAnimationAndAReservableOpenHold() {
        assertFalse(EntityRelocationLifecycle.shouldDeferExit(true, false));
        assertFalse(EntityRelocationLifecycle.shouldDeferExit(false, true));
        assertTrue(EntityRelocationLifecycle.shouldDeferExit(false, false));
        assertTrue(EntityRelocationLifecycle.shouldTransit(5));
        assertFalse(EntityRelocationLifecycle.shouldTransit(6));
        assertFalse(EntityRelocationLifecycle.shouldBeginClosing(
            PortalLifecycle.Phase.OPEN, 59, 60, 0));
        assertTrue(EntityRelocationLifecycle.shouldBeginClosing(
            PortalLifecycle.Phase.OPEN, 60, 60, 0));
        assertFalse(EntityRelocationLifecycle.shouldBeginClosing(
            PortalLifecycle.Phase.OPEN, 60, 60, 1));
        assertEquals(1.0F, EntityRelocationLifecycle.visibleProgress(
            PortalLifecycle.Phase.OPEN, 20, 0.0F));
        assertEquals(0.0F, EntityRelocationLifecycle.visibleProgress(
            PortalLifecycle.Phase.CLOSED, 0, 0.0F));
    }
}

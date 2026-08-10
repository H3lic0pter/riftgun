package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class FloatingPortalBoundsTest {
    @Test
    void allowsPortalsBelowBuildHeightWhilePlayerCanStillSurvive() {
        int minimumBuildHeight = -64;

        assertTrue(FloatingPortalBounds.allows(
            new AABB(0.0, -100.0, 0.0, 1.0, -98.0, 1.0), minimumBuildHeight));
        assertFalse(FloatingPortalBounds.allows(
            new AABB(0.0, -129.0, 0.0, 1.0, -127.0, 1.0), minimumBuildHeight));
    }

    @Test
    void doesNotApplyBlockBuildCeilingToFloatingPortals() {
        assertTrue(FloatingPortalBounds.allows(
            new AABB(0.0, 400.0, 0.0, 1.0, 402.0, 1.0), -64));
    }
}

package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class FloatingPortalBoundsTest {
    @Test
    void allowsPortalsBelowBuildHeightAndVoidDamageThreshold() {
        assertTrue(FloatingPortalBounds.allows(
            new AABB(0.0, -100.0, 0.0, 1.0, -98.0, 1.0)));
        assertTrue(FloatingPortalBounds.allows(
            new AABB(0.0, -1_000.0, 0.0, 1.0, -998.0, 1.0)));
    }

    @Test
    void doesNotApplyBlockBuildCeilingToFloatingPortals() {
        assertTrue(FloatingPortalBounds.allows(
            new AABB(0.0, 400.0, 0.0, 1.0, 402.0, 1.0)));
    }

    @Test
    void rejectsCoordinatesOutsideEntityBoundsAndNonFiniteCoordinates() {
        assertFalse(FloatingPortalBounds.allows(
            new AABB(0.0, -20_000_001.0, 0.0, 1.0, -19_999_999.0, 1.0)));
        assertFalse(FloatingPortalBounds.allows(
            new AABB(29_999_999.0, 0.0, 0.0, 30_000_001.0, 2.0, 1.0)));
        assertFalse(FloatingPortalBounds.allows(
            new AABB(0.0, 0.0, 0.0, 1.0, Double.POSITIVE_INFINITY, 1.0)));
        assertFalse(FloatingPortalBounds.allows(
            new AABB(0.0, Double.NaN, 0.0, 1.0, 2.0, 1.0)));
    }
}

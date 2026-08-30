package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalPlacementPreviewCacheTest {
    @Test
    void stationaryAimReusesPredictionForTenTicks() {
        PortalPlacementPreviewCache cache = new PortalPlacementPreviewCache();
        var input = input(32, Vec3.ZERO);

        assertTrue(cache.shouldRefresh(0, input));
        cache.update(0, input, placement());

        for (int tick = 1; tick < 10; tick++) assertFalse(cache.shouldRefresh(tick, input));
        assertTrue(cache.shouldRefresh(10, input));
    }

    @Test
    void thousandBlockAimRefreshesAtMostEveryFourTicksWhileMoving() {
        PortalPlacementPreviewCache cache = new PortalPlacementPreviewCache();
        var initial = input(1000, Vec3.ZERO);
        cache.update(0, initial, placement());

        assertFalse(cache.shouldRefresh(1, input(1000, new Vec3(0.01, 0.0, 0.0))));
        assertFalse(cache.shouldRefresh(2, input(1000, new Vec3(0.02, 0.0, 0.0))));
        assertFalse(cache.shouldRefresh(3, input(1000, new Vec3(0.03, 0.0, 0.0))));
        assertTrue(cache.shouldRefresh(4, input(1000, new Vec3(0.04, 0.0, 0.0))));
    }

    @Test
    void shortAimCanRespondOnTheNextTick() {
        PortalPlacementPreviewCache cache = new PortalPlacementPreviewCache();
        cache.update(0, input(32, Vec3.ZERO), placement());

        assertTrue(cache.shouldRefresh(1, input(32, new Vec3(0.01, 0.0, 0.0))));
    }

    @Test
    void expensiveRemoteWorkBacksOffUsingMeasuredDuration() {
        PortalPlacementPreviewCache cache = new PortalPlacementPreviewCache();
        var initial = input(1000, Vec3.ZERO);
        cache.updateMeasured(0, initial, placement(), 40_000_000L);

        assertFalse(cache.shouldRefresh(19, input(1000, new Vec3(0.19, 0.0, 0.0))));
        assertTrue(cache.shouldRefresh(20, input(1000, new Vec3(0.20, 0.0, 0.0))));
    }

    @Test
    void renderedSegmentsAreReusedUntilPredictionChanges() {
        PortalPlacementPreviewCache cache = new PortalPlacementPreviewCache();
        cache.update(0, input(32, Vec3.ZERO), placement());

        var first = cache.segments();

        assertSame(first, cache.segments());
    }

    private static PortalPlacementPreviewCache.Input input(int range, Vec3 eye) {
        return new PortalPlacementPreviewCache.Input(
            eye, new Vec3(0.0, 0.0, 1.0), range, PortalAperture.STANDARD, 0.0F, 0.0F);
    }

    private static PortalPlacement placement() {
        return new PortalPlacement(new Vec3(0.0, 1.0, 8.0), PortalOrientation.VERTICAL,
            PortalGeometry.FLOATING_VERTICAL, 0.0F, null, null);
    }
}

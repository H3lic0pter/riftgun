package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RemotePortalPlacementSearchBudgetTest {
    @Test
    void extremeConfiguredRangeHasABoundedCoarseSearch() {
        double maximumConfiguredRange = 32.0 + 9.0 * 1024.0;
        double step = RemotePortalPlacementResolver.coarseStep(maximumConfiguredRange);

        assertTrue(step > 9.0);
        assertTrue(Math.ceil((maximumConfiguredRange - 1.5) / step) <= 1024.0);
    }

    @Test
    void normalRangesRetainQuarterBlockPrecision() {
        assertEquals(0.25, RemotePortalPlacementResolver.coarseStep(80.0));
    }
}

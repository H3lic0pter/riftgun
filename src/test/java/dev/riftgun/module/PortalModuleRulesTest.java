package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalModuleRulesTest {
    @Test
    void aggregatesCapacityAndSurfaceRange() {
        PortalModuleRules rules = new PortalModuleRules(8000, 8000, 2, 32, 16, 3, 2, 45, 15);

        assertEquals(8000, rules.capacityFor(0));
        assertEquals(24000, rules.capacityFor(2));
        assertEquals(32, rules.maximumSurfaceRangeFor(0));
        assertEquals(80, rules.maximumSurfaceRangeFor(3));
        assertEquals(105, rules.maximumPortalDurationSeconds(2));
    }

    @Test
    void sanitizesInvalidServerValuesAndSaturatesOverflow() {
        PortalModuleRules rules = new PortalModuleRules(0, 0, -1, 0, 0, -1, -1, 0, 0);

        assertEquals(1, rules.baseCapacity());
        assertEquals(1, rules.reservoirBonus());
        assertEquals(0, rules.maximumReservoirModules());
        assertEquals(1, rules.baseSurfaceRange());
        assertEquals(1, rules.surfaceRangeBonus());
        assertEquals(0, rules.maximumSurfaceRangeModules());
        assertEquals(1, rules.basePortalDurationSeconds());

        PortalModuleRules huge = new PortalModuleRules(Integer.MAX_VALUE, Integer.MAX_VALUE, 9,
            Integer.MAX_VALUE, Integer.MAX_VALUE, 9, 9, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, huge.capacityFor(9));
        assertEquals(Integer.MAX_VALUE, huge.maximumSurfaceRangeFor(9));
    }
}

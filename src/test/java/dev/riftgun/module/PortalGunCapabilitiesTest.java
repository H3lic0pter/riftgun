package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalGunCapabilitiesTest {
    @Test
    void configuredSurfaceRangeCanBeLowerThanTheBaseCapability() {
        assertEquals(1, PortalGunCapabilities.configuredSurfaceRange(1, 80));
        assertEquals(24, PortalGunCapabilities.configuredSurfaceRange(24, 80));
        assertEquals(80, PortalGunCapabilities.configuredSurfaceRange(120, 80));
    }

    @Test
    void smartRangeDependsOnCapabilityMaximumNotConfiguredSurfaceRange() {
        int configuredSurfaceRange = PortalGunCapabilities.configuredSurfaceRange(12, 80);
        int smartRange = PortalGunCapabilities.configuredSmartDistance(64, 80);

        assertEquals(12, configuredSurfaceRange);
        assertEquals(64, smartRange);
    }
}

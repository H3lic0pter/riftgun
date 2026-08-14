package dev.riftgun.core.fuel;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class PortalFluidContentTest {
    @Test
    void rejectsInvalidValuesWithoutBootstrappingAFluidRegistry() {
        assertThrows(IllegalArgumentException.class,
            () -> new PortalFluidContent(null, -1));
        assertThrows(NullPointerException.class,
            () -> new PortalFluidContent(null, 0));
    }
}

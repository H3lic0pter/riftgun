package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalEntityAccessSnapshotTest {
    @Test
    void encodesStableCategoryMask() {
        assertEquals(0, PortalEntityAccessSnapshot.NONE.mask());
        assertEquals(1, new PortalEntityAccessSnapshot(true, false, false).mask());
        assertEquals(2, new PortalEntityAccessSnapshot(false, true, false).mask());
        assertEquals(4, new PortalEntityAccessSnapshot(false, false, true).mask());
        assertEquals(7, new PortalEntityAccessSnapshot(true, true, true).mask());
        assertEquals(8, new PortalEntityAccessSnapshot(false, false, false, true).mask());
    }
}

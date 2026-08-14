package dev.riftgun.core.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalGunVisualSnapshotTest {
    @Test
    void encodesLayersColorsAndGeometryWithoutRendererTypes() {
        PortalGunVisualSnapshot snapshot = PortalGunVisualSnapshot.create(4, true, 0x4FCB72);

        assertEquals(11, snapshot.geometryKey());
        assertTrue(snapshot.includesTint(4));
        assertFalse(snapshot.includesTint(3));
        assertTrue(snapshot.includesTint(9));
        assertEquals(0xFF4FCB72, snapshot.color(4));
        assertEquals(PortalGunVisualSnapshot.HIDDEN, snapshot.color(3));
    }
}

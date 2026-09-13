package dev.riftgun.core.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalGunVisualSnapshotTest {
    @Test
    void modeSlotsStayVisibleAndLeavePaletteSelectionToTheSkin() {
        for (int liquid : new int[] {0, 2, 3, 4, 5, 6, 7, 8}) {
            for (boolean core : new boolean[] {false, true}) {
                for (int fuel : new int[] {0, 0xFFFFFF, 0x4FCB72}) {
                    var snapshot = PortalGunVisualSnapshot.create(liquid, core, fuel);
                    assertEquals(-1, snapshot.color(40));
                    assertEquals(-1, snapshot.color(41));
                    assertTrue(snapshot.includesTint(40));
                    assertTrue(snapshot.includesTint(41));
                    assertTrue(PortalGunVisualSnapshot.includesTint(snapshot.geometryKey(), 40));
                    assertTrue(PortalGunVisualSnapshot.includesTint(snapshot.geometryKey(), 41));
                }
            }
        }
    }

    @Test
    void encodesLayersColorsAndGeometryWithoutRendererTypes() {
        PortalGunVisualSnapshot snapshot = PortalGunVisualSnapshot.create(4, true, 0x4FCB72);

        assertEquals(11, snapshot.geometryKey());
        assertTrue(snapshot.includesTint(4));
        assertFalse(snapshot.includesTint(3));
        assertTrue(snapshot.includesTint(9));
        assertTrue(snapshot.includesTint(PortalGunVisualSnapshot.FUEL_ACCENT_TINT));
        assertTrue(snapshot.includesTint(PortalGunVisualSnapshot.ZERO_POINT_MARKER_TINT));
        assertEquals(0xFF4FCB72, snapshot.color(4));
        assertEquals(0xFF4FCB72, snapshot.color(PortalGunVisualSnapshot.FUEL_ACCENT_TINT));
        assertEquals(-1, snapshot.color(PortalGunVisualSnapshot.ZERO_POINT_MARKER_TINT));
        assertEquals(PortalGunVisualSnapshot.HIDDEN, snapshot.color(3));

        PortalGunVisualSnapshot finite = PortalGunVisualSnapshot.create(4, false, 0x4FCB72);
        assertFalse(finite.includesTint(PortalGunVisualSnapshot.ZERO_POINT_MARKER_TINT));
        assertEquals(PortalGunVisualSnapshot.HIDDEN,
            finite.color(PortalGunVisualSnapshot.ZERO_POINT_MARKER_TINT));
    }
}

package dev.riftgun.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalConfigLayoutTest {
    @Test
    void gunPageIncludesWrappedDescriptionHeight() {
        var base = PortalConfigLayout.modalBox(
            PortalConfigPage.SMART_DISTANCE_SETTINGS, 500, 400, 380, 0);
        var wrapped = PortalConfigLayout.modalBox(
            PortalConfigPage.SMART_DISTANCE_SETTINGS, 500, 400, 380, 18);
        assertEquals(base.height() + 18, wrapped.height());
    }

    @Test
    void dropdownStaysInsideModal() {
        var modal = new PortalConfigLayout.Box(50, 30, 300, 180);
        var dropdown = PortalConfigLayout.dropdownBox(modal, 70, 40, 120, 20, 7);
        assertTrue(dropdown.y() >= modal.y() + 3);
        assertTrue(dropdown.y() + dropdown.height() <= modal.y() + modal.height() - 3);
        assertEquals(7 * PortalConfigLayout.ROW_HEIGHT + 4, dropdown.height());
    }

    @Test
    void scrollbarEndpointsMatchTrack() {
        int thumb = PortalConfigLayout.scrollbarThumbHeight(10, 110, 300, 100);
        assertEquals(12, PortalConfigLayout.scrollbarThumbY(10, 110, 0, 300, 100));
        assertEquals(108 - thumb,
            PortalConfigLayout.scrollbarThumbY(10, 110, 200, 300, 100));
    }
}

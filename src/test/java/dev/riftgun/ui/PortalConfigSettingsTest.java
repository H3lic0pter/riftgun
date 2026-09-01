package dev.riftgun.ui;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPlayerSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class PortalConfigSettingsTest {
    @Test
    void eachToggleChangesOnlyItsOwnFlag() {
        PortalPlayerSettings defaults = PortalPlayerSettings.defaults();
        for (PortalConfigSettings.Toggle toggle : PortalConfigSettings.Toggle.values()) {
            PortalPlayerSettings changed = PortalConfigSettings.toggle(defaults, toggle);
            assertEquals(toggle == PortalConfigSettings.Toggle.SAFETY_CHECK,
                !changed.safetyCheckEnabled());
            assertEquals(toggle == PortalConfigSettings.Toggle.CONFIRM_DELETION,
                !changed.confirmDeletion());
            assertEquals(toggle == PortalConfigSettings.Toggle.CONFIRM_DISCARD,
                !changed.confirmDiscardedChanges());
            assertEquals(toggle == PortalConfigSettings.Toggle.CONFIRM_CLEAR_FLUID,
                !changed.confirmClearFluid());
            assertEquals(toggle == PortalConfigSettings.Toggle.ANIMATIONS,
                !changed.animationsEnabled());
            assertEquals(toggle == PortalConfigSettings.Toggle.SOUNDS,
                !changed.soundsEnabled());
        }
    }

    @Test
    void placementCycleSkipsUnavailableModuleModes() {
        PortalPlayerSettings atFront = PortalPlayerSettings.defaults()
            .withPlacementMode(PortalPlacementMode.FRONT);
        assertEquals(PortalPlacementMode.SURFACE,
            PortalConfigSettings.cyclePlacementMode(atFront, false, false).placementMode());

        PortalPlayerSettings atSurface = atFront.withPlacementMode(PortalPlacementMode.SURFACE);
        assertEquals(PortalPlacementMode.SMART,
            PortalConfigSettings.cyclePlacementMode(atSurface, false, true).placementMode());
        assertFalse(PortalConfigSettings.cyclePlacementMode(atFront, false, true)
            .placementMode() == PortalPlacementMode.ENTITY_RELOCATION);
    }
}

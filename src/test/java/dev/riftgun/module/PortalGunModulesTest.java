package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalGunModulesTest {
    @Test
    void expansionModulesUnlockThreeSlotsUpToThreeRows() {
        assertEquals(9, PortalGunModules.slotCountForExpansionModules(0));
        assertEquals(12, PortalGunModules.slotCountForExpansionModules(1));
        assertEquals(18, PortalGunModules.slotCountForExpansionModules(3));
        assertEquals(27, PortalGunModules.slotCountForExpansionModules(6));
        assertEquals(27, PortalGunModules.slotCountForExpansionModules(99));
    }

    @Test
    void negativeExpansionCountsCannotReduceTheBaseRow() {
        assertEquals(9, PortalGunModules.slotCountForExpansionModules(-1));
    }
}

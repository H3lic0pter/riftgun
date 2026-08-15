package dev.riftgun.fuel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import org.junit.jupiter.api.Test;

final class PortalFuelManagerTest {
    @Test
    void zeroPointModuleMakesTheLoadedFuelProfileUnlimited() {
        //? if >=1.21.11 {
        /*PortalFuelProfile portalFluid = new PortalFuelProfile(
            Identifier.fromNamespaceAndPath("riftgun", "portal_fluid"),
            0x58BFFF, false, 5, 8);
        *///?} else {
        PortalFuelProfile portalFluid = new PortalFuelProfile(
            ResourceLocation.fromNamespaceAndPath("riftgun", "portal_fluid"),
            0x58BFFF, false, 5, 8);
        //?}

        PortalFuelUse use = PortalFuelManager.selectLoadedFuel(portalFluid, 8, true);

        assertEquals(portalFluid, use.profile());
        assertEquals(0, use.amount());
        assertTrue(use.virtual(), "loaded fuel should not be consumed while the module is active");

        PortalFuelUse ordinaryUse = PortalFuelManager.selectLoadedFuel(portalFluid, 8, false);
        assertEquals(8, ordinaryUse.amount());
        assertFalse(ordinaryUse.virtual());
    }
}

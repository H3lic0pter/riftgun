package dev.riftgun.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class PortalConfigPresentationTest {
    @Test
    void formatsFluidAmountsCompactly() {
        assertEquals("999", PortalConfigPresentation.shortFluidAmount(999));
        assertEquals("1k", PortalConfigPresentation.shortFluidAmount(1_000));
        assertEquals("1.5k", PortalConfigPresentation.shortFluidAmount(1_500));
    }

    @Test
    void buildsStableFluidTranslationKeys() {
        assertEquals("screen.riftgun.empty_fluid",
            PortalConfigPresentation.fluidTranslationKey(""));
        assertEquals("fluid.minecraft.water",
            PortalConfigPresentation.fluidTranslationKey("water"));
        assertEquals("fluid.riftgun.zero_point",
            PortalConfigPresentation.fluidTranslationKey("riftgun:zero_point"));
    }

    @Test
    void descriptionExistsOnlyForNestedGunPages() {
        assertEquals("screen.riftgun.remote.settings_hint",
            PortalConfigPresentation.gunSettingDescriptionKey(
                PortalConfigPage.REMOTE_SETTINGS));
        assertNull(PortalConfigPresentation.gunSettingDescriptionKey(
            PortalConfigPage.SETTINGS));
    }
}

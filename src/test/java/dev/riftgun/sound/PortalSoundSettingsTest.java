package dev.riftgun.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class PortalSoundSettingsTest {
    @Test
    void builtInChoicesAreIndependentPerChannel() {
        assertEquals(2, PortalSoundRegistry.values(PortalSoundChannel.SHOT).size());
        assertEquals(2, PortalSoundRegistry.values(PortalSoundChannel.PORTAL).size());
        assertEquals(3, PortalSoundRegistry.values(PortalSoundChannel.TRANSIT).size());
        assertTrue(PortalSoundRegistry.values(PortalSoundChannel.TRANSIT).stream()
            .anyMatch(choice -> choice.id().equals(PortalSoundRegistry.ENDER_ID)));
        assertFalse(PortalSoundRegistry.values(PortalSoundChannel.SHOT).stream()
            .anyMatch(choice -> choice.id().equals(PortalSoundRegistry.ENDER_ID)));
    }

    @Test
    void unknownChoiceFallsBackToRiftAndCyclingWraps() {
        ResourceLocation missing = ResourceLocation.fromNamespaceAndPath("example", "missing");
        assertEquals(PortalSoundRegistry.RIFT_ID,
            PortalSoundRegistry.normalize(PortalSoundChannel.SHOT, missing));
        assertEquals(PortalSoundRegistry.NONE_ID,
            PortalSoundRegistry.cycle(PortalSoundChannel.SHOT, PortalSoundRegistry.RIFT_ID, 1));
        assertEquals(PortalSoundRegistry.RIFT_ID,
            PortalSoundRegistry.cycle(PortalSoundChannel.SHOT, PortalSoundRegistry.NONE_ID, -1));
    }

    @Test
    void settingsAndOpenSnapshotRoundTrip() {
        PortalSoundSettings settings = new PortalSoundSettings(
            PortalSoundRegistry.NONE_ID, PortalSoundRegistry.RIFT_ID,
            PortalSoundRegistry.ENDER_ID, true);
        assertEquals(settings, PortalSoundSettings.load(settings.save()));

        PortalSoundSnapshot snapshot = PortalSoundSnapshot.from(settings);
        assertEquals(snapshot, PortalSoundSnapshot.load(snapshot.save()));
        assertEquals(PortalSoundRegistry.NONE_ID, snapshot.shot());
        assertTrue(snapshot.splashEnabled());
    }

    @Test
    void legacyTagsUseRiftThemesWithSplashDisabled() {
        PortalSoundSettings defaults = PortalSoundSettings.defaults();
        assertEquals(PortalSoundRegistry.RIFT_ID, defaults.shot());
        assertEquals(PortalSoundRegistry.RIFT_ID, defaults.portal());
        assertEquals(PortalSoundRegistry.RIFT_ID, defaults.transit());
        assertFalse(defaults.splashEnabled());
        assertEquals(defaults, PortalSoundSettings.load(new CompoundTag()));
        assertEquals(PortalSoundSnapshot.defaults(), PortalSoundSnapshot.load(new CompoundTag()));
    }
}

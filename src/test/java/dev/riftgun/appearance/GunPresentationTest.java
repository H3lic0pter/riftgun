package dev.riftgun.appearance;

import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.sound.PortalSoundRegistry;
import dev.riftgun.sound.PortalSoundSettings;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class GunPresentationTest {
    @Test
    void choicesRoundTripWithoutRendererOrRecoilParameters() {
        var value = new GunPresentation("riftgun:endframe", GunShotAnimation.LOWER,
            new PortalSoundSettings(PortalSoundRegistry.APERTURE_ISH_ID, PortalSoundRegistry.RIFT_ID,
                PortalSoundRegistry.ENDER_ID, true), true);
        var encoded = GunPresentation.CODEC.encodeStart(NbtOps.INSTANCE, value).getOrThrow();
        assertEquals(value, GunPresentation.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
        assertEquals(4, value.save().size());
        for (String key : new String[] {"Visual", "Animation", "Sounds", "Initialized"}) {
            assertTrue(value.save().contains(key));
        }
    }

    @Test
    void pendingMarkerSurvivesSavingAndEditsPreserveOtherCategories() {
        assertFalse(GunPresentation.load(GunPresentation.NEW.save()).initialized());
        var source = GunPresentation.DEFAULT.withVisual("pack:custom");
        var edited = source.withAnimation(GunShotAnimation.SWING);
        assertEquals("pack:custom", edited.visual());
        assertEquals(source.sounds(), edited.sounds());
        assertEquals(GunShotAnimation.RECOIL, source.animation());
    }
}

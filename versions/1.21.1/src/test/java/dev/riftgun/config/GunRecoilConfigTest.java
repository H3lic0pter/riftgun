package dev.riftgun.config;

import static org.junit.jupiter.api.Assertions.*;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import dev.riftgun.core.config.ClientVisualConfig;
import dev.riftgun.core.config.GunRecoilConfig;
import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.core.config.RiftConfigs;
import net.neoforged.fml.config.IConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class GunRecoilConfigTest {
    private final ClientVisualConfig previous = RiftConfigs.client();

    @AfterEach
    void restoreSnapshot() {
        ClientConfig.SPEC.acceptConfig(null);
        RiftConfigs.publishClient(previous);
    }

    private static void load(CommentedConfig config) throws ReflectiveOperationException {
        ClientConfig.SPEC.correct(config);
        // NeoForge seals ILoadedConfig to its package-private record. Use that real
        // record with in-memory TOML; correction above avoids its disk-save path.
        var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
            .getDeclaredConstructor(CommentedConfig.class, java.nio.file.Path.class,
                net.neoforged.fml.config.ModConfig.class);
        constructor.setAccessible(true);
        var loaded = (IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null);
        ClientConfig.SPEC.acceptConfig(loaded);
        ClientConfig.publishSnapshot();
    }

    @Test
    void missingParametersGetNewDefaultsWithoutChangingTheOffSelection() throws ReflectiveOperationException {
        load(new TomlParser().parse("""
            [visuals.gun]
            animation = "OFF"
            """));
        assertEquals(GunShotAnimation.OFF, RiftConfigs.client().gunAnimation());
        assertEquals(GunRecoilConfig.defaults(), RiftConfigs.client().gunRecoil());
        assertEquals(400, RiftConfigs.client().gunRecoil().recoveryMillis());
        assertEquals(0.15, RiftConfigs.client().gunRecoil().maxBackwardOffset());
        assertEquals(10.0, RiftConfigs.client().gunRecoil().maxPitchDegrees());
    }

    @Test
    void tomlValuesAndReloadReachThePublishedSnapshot() throws ReflectiveOperationException {
        var config = new TomlParser().parse("""
            [visuals.gun]
            animation = "RECOIL"
            kickMillis = 50
            recoveryMillis = 250
            shotStrength = 0.85
            maxBackwardOffset = 0.12
            maxPitchDegrees = 6.0
            useEquipRecoveryMillis = 300
            """);
        load(config);
        var first = RiftConfigs.client().gunRecoil();
        assertEquals(new GunRecoilConfig(50, 250, 0.85, 0.12, 6.0, 300), first);

        config.set("visuals.gun.recoveryMillis", 400);
        config.set("visuals.gun.maxPitchDegrees", 8.0);
        config.set("visuals.gun.animation", "SWING");
        ClientConfig.SPEC.afterReload();
        ClientConfig.publishSnapshot();
        assertEquals(new GunRecoilConfig(50, 400, 0.85, 0.12, 8.0, 300),
            RiftConfigs.client().gunRecoil());
        assertEquals(250, first.recoveryMillis(), "An existing shot keeps its immutable snapshot");
        assertEquals(8.0, RiftConfigs.client().gunRecoil().maxPitchDegrees());
        assertEquals(GunShotAnimation.SWING, RiftConfigs.client().gunAnimation());
    }

    @Test
    void guiCyclesAllThreeStatesAndSavesTheCanonicalSelection() throws ReflectiveOperationException {
        var config = new TomlParser().parse("");
        load(config);
        assertEquals(GunShotAnimation.RECOIL, RiftConfigs.client().gunAnimation());
        for (var expected : new GunShotAnimation[] {
                GunShotAnimation.SWING, GunShotAnimation.OFF, GunShotAnimation.RECOIL}) {
            ClientConfig.VALUES.gunAnimation.set(ClientConfig.VALUES.gunAnimation.get().next());
            ClientConfig.publishSnapshot();
            assertEquals(expected, RiftConfigs.client().gunAnimation());
            assertEquals(expected.name(), config.get("visuals.gun.animation").toString());
        }
        config.set("visuals.gun.animation", "OFF");
        ClientConfig.SPEC.afterReload();
        ClientConfig.publishSnapshot();
        assertEquals(GunShotAnimation.OFF, RiftConfigs.client().gunAnimation());
    }

    @Test
    void invalidFileValuesAreCorrectedBeforeRenderingCanUseThem() throws ReflectiveOperationException {
        var config = new TomlParser().parse("""
            [visuals.gun]
            kickMillis = 0
            recoveryMillis = -1
            shotStrength = 2.0
            maxBackwardOffset = -0.3
            maxPitchDegrees = 99.0
            useEquipRecoveryMillis = -5
            """);
        assertFalse(ClientConfig.SPEC.isCorrect(config));
        load(config);
        var corrected = RiftConfigs.client().gunRecoil();
        assertTrue(ClientConfig.SPEC.isCorrect(config));
        assertTrue(corrected.kickMillis() > 0);
        assertTrue(corrected.recoveryMillis() > 0);
        assertTrue(corrected.shotStrength() >= 0.0 && corrected.shotStrength() <= 1.0);
        assertTrue(corrected.maxBackwardOffset() >= 0.0);
        assertTrue(corrected.maxPitchDegrees() >= 0.0 && corrected.maxPitchDegrees() <= 45.0);
        assertTrue(corrected.useEquipRecoveryMillis() >= 0);
    }
}

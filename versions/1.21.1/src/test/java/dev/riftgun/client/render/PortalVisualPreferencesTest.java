package dev.riftgun.client.render;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import dev.riftgun.config.ClientConfig;
import dev.riftgun.core.config.ClientVisualConfig;
import dev.riftgun.core.config.RiftConfigs;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.fml.config.IConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

final class PortalVisualPreferencesTest {
    private final ClientVisualConfig previous = RiftConfigs.client();

    @AfterEach
    void restore() {
        ClientConfig.SPEC.acceptConfig(null);
        RiftConfigs.publishClient(previous);
    }

    @Test
    void gunIpChoiceWaitsForCapabilityWithoutRewritingTheStoredChoice() throws Exception {
        load(new TomlParser().parse("""
            portalVisualType = "riftgun:immersive_portal"
            [appearance.recommendations]
            portalVisualSkin = "riftgun:aperture_ish"
            [appearance.presets.aperture_ish]
            portalVisual = "pack:missing"
            """));
        var ip = PortalVisualRegistry.IMMERSIVE_PORTAL_ID;
        var fallback = PortalVisualRegistry.DEFAULT_ID;
        var available = new AtomicBoolean(false);
        try (var registry = mockStatic(PortalVisualRegistry.class);
             var gun = mockStatic(dev.riftgun.client.appearance.SkinRecommendations.class)) {
            gun.when(dev.riftgun.client.appearance.SkinRecommendations::current).thenReturn(
                dev.riftgun.appearance.GunPresentation.DEFAULT.withVisual(ip.toString()));
            registry.when(() -> PortalVisualRegistry.registered(any()))
                .thenAnswer(call -> ip.equals(call.getArgument(0)) || fallback.equals(call.getArgument(0)));
            registry.when(() -> PortalVisualRegistry.contains(any()))
                .thenAnswer(call -> fallback.equals(call.getArgument(0))
                    || ip.equals(call.getArgument(0)) && available.get());
            assertEquals(fallback, PortalVisualPreferences.selectedId()); // Wait for server capability.
            available.set(true);
            assertEquals(ip, PortalVisualPreferences.selectedId());
            assertEquals("riftgun:immersive_portal", ClientConfig.VALUES.portalVisualType.get());
        }
    }

    @Test
    void missingRecommendationAndCustomVisualBothResolveToTheDefault() throws Exception {
        load(new TomlParser().parse("""
            portalVisualType = "pack:missing_custom"
            [appearance.recommendations]
            portalVisualSkin = "riftgun:default"
            [appearance.presets.default]
            portalVisual = "pack:missing_preset"
            """));
        try (var gun = mockStatic(dev.riftgun.client.appearance.SkinRecommendations.class)) {
            gun.when(dev.riftgun.client.appearance.SkinRecommendations::current).thenReturn(
                dev.riftgun.appearance.GunPresentation.DEFAULT.withVisual("pack:missing_preset"));
            assertEquals(PortalVisualRegistry.DEFAULT_ID, PortalVisualPreferences.selectedId());
        }
    }

    private static void load(CommentedConfig config) throws Exception {
        ClientConfig.SPEC.correct(config);
        var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
            .getDeclaredConstructor(CommentedConfig.class, java.nio.file.Path.class,
                net.neoforged.fml.config.ModConfig.class);
        constructor.setAccessible(true);
        ClientConfig.SPEC.acceptConfig((IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null));
        ClientConfig.publishSnapshot();
    }
}

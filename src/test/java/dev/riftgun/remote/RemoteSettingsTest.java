package dev.riftgun.remote;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RemoteSettingsTest {
    @Test
    void previewDefaultsOnForExistingGuns() {
        RemoteSettings settings = RemoteSettings.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString("{}"))
            .result().orElseThrow();

        assertTrue(settings.placementPreviewEnabled());
    }

    @Test
    void previewPreferenceRoundTripsWhenDisabled() {
        RemoteSettings disabled = RemoteSettings.defaults().withPlacementPreviewEnabled(false);
        var encoded = RemoteSettings.CODEC.encodeStart(JsonOps.INSTANCE, disabled)
            .result().orElseThrow();
        RemoteSettings decoded = RemoteSettings.CODEC.parse(JsonOps.INSTANCE, encoded)
            .result().orElseThrow();

        assertFalse(decoded.placementPreviewEnabled());
    }
}

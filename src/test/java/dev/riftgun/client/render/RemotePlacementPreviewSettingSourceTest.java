package dev.riftgun.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RemotePlacementPreviewSettingSourceTest {
    @Test
    void bothClientsGatePreviewAndExposeRoomyRemoteSettingPage() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            Path sourceRoot = Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client");
            String preview = Files.readString(sourceRoot.resolve(
                Path.of("render", "PortalPlacementPreview.java")));
            String screen = Files.readString(sourceRoot.resolve(
                Path.of("screen", "PortalConfigScreen.java")));

            assertTrue(preview.contains("placementPreviewEnabled()"));
            assertTrue(screen.contains("\"RemotePlacementPreview\", \"RemotePlacementPreviewEnabled\""));
            assertTrue(screen.contains("case REMOTE_SETTINGS -> 156;"));
        }
    }
}

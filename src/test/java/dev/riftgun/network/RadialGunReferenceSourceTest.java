package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RadialGunReferenceSourceTest {
    @Test
    void radialPreviewAndCommitStayBoundToTheServerSelectedGun() throws Exception {
        String handler = Files.readString(Path.of(
            "src/main/java/dev/riftgun/network/PortalRequestHandler.java"));
        assertTrue(handler.contains("keyboardShortcut && request.contains(\"GunReference\")"));
        assertTrue(handler.contains("PortalGunLocator.resolveReference("));

        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String state = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "PortalClientState.java"));
            String preview = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));
            assertTrue(state.contains(
                "instanceof dev.riftgun.client.screen.ModeRadialScreen"), version);
            assertTrue(preview.contains("PortalPreviewGunState.fromSnapshot(PortalClientState.gun()"), version);
        }
    }
}

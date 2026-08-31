package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPairingEntityTargetPreviewSourceTest {
    @Test
    void entityTargetMarkerIsScopedToItsGunAndEntityPairingMode() throws IOException {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            Path root = Path.of("versions", version, "src", "main", "java", "dev", "riftgun");
            String preview = Files.readString(root.resolve(
                Path.of("client", "render", "PortalPlacementPreview.java")));

            assertTrue(preview.contains("gun.pending()"), version);
            assertTrue(preview.contains("next.entityTarget()"), version);
            assertTrue(preview.contains("PortalFunctionMode.PORTAL_PAIRING"), version);
            assertTrue(preview.contains("PortalPlacementMode.ENTITY_RELOCATION"), version);
            assertTrue(preview.contains(
                "PortalPairingPreviewGeometry.entityTargetSegments("), version);
            assertFalse(preview.contains("entitiesForRendering()"), version);
        }
    }

    @Test
    void heldSneakPreviewsTheSameSurfaceThenRemoteTargetingPolicy() throws IOException {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            Path previewPath = Path.of("versions", version, "src", "main", "java", "dev",
                "riftgun", "client", "render", "PortalPlacementPreview.java");
            String preview = Files.readString(previewPath);
            int start = preview.indexOf("private static boolean tickPairingEntityTargetPreview");
            int end = preview.indexOf("private static PortalPlacement surfacePreview", start);
            String livePreview = preview.substring(start, end);

            assertTrue(livePreview.contains("minecraft.player.isShiftKeyDown()"), version);
            assertTrue(livePreview.contains("PortalPlacementMode.ENTITY_RELOCATION"), version);
            assertTrue(livePreview.contains("SurfaceFaceRequest"), version);
            assertTrue(livePreview.contains("surfacePreview("), version);
            assertTrue(livePreview.contains("updateRemotePreview("), version);
            assertTrue(livePreview.contains(
                "gun.remote() ? gun.remoteDistance() : gun.maximumSurfaceRange()"), version);
        }
    }
}

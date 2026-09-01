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
        String preview = sharedEngine();
        assertTrue(preview.contains("gun.pending()"));
        assertTrue(preview.contains("next.entityTarget()"));
        assertTrue(preview.contains("PortalFunctionMode.PORTAL_PAIRING"));
        assertTrue(preview.contains("PortalPlacementMode.ENTITY_RELOCATION"));
        assertTrue(preview.contains("PortalPairingPreviewGeometry.entityTargetSegments("));
        assertFalse(preview.contains("entitiesForRendering()"));
    }

    @Test
    void heldSneakPreviewsSurfaceThenRemoteForEntityAndSmartRoutes() throws IOException {
        String preview = sharedEngine();
        int start = preview.indexOf("private boolean tickShiftRoutedPreview");
        int end = preview.indexOf("public static boolean usesShiftRoutedPreview", start);
        String livePreview = preview.substring(start, end);

        assertTrue(livePreview.contains("input.shiftDown()"));
        assertTrue(livePreview.contains("usesShiftRoutedPreview(gun)"));
        assertTrue(livePreview.contains("gun.smartDistance()"));
        assertTrue(livePreview.contains("resolver.surfaceHit("));
        assertTrue(livePreview.contains("resolver.surface("));
        assertTrue(livePreview.contains("cache.updateSurface("));
        assertTrue(livePreview.contains("updateRemotePreview("));
        assertTrue(livePreview.contains(
            "gun.remote() ? gun.remoteDistance() : gun.maximumSurfaceRange()"));
        assertTrue(preview.contains("PortalPlacementMode.ENTITY_RELOCATION"));
        assertTrue(preview.contains("PortalPlacementMode.SMART"));
        assertTrue(preview.contains("PortalFloatingFallback.REMOTE"));
    }

    private static String sharedEngine() throws IOException {
        return Files.readString(Path.of("src", "main", "java", "dev", "riftgun", "portal",
            "PortalPlacementPreviewEngine.java"));
    }
}

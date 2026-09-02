package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPairingLightweightSourceTest {
    @Test
    void noPairingMarkerCreatesADormantPortalEntity() throws Exception {
        String manager = Files.readString(Path.of("src", "main", "java", "dev", "riftgun",
            "pairing", "PortalPairingManager.java"));
        assertFalse(manager.contains("PortalEntity.openDormant("));
        assertTrue(manager.contains("PortalPairingPendingEndpoints.save("));
        assertTrue(manager.contains("PortalPairingEndpoint.ENTITY_TARGET"));
    }

    @Test
    void clientBuildsEntityFreeVectorLinesForThePendingEndpoint() throws Exception {
        String preview = Files.readString(Path.of("src", "main", "java", "dev", "riftgun",
            "portal", "PortalPlacementPreviewEngine.java"));
        assertTrue(preview.contains("gun.pending()"));
        assertTrue(preview.contains("PortalPairingPreviewGeometry.segments("));
        assertFalse(preview.contains("new PortalEntity("));
        assertFalse(preview.contains("PortalPreviewVisualSource"));
    }
}

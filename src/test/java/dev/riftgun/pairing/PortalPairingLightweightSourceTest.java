package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPairingLightweightSourceTest {
    @Test
    void onlyEntityRelocationTargetStillCreatesDormantPortalEntity() throws Exception {
        String manager = Files.readString(Path.of("src", "main", "java", "dev", "riftgun",
            "pairing", "PortalPairingManager.java"));
        assertEquals(1, occurrences(manager, "PortalEntity.openDormant("));
        assertTrue(manager.contains("PortalPairingPendingEndpoints.save("));
        assertTrue(manager.contains("PortalPairingPendingEndpoints.clear("));
    }

    @Test
    void bothClientsRenderPendingFrameAndGlyphThroughTheSameLinePath() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String preview = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));
            assertTrue(preview.contains("PortalPairingPendingEndpoints.get(gun)"), version);
            assertTrue(preview.contains("PortalPairingPreviewGeometry.segments("), version);
            assertFalse(preview.contains("drawPendingLabel"), version);
        }
    }

    private static int occurrences(String value, String needle) {
        return value.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }
}

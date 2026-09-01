package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPairingPreviewVisibilitySourceTest {
    @Test
    void ordinaryPendingMarkerDependsOnlyOnLeavingEntityPlacementMode() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "dev", "riftgun",
            "portal", "PortalPlacementPreviewEngine.java"));
        String method = source.substring(source.indexOf("private void tickPending"),
            source.indexOf("private void clearPending"));

        assertTrue(method.contains("PortalPlacementMode.ENTITY_RELOCATION"));
        assertFalse(method.contains("PortalFunctionMode"),
            "function-mode changes must not hide an existing pending marker");
    }
}

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
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));
            String method = source.substring(source.indexOf("private static void tickPending"),
                source.indexOf("private static void clearPending"));

            assertTrue(method.contains("PortalPlacementMode.ENTITY_RELOCATION"), version);
            assertFalse(method.contains("PortalFunctionMode"),
                version + " must not hide an existing pending marker when function mode changes");
        }
    }
}

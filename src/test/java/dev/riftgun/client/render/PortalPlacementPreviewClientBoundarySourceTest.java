package dev.riftgun.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalPlacementPreviewClientBoundarySourceTest {
    @Test
    void bothPreviewAdaptersStayClientOnlyAndPacketFree() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", version, "src", "main",
                "java", "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));

            assertTrue(source.contains("value = Dist.CLIENT"));
            assertFalse(source.contains("PortalNetworking"));
            assertFalse(source.contains("PORTAL_SPLASH"));
            assertFalse(source.contains("addParticle"));
            assertFalse(source.contains("PortalEntity"));
        }
    }
}

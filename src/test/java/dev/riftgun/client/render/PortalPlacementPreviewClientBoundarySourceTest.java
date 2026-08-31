package dev.riftgun.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
            assertFalse(source.contains("SurfaceFaceRequest"),
                version + " preview hot path must use the domain selection directly");
            assertFalse(source.contains(".toSelection()"),
                version + " preview hot path must not allocate a packet-to-domain wrapper");
        }
    }

    @Test
    void modernPreviewSubmitsOneGeometryNodePerFrame() throws Exception {
        String source = Files.readString(Path.of("versions", "26.1.2", "src", "main",
            "java", "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));
        int methodStart = source.indexOf("public static void submitCustomGeometry(");
        int methodEnd = source.indexOf("private static void tickPending(", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertEquals(1, occurrences(method, ".submitCustomGeometry("));
    }

    private static int occurrences(String source, String token) {
        return source.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}

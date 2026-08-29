package dev.riftgun.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class PortalPlacementPreviewWorkloadSourceTest {
    @Test
    void previewDoesNotResolveEveryTickOrRebuildGeometryEveryFrame() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));

            assertFalse(source.contains("placement = calculate(minecraft);"),
                version + " runs the ray/collision resolver on every client tick");
            assertFalse(source.contains("PortalPlacementPreviewGeometry.corners(preview)"),
                version + " allocates preview geometry on every rendered frame");
        }
    }
}

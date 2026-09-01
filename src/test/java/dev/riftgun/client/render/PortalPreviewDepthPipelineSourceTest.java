package dev.riftgun.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalPreviewDepthPipelineSourceTest {
    @Test
    void modernPreviewMatchesTheDepthWritingLegacyLinePath() throws Exception {
        String preview = read("PortalPlacementPreview.java");

        assertTrue(preview.contains("RenderTypes.lines()"));
        assertFalse(preview.contains("RenderTypes.linesTranslucent()"));
    }

    private static String read(String file) throws Exception {
        return Files.readString(Path.of("versions", "26.1.2", "src", "main", "java",
            "dev", "riftgun", "client", "render", file));
    }
}

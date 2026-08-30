package dev.riftgun.input;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PrecisionRadialInteractionSourceTest {
    @Test
    void leftClickFunctionToggleExcludesEveryPrecisionPlacementPage() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String screen = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "screen", "ModeRadialScreen.java"));
            assertTrue(screen.contains(
                "button() == 0 && !precisionPreviewOnly && page != Page.SURFACE_FACE")
                || screen.contains(
                    "button == 0 && !precisionPreviewOnly && page != Page.SURFACE_FACE"),
                version + " must not toggle function mode from a precision radial");
        }
    }
}

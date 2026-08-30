package dev.riftgun.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void precisionPlacementCommitsOnLeftClickAndClosesOnShortcutRelease() throws Exception {
        String input = Files.readString(Path.of("src", "main", "java", "dev",
            "riftgun", "client", "ModeRadialInput.java"));
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            Path client = Path.of("versions", version, "src", "main", "java", "dev",
                "riftgun", "client");
            String screen = Files.readString(client.resolve(
                Path.of("screen", "ModeRadialScreen.java")));
            int mouseStart = screen.indexOf("boolean mouseClicked(");
            int mouseEnd = screen.indexOf("boolean mouseDragged(", mouseStart);
            String mouseClicked = screen.substring(mouseStart, mouseEnd);
            assertTrue(mouseClicked.contains("button == 0 && precisionPreviewOnly")
                || mouseClicked.contains("button() == 0 && precisionPreviewOnly"));
            assertTrue(mouseClicked.contains("commitSelection()"));
            assertFalse(mouseClicked.contains("commitAndClose()"));

        }
        assertTrue(input.contains("pendingSource == Source.PRECISION_PREVIEW"));
        assertTrue(input.contains("closePrecisionFromShortcutRelease()"));
        assertTrue(input.contains("ModeRadialClientAccess.commitAndClose(false)"));
        assertTrue(input.contains("ModeRadialClientAccess.commitAndClose(true)"));
    }
}

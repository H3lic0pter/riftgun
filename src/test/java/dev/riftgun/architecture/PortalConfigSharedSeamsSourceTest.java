package dev.riftgun.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents cross-version GUI and preview business logic from drifting back into node adapters. */
final class PortalConfigSharedSeamsSourceTest {
    private static final List<String> NODES = List.of("1.21.1", "26.1.2");

    @Test
    void screensUseSharedPageLayoutRowsAndSettings() throws IOException {
        for (String node : NODES) {
            String source = read(node, "client/screen/PortalConfigScreen.java");
            assertTrue(source.contains("PortalConfigSession"), node);
            assertTrue(source.contains("PortalConfigRows.build"), node);
            assertTrue(source.contains("PortalConfigLayout.modalBox"), node);
            assertTrue(source.contains("PortalConfigSettings."), node);
            assertTrue(source.contains("PortalConfigPresentation.*"), node);
            assertFalse(source.contains("private enum Modal"), node);
            assertFalse(source.contains("private record Row("), node);
            assertFalse(source.contains("new PortalPlayerSettings("), node);
        }
    }

    @Test
    void playerTargetControllersDelegateStateTransitions() throws IOException {
        for (String node : NODES) {
            String source = read(node, "client/screen/PlayerTargetController.java");
            assertTrue(source.contains("PortalPlayerTargetSession"), node);
            assertTrue(source.contains("session.toggleExpanded()"), node);
            assertTrue(source.contains("Minecraft.getInstance().getConnection() != null"), node);
            assertFalse(source.contains("private boolean expanded"), node);
            assertFalse(source.contains("private boolean listRequested"), node);
        }
    }

    @Test
    void previewAdaptersDelegateTickState() throws IOException {
        for (String node : NODES) {
            String source = read(node, "client/render/PortalPlacementPreview.java");
            assertTrue(source.contains("PortalPlacementPreviewEngine"), node);
            assertTrue(source.contains("ENGINE.tick("), node);
            assertFalse(source.contains("cachedPendingSegment"), node);
            assertFalse(source.contains("cachedTargetSegment"), node);
        }
    }

    private static String read(String node, String relative) throws IOException {
        return Files.readString(Path.of("versions", node, "src/main/java/dev/riftgun", relative));
    }
}

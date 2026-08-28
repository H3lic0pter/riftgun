package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalGunToggleFeedbackSourceTest {
    @Test
    void gunBooleanTogglesRefreshTheirVisibleLabelsImmediately() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", node,
                "src/main/java/dev/riftgun/client/screen/PortalConfigScreen.java"));
            String method = section(source,
                "private boolean applyGunBooleanToggle(String snapshotKey) {",
                "\n    private Component fallbackLabel");

            assertTrue(method.contains("PortalClientState.gun().putBoolean(snapshotKey, enabled);"),
                node + " must update the optimistic client snapshot");
            assertTrue(method.contains("rebuildWidgets();"),
                node + " must rebuild labels after the optimistic update");
        }
    }

    private static String section(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0 && end > start, "Expected source section was not found");
        return source.substring(start, end);
    }
}

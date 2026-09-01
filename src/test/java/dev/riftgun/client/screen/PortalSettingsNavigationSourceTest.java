package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalSettingsNavigationSourceTest {
    @Test
    void bothNodesKeepGuiTogglesOnTheTopLevelAndPortalOptionsBehindIcons()
        throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", node,
                "src/main/java/dev/riftgun/client/screen/PortalConfigScreen.java"));
            String settings = section(source,
                "} else if (session.page() == PortalConfigPage.SETTINGS) {",
                "} else if (session.page() == PortalConfigPage.CONFIRM_SETTINGS) {");
            String visuals = section(source,
                "} else if (session.page() == PortalConfigPage.VISUAL_SETTINGS) {",
                "} else if (session.page() == PortalConfigPage.SWIRL_ANIMATION_SETTINGS) {");
            String sounds = section(source,
                "} else if (session.page() == PortalConfigPage.SOUND_SETTINGS) {",
                "\n        }");

            assertTrue(settings.contains("toggleLabel(\"screen.riftgun.animations\""));
            assertTrue(settings.contains("toggleLabel(\"screen.riftgun.sounds\""));
            assertTrue(settings.contains("visualSettingsButton = button"));
            assertTrue(settings.contains("soundSettingsButton = button"));
            assertFalse(visuals.contains("toggleLabel(\"screen.riftgun.animations\""));
            assertFalse(sounds.contains("toggleLabel(\"screen.riftgun.sounds\""));
        }
    }

    private static String section(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0 && end > start, "Expected source section was not found");
        return source.substring(start, end);
    }
}

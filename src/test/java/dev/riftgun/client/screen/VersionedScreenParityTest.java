package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The Minecraft client GUI APIs differ too heavily to share the concrete Screen classes. Keep
 * behavior-facing network actions and translation contracts identical while rendering adapters
 * remain version-specific.
 */
final class VersionedScreenParityTest {
    private static final Path LEGACY = Path.of("versions/1.21.1/src/main/java/dev/riftgun/client/screen");
    private static final Path CURRENT = Path.of("versions/26.1.2/src/main/java/dev/riftgun/client/screen");
    private static final Pattern ACTION = Pattern.compile("PortalAction\\.([A-Z_]+)");
    private static final Pattern TRANSLATION = Pattern.compile("\"(screen\\.riftgun\\.[^\"]+)\"");

    @Test
    void portalConfigScreensExposeTheSameBehaviorContract() throws Exception {
        assertContractParity("PortalConfigScreen.java", ACTION);
        assertContractParity("PortalConfigScreen.java", TRANSLATION);
    }

    @Test
    void radialScreensSendTheSameActionsAndUseTheSameText() throws Exception {
        assertContractParity("ModeRadialScreen.java", ACTION);
        assertContractParity("ModeRadialScreen.java", TRANSLATION);
    }

    @Test
    void predictionStatesShareTheReviewedOpticalCorrection() throws Exception {
        for (Path root : new Path[] { LEGACY, CURRENT }) {
            String icons = Files.readString(root.resolve("PortalGuiIcons.java"));
            assertTrue(icons.contains("PREDICTION_OPTICAL_X = -3"));
            assertTrue(icons.contains("? PortalGuiSprites.PREDICTION_OFF : PortalGuiSprites.PREDICTION_ON,"));
            assertTrue(icons.contains("x + PREDICTION_OPTICAL_X, y + PREDICTION_OPTICAL_Y"));
        }
    }

    private static void assertContractParity(String file, Pattern pattern) throws Exception {
        assertEquals(matches(Files.readString(LEGACY.resolve(file)), pattern),
            matches(Files.readString(CURRENT.resolve(file)), pattern),
            file + " behavior contract drifted between Minecraft versions");
    }

    private static Set<String> matches(String source, Pattern pattern) {
        Set<String> values = new TreeSet<>();
        var matcher = pattern.matcher(source);
        while (matcher.find()) values.add(matcher.group(1));
        return values;
    }
}

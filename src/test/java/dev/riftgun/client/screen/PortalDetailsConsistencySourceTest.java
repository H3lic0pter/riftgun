package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalDetailsConsistencySourceTest {
    @Test
    void bothNodesUseHyphensAndApplyFuelCapabilityToPlayerWarnings() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", node,
                "src/main/java/dev/riftgun/client/screen/PortalConfigScreen.java"));
            String details = section(source, "private void renderDetails(",
                "private int detailField(");
            String player = section(details, "if (playerTargets.selectedId() != null)",
                "} else if (external != null)");
            String fuelCapability = section(source,
                "private boolean hasCrossDimensionFuel()",
                "private int moduleCount(");

            assertTrue(details.contains("displayName() + \" - \" + external.sourceGroup()"));
            assertFalse(details.contains("displayName() + \" · \" + external.sourceGroup()"));
            assertTrue(player.contains("&& !hasCrossDimensionFuel()"));
            assertTrue(fuelCapability.contains("InfiniteFuel"));
            assertTrue(fuelCapability.contains("CrossDimension"));
        }
    }

    private static String section(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0 && end > start, "Expected source section was not found");
        return source.substring(start, end);
    }
}

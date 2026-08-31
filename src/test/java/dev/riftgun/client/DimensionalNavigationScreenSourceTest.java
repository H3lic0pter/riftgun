package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DimensionalNavigationScreenSourceTest {
    @Test
    void navigationScreensUseTheEstablishedUnblurredHeaderAndDropdownContract() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String root = "versions/" + version
                + "/src/main/java/dev/riftgun/client/screen/";
            String navigation = Files.readString(Path.of(root + "DimensionalNavigationScreen.java"));
            String selection = Files.readString(Path.of(root + "DimensionSelectionScreen.java"));

            assertFalse(navigation.contains("renderBackground("), version + " must not blur the game");
            assertFalse(selection.contains("renderBackground("), version + " picker must not blur the game");
            assertFalse(navigation.contains("\"▲\"") || navigation.contains("\"▼\""),
                version + " must reuse the dropdown sprite");
            assertTrue(navigation.contains("drawDownIcon("),
                version + " must render the existing dropdown arrow");
            assertTrue(navigation.contains("drawBackIcon("),
                version + " must render the existing back arrow");
            assertTrue(navigation.contains("panelX + panelWidth - 12 - font.width(title)"),
                version + " must right-align the title");
            assertTrue(selection.contains("parent.selectDimensionAndReturn("),
                version + " picker selection must own the return transition");
        }
    }
}

package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DimensionalNavigationScreenSourceTest {
    @Test
    void navigationScreensKeepUiSharpReturnImmediatelyAndUseReviewedHeaderLayout() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String root = "versions/" + version
                + "/src/main/java/dev/riftgun/client/screen/";
            String navigation = Files.readString(Path.of(root + "DimensionalNavigationScreen.java"));
            String selection = Files.readString(Path.of(root + "DimensionSelectionScreen.java"));

            assertFalse(navigation.contains("super.render("),
                version + " must not apply blur after drawing its UI");
            assertFalse(selection.contains("super.render("),
                version + " picker must not apply blur after drawing its UI");
            if (version.equals("1.21.1")) {
                assertTrue(navigation.indexOf("renderBackground(")
                    < navigation.indexOf("graphics.fill(panelX"),
                    "1.21.1 must blur the world before drawing the navigation panel");
                assertTrue(selection.indexOf("renderBackground(")
                    < selection.indexOf("graphics.fill(panelX"),
                    "1.21.1 must blur the world before drawing the picker panel");
            }
            assertFalse(navigation.contains("\"▲\"") || navigation.contains("\"▼\""),
                version + " must reuse the dropdown sprite");
            assertTrue(navigation.contains("drawDownIcon("),
                version + " must render the existing dropdown arrow");
            assertFalse(navigation.contains("BACK_ICON_OPTICAL_X"),
                version + " must not duplicate the shared arrow correction");
            assertTrue(navigation.contains("drawCompactBackButtonIcon("),
                version + " must use the shared compact back-button renderer");
            assertTrue(selection.contains("drawCompactBackButtonIcon("),
                version + " picker must use the same back-button renderer");
            assertTrue(navigation.contains("backButton = button(panelX + panelWidth - 27"),
                version + " must place Back at top-right");
            assertTrue(selection.contains("panelX + panelWidth - 27, panelY + 7"),
                version + " picker must place Back at top-right");
            assertTrue(navigation.contains("font, title, panelX + 12, panelY + 12"),
                version + " must place the title at top-left");
            assertTrue(selection.contains("font, title, panelX + 12, panelY + 12"),
                version + " picker must place its title at top-left");
            assertTrue(selection.contains("parent.selectDimension(dimensions.get(index).id());"),
                version + " picker must update its parent selection");
            assertTrue(selection.contains("onClose();"),
                version + " picker must return through its own active Screen");
            assertTrue(navigation.indexOf("protected void init()")
                < navigation.indexOf("coordinateDefaultsInitialized = resetCoordinateDefaults()"),
                version + " must initialize coordinate defaults after Minecraft injects the client");
            assertTrue(navigation.contains("rebuildDropdownLabels();"),
                version + " must cache dropdown labels outside the render loop");
            assertTrue(navigation.contains("action.active = !saving &&"),
                version + " must suppress duplicate saves until the server replies");
            assertTrue(selection.contains("if (filteredSource != DimensionLabelState.dimensions())"),
                version + " picker must reuse its filtered list until input changes");
        }
    }
}

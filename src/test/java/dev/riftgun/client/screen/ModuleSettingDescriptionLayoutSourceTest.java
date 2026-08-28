package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ModuleSettingDescriptionLayoutSourceTest {
    private static final List<String> VERSIONS = List.of("1.21.1", "26.1.2");

    @Test
    void moduleDescriptionsWrapAndReserveTheirRenderedHeight() throws Exception {
        for (String version : VERSIONS) {
            String source = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "screen", "PortalConfigScreen.java"));

            assertTrue(source.contains("drawGunSettingDescription(graphics, box)"),
                version + " must route module descriptions through the wrapping helper");
            assertTrue(source.contains("font.split(description, Math.max(1, boxWidth - 36)).size()"),
                version + " must measure translated description lines");
            assertTrue(source.contains("gunSettingControlTop(box)"),
                version + " must move controls below wrapped descriptions");
            assertTrue(source.contains("gunSettingDescriptionExtraHeight(boxWidth)"),
                version + " must grow the modal for wrapped descriptions");
        }
    }
}

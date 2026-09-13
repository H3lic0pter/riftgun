package dev.riftgun.core.visual;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class PortalGunPanelModelTest {
    @Test
    void bothPanelCubesUseDistinctModeTintsAndStayInEveryGeometryVariant() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/riftgun/models/item/portal_gun.json")) {
            assertNotNull(stream);
            var model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var panel = model.getAsJsonArray("groups").asList().stream()
                .filter(group -> group.isJsonObject())
                .map(group -> group.getAsJsonObject())
                .filter(group -> group.get("name").getAsString().equals("panel"))
                .findFirst().orElseThrow();
            var children = panel.getAsJsonArray("children");
            assertEquals(2, children.size());
            for (int cube = 0; cube < children.size(); cube++) {
                int expected = 40 + cube;
                var element = model.getAsJsonArray("elements").get(children.get(cube).getAsInt()).getAsJsonObject();
                for (var entry : element.getAsJsonObject("faces").entrySet()) {
                    var face = entry.getValue().getAsJsonObject();
                    assertEquals(expected, face.get("tintindex").getAsInt());
                    for (int key = 0; key < PortalGunVisualSnapshot.VARIANT_COUNT; key++) {
                        assertTrue(PortalGunVisualSnapshot.includesTint(key, expected));
                    }
                }
            }
        }
    }
}

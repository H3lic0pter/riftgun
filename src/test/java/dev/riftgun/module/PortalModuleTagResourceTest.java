package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class PortalModuleTagResourceTest {
    private static final String TAG_PATH = "data/riftgun/tags/item/module.json";

    @Test
    void moduleTagContainsEveryModuleItem() throws Exception {
        var stream = PortalModuleTagResourceTest.class.getClassLoader().getResourceAsStream(TAG_PATH);
        if (stream == null) throw new IllegalStateException("missing " + TAG_PATH);

        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var tag = JsonParser.parseReader(reader).getAsJsonObject();
            assertFalse(tag.get("replace").getAsBoolean());

            Set<String> actual = new LinkedHashSet<>();
            tag.getAsJsonArray("values").forEach(value -> actual.add(value.getAsString()));
            assertEquals(Set.of(
                "riftgun:basic_module",
                "riftgun:advanced_basic_module",
                "riftgun:coordinate_override_module",
                "riftgun:dimensional_traversal_module",
                "riftgun:reservoir_expansion_module",
                "riftgun:passive_transit_module",
                "riftgun:hostile_transit_module",
                "riftgun:boss_transit_module",
                "riftgun:surface_range_amplifier",
                "riftgun:portal_aperture_module",
                "riftgun:module_bay_expansion",
                "riftgun:player_target_module",
                "riftgun:duration_extension_module",
                "riftgun:duration_eternal_module",
                "riftgun:fall_guard_module",
                "riftgun:entity_relocation_module",
                "riftgun:remote_module",
                "riftgun:precision_placement_module",
                "riftgun:portal_pairing_module",
                "riftgun:matter_anchor_module",
                "riftgun:projectile_transit_module",
                "riftgun:zero_point_fuel_module",
                "riftgun:creative_module"
            ), actual);
        }
    }
}

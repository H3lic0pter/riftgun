package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

final class PortalVisualSelectionTest {
    private static final Identifier CLASSIC = id("classic");
    private static final Identifier SWIRL = id("swirl");
    private static final List<PortalVisualType> TYPES = List.of(type(CLASSIC), type(SWIRL));

    @Test
    void unknownIdFallsBackToClassic() {
        assertEquals(CLASSIC, PortalVisualSelection.resolve(TYPES, id("missing"), CLASSIC));
    }

    @Test
    void cycleWrapsInStableRegistrationOrder() {
        assertEquals(SWIRL, PortalVisualSelection.cycle(TYPES, CLASSIC, 1, CLASSIC));
        assertEquals(CLASSIC, PortalVisualSelection.cycle(TYPES, SWIRL, 1, CLASSIC));
        assertEquals(SWIRL, PortalVisualSelection.cycle(TYPES, CLASSIC, -1, CLASSIC));
    }

    @Test
    void registryExposesBuiltinsInUiOrder() {
        assertEquals(List.of(PortalVisualRegistry.CLASSIC_ID, PortalVisualRegistry.SWIRL_ID,
                PortalVisualRegistry.ENDFRAME_ID),
            PortalVisualRegistry.values().stream().map(PortalVisualType::id).toList());
        assertEquals(PortalVisualRegistry.SWIRL_ID, PortalVisualRegistry.DEFAULT_ID);
    }

    private static PortalVisualType type(Identifier id) {
        return new PortalVisualType(id, "name", "description", ignored -> {});
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("riftgun_test", path);
    }
}

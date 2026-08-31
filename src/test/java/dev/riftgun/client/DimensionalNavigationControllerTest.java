package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.client.DimensionLabelState.DimensionInfo;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.state.PortalGunViewStateFixtures;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class DimensionalNavigationControllerTest {
    private static final List<DimensionInfo> DIMENSIONS = List.of(
        new DimensionInfo("minecraft:overworld", 1.0),
        new DimensionInfo("minecraft:the_nether", 8.0),
        new DimensionInfo("minecraft:the_end", 1.0));

    @Test
    void dropdownSelectionAndModeRulesLiveOutsideVersionedScreens() {
        var controller = new DimensionalNavigationController(
            PortalGunViewStateFixtures.representative(), UUID.randomUUID());

        controller.openDropdown(DIMENSIONS);
        assertTrue(controller.dropdownOpen());
        assertEquals(1, controller.dropdownIndex());
        assertTrue(controller.selectDimension(DIMENSIONS, "minecraft:the_end"));
        assertFalse(controller.dropdownOpen());
        assertFalse(controller.selectMode(DimensionalTraversalMode.AUTOMATIC_SEARCH, false));
        assertTrue(controller.selectMode(DimensionalTraversalMode.EXACT_COORDINATES, false));
    }

    @Test
    void saveOutcomeDistinguishesAuthoritativeAcceptanceFromRejection() {
        var controller = new DimensionalNavigationController(
            PortalGunViewStateFixtures.representative(), UUID.randomUUID());
        UUID original = UUID.randomUUID();
        controller.beginSave(original);

        assertEquals(DimensionalNavigationController.SaveOutcome.REJECTED,
            controller.acceptSnapshot(original));

        controller.beginSave(original);
        assertEquals(DimensionalNavigationController.SaveOutcome.SAVED,
            controller.acceptSnapshot(UUID.randomUUID()));
    }

    @Test
    void authoritativeRefreshRollsBackAnOptimisticSelection() {
        var authoritative = PortalGunViewStateFixtures.representative();
        var controller = new DimensionalNavigationController(authoritative, UUID.randomUUID());
        controller.selectDimension(DIMENSIONS, "minecraft:the_end");

        controller.refresh(authoritative);

        assertEquals("minecraft:the_nether", controller.dimension());
        assertEquals(DimensionalTraversalMode.AUTOMATIC_SEARCH, controller.mode());
    }
}

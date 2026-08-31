package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.client.DimensionLabelState.DimensionInfo;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.network.PortalAction;
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

    @Test
    void workflowBuildsBothWireCommandsOutsideVersionedScreens() {
        var controller = new DimensionalNavigationController(
            PortalGunViewStateFixtures.representative(), UUID.randomUUID());
        var fields = new DimensionalNavigationWorkflow.ExactFields(
            "Base", "1", "64", "2", "90");

        var automatic = DimensionalNavigationWorkflow.begin(controller, fields, null);
        assertEquals(PortalAction.OPEN_DIMENSIONAL_RIFT, automatic.action());
        assertTrue(automatic.closesScreen());

        assertTrue(controller.selectMode(DimensionalTraversalMode.EXACT_COORDINATES, true));
        UUID selected = UUID.randomUUID();
        var exact = DimensionalNavigationWorkflow.begin(controller, fields, selected);
        var payload = new net.minecraft.nbt.CompoundTag();
        exact.writeTo(payload);

        assertEquals(PortalAction.CREATE_DIMENSIONAL_COORDINATE, exact.action());
        assertFalse(exact.closesScreen());
        assertEquals("Base", Nbt.getString(payload, "Name"));
        assertEquals("minecraft:the_nether", Nbt.getString(payload, "Dimension"));
        assertTrue(controller.saving());
    }

    @Test
    void coordinateScalingAndFormattingAreShared() {
        var defaults = DimensionalNavigationWorkflow.coordinateDefaults(
            DIMENSIONS, "minecraft:the_nether", 1.0, 80.0, 64.0, -80.0, 45.0F);

        assertEquals("10.00", defaults.x());
        assertEquals("64.00", defaults.y());
        assertEquals("-10.00", defaults.z());
        assertEquals("45.00", defaults.yaw());
    }
}

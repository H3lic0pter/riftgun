package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.data.PortalPlayerData;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class PortalDestinationActionsTest {
    @Test
    void automaticNameDefaultsToDimensionAndValidatesCustomNames() {
        PortalPlayerData data = new PortalPlayerData();
        for (String input : new String[]{null, "", " \t "}) {
            assertEquals("The Nether", PortalDestinationActions.automaticDestinationName(data, input, () -> "The Nether"));
        }
        assertEquals("Exploration", PortalDestinationActions.automaticDestinationName(data, " Exploration ",
            () -> { throw new AssertionError("Custom names do not need a dimension label"); }));
        org.junit.jupiter.api.Assertions.assertThrows(PortalRequestException.class,
            () -> PortalDestinationActions.automaticDestinationName(data, "X".repeat(1000), () -> "The Nether"));
    }

    @Test
    void randomEntriesCannotBeMovedAndExpansionPersists() {
        PortalPlayerData data = new PortalPlayerData();
        var random = new dev.riftgun.data.Destination(java.util.UUID.randomUUID(), "Explore",
            PortalPlayerData.DEFAULT_GROUP_ID, null, 0, 0, 0, 0, 0, 0, false, true, null);
        data.destinations().add(random);
        CompoundTag request = new CompoundTag();
        Nbt.putUUID(request, "Destination", random.id());
        Nbt.putUUID(request, "Group", PortalPlayerData.DEFAULT_GROUP_ID);
        org.junit.jupiter.api.Assertions.assertThrows(PortalRequestException.class,
            () -> PortalDestinationActions.moveDestinationGroup(data, request));
        assertEquals(PortalPlayerData.RANDOM_SECTION_ID, data.destinations().getFirst().groupId());
        request.putString("Name", "Renamed");
        PortalRequestException error = org.junit.jupiter.api.Assertions.assertThrows(PortalRequestException.class,
            () -> PortalDestinationActions.edit(null, data, request, null));
        assertEquals("message.riftgun.random_read_only", error.translationKey());
        assertEquals(random, data.destinations().getFirst());
        Nbt.putUUID(request, "Group", PortalPlayerData.RANDOM_SECTION_ID);
        request.putBoolean("Expanded", false);
        PortalDestinationActions.setExpanded(data, request);
        assertFalse(data.expandedGroups().contains(PortalPlayerData.RANDOM_SECTION_ID));
    }

    @Test
    void clampsEnteredCoordinatesToMinecraftWorldAndDimensionBounds() {
        DestinationCoordinateBounds.Coordinates coordinates = DestinationCoordinateBounds.clamp(
            1.0E100, -1.0E100, -1.0E100, -64, 320);

        assertEquals(29_999_999.0, coordinates.x());
        assertEquals(-64.0, coordinates.y());
        assertEquals(-30_000_000.0, coordinates.z());
    }

    @Test
    void preservesCoordinatesWhoseContainingBlocksAreInBounds() {
        DestinationCoordinateBounds.Coordinates coordinates = DestinationCoordinateBounds.clamp(
            29_999_999.75, 319.75, -29_999_999.75, -64, 320);

        assertEquals(29_999_999.75, coordinates.x());
        assertEquals(319.75, coordinates.y());
        assertEquals(-29_999_999.75, coordinates.z());
    }

    @Test
    void mapSectionExpansionUsesThePersistedGroupAction() {
        PortalPlayerData data = new PortalPlayerData();
        CompoundTag request = new CompoundTag();
        Nbt.putUUID(request, "Group", PortalPlayerData.JOURNEYMAP_SECTION_ID);
        request.putBoolean("Expanded", false);

        assertTrue(PortalDestinationActions.setExpanded(data, request));
        assertFalse(data.expandedGroups().contains(PortalPlayerData.JOURNEYMAP_SECTION_ID));

        Nbt.putUUID(request, "Group", PortalPlayerData.XAERO_MINIMAP_SECTION_ID);
        request.putBoolean("Expanded", true);
        assertTrue(PortalDestinationActions.setExpanded(data, request));
        assertTrue(data.expandedGroups().contains(PortalPlayerData.XAERO_MINIMAP_SECTION_ID));
    }
}

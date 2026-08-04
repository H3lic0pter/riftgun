package dev.riftgun.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

final class PortalPlayerDataTest {
    @Test
    void defaultNamesRemainMonotonicAcrossPersistence() {
        PortalPlayerData data = new PortalPlayerData();
        assertEquals("Location1", data.nextLocationName());
        assertEquals("Location2", data.nextLocationName());

        PortalPlayerData restored = PortalPlayerData.load(data.save());
        assertEquals("Location3", restored.nextLocationName());
        assertTrue(restored.expandedGroups().contains(PortalPlayerData.DEFAULT_GROUP_ID));
    }

    @Test
    void repairsUnknownGroupsWithoutChangingDestinationIdentity() {
        PortalPlayerData data = new PortalPlayerData();
        UUID id = UUID.randomUUID();
        data.destinations().add(new Destination(id, "Home", UUID.randomUUID(), Level.OVERWORLD,
            1.25, 64.0, -8.5, 90.0F, 10L, 0L, true));
        data.selectedDestinationId(id);

        PortalPlayerData restored = PortalPlayerData.load(data.save());
        Destination destination = restored.destination(id).orElseThrow();
        assertEquals(PortalPlayerData.DEFAULT_GROUP_ID, destination.groupId());
        assertEquals(id, restored.selectedDestinationId());
        assertEquals(1.25, destination.x());
    }
}

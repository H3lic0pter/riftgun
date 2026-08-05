package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlayerData;
import java.util.UUID;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

final class DestinationSafetyFingerprintTest {
    @Test
    void metadataChangesKeepCacheWhilePositionChangesInvalidateIt() {
        UUID id = UUID.randomUUID();
        Destination original = new Destination(id, "A", PortalPlayerData.DEFAULT_GROUP_ID,
            Level.OVERWORLD, 1.0, 64.0, 2.0, 0.0F, 1L, 0L, false);
        Destination renamed = original.withDetails("B", UUID.randomUUID(), Level.OVERWORLD,
            1.0, 64.0, 2.0, 90.0F);
        Destination moved = original.withDetails("A", original.groupId(), Level.NETHER,
            1.0, 64.0, 2.0, 0.0F);

        assertEquals(DestinationSafetyFingerprint.of(original), DestinationSafetyFingerprint.of(renamed));
        assertNotEquals(DestinationSafetyFingerprint.of(original), DestinationSafetyFingerprint.of(moved));
    }
}

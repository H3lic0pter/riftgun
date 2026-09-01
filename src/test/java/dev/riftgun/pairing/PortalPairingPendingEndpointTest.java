package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.core.nbt.Nbt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class PortalPairingPendingEndpointTest {
    @Test
    void roundTripsDimensionPlacementAndEndpoint() {
        CompoundTag tag = new CompoundTag();
        UUID owner = UUID.randomUUID();
        UUID gun = UUID.randomUUID();
        Nbt.putUUID(tag, "Owner", owner);
        Nbt.putUUID(tag, "Gun", gun);
        tag.putString("Dimension", "minecraft:the_nether");
        tag.putString("Endpoint", "B");
        tag.putLong("StartedAt", 120L);
        tag.putInt("DurationTicks", 60);
        tag.putDouble("X", 12.5);
        tag.putDouble("Y", 64.0);
        tag.putDouble("Z", -8.25);
        tag.putString("Orientation", "VERTICAL");
        tag.putString("Geometry", "SURFACE_EXPANDED");
        tag.putFloat("Yaw", 90.0F);
        tag.putLong("Anchor", new BlockPos(12, 64, -9).asLong());
        tag.putString("AnchorFace", "SOUTH");

        PortalPairingPendingEndpoint pending = PortalPairingPendingEndpoint.load(tag);
        assertNotNull(pending);
        assertEquals(PortalPairingEndpoint.B, pending.endpoint());
        assertEquals(PortalGeometry.SURFACE_EXPANDED, pending.placement().geometry());
        assertEquals(owner, pending.ownerId());
        assertEquals(gun, pending.gunId());
        assertTrue(pending.validFor(owner, gun, 179L));
        assertTrue(pending.validFor(owner, gun, 180L));
        assertTrue(pending.validFor(owner, gun, Long.MAX_VALUE));
        assertFalse(pending.validFor(UUID.randomUUID(), gun, 121L));
        assertEquals(pending, PortalPairingPendingEndpoint.load(pending.save()));
    }

    @Test
    void fixedEntityTargetStillExpiresAfterItsPortalDuration() {
        UUID owner = UUID.randomUUID();
        UUID gun = UUID.randomUUID();
        CompoundTag tag = new CompoundTag();
        Nbt.putUUID(tag, "Owner", owner);
        Nbt.putUUID(tag, "Gun", gun);
        tag.putString("Dimension", "minecraft:overworld");
        tag.putString("Endpoint", "ENTITY_TARGET");
        tag.putLong("StartedAt", 120L);
        tag.putInt("DurationTicks", 60);
        tag.putDouble("X", 0.0);
        tag.putDouble("Y", 64.0);
        tag.putDouble("Z", 0.0);
        tag.putString("Orientation", "VERTICAL");
        tag.putString("Geometry", "FLOATING_VERTICAL");
        tag.putFloat("Yaw", 0.0F);
        PortalPairingPendingEndpoint target = PortalPairingPendingEndpoint.load(tag);
        assertNotNull(target);

        assertTrue(target.validFor(owner, gun, 179L));
        assertFalse(target.validFor(owner, gun, 180L));
    }

    @Test
    void rejectsInvalidOrPartialPayloads() {
        CompoundTag invalid = new CompoundTag();
        Nbt.putUUID(invalid, "Owner", UUID.randomUUID());
        Nbt.putUUID(invalid, "Gun", UUID.randomUUID());
        invalid.putLong("StartedAt", 0L);
        invalid.putInt("DurationTicks", 20);
        invalid.putString("Dimension", "minecraft:overworld");
        invalid.putString("Endpoint", "NONE");
        invalid.putString("Orientation", "VERTICAL");
        invalid.putString("Geometry", "SURFACE_VERTICAL");
        assertNull(PortalPairingPendingEndpoint.load(invalid));

        invalid.putString("Endpoint", "A");
        invalid.putDouble("X", Double.NaN);
        assertNull(PortalPairingPendingEndpoint.load(invalid));

        invalid.putDouble("X", 0.0);
        invalid.putLong("Anchor", BlockPos.ZERO.asLong());
        assertNull(PortalPairingPendingEndpoint.load(invalid));
    }
}

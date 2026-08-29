package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.riftgun.portal.PortalGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class PortalPairingPendingEndpointTest {
    @Test
    void roundTripsDimensionPlacementAndEndpoint() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", "minecraft:the_nether");
        tag.putString("Endpoint", "B");
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
        assertEquals(pending, PortalPairingPendingEndpoint.load(pending.save()));
    }

    @Test
    void rejectsInvalidOrPartialPayloads() {
        CompoundTag invalid = new CompoundTag();
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

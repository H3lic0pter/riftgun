package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.api.PortalTransitAuthorization;
import dev.riftgun.api.RiftResourceId;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class PortalTransitAuthorizationCodecTest {
    @Test
    void roundTripsOpaqueAuthorization() {
        PortalTransitAuthorization expected = new PortalTransitAuthorization(
            RiftResourceId.parse("riftworld:entry"),
            RiftResourceId.parse("riftworld:reality/123e4567-e89b-12d3-a456-426614174000"));

        assertEquals(expected, PortalTransitAuthorizationCodec.load(
            PortalTransitAuthorizationCodec.save(expected)).orElseThrow());
    }

    @Test
    void rejectsInvalidSnapshotWithoutBreakingPortalLoad() {
        CompoundTag damaged = new CompoundTag();
        damaged.putString("Authority", "not a resource id");
        damaged.putString("DestinationDimension", "riftworld:reality/test");

        assertTrue(PortalTransitAuthorizationCodec.load(damaged).isEmpty());
    }
}

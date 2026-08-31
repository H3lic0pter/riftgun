package dev.riftgun.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalGunViewStateCodecTest {
    @Test
    void representativeServerSnapshotRoundTripsWithoutLosingTypedState() {
        PortalGunViewState state = PortalGunViewStateFixtures.representative();

        PortalGunViewState decoded = PortalGunViewStateCodec.decode(
            PortalGunViewStateCodec.encode(state));

        assertEquals(state, decoded);
    }

    @Test
    void emptyWireStateUsesSafeRangesAndEnums() {
        PortalGunViewState decoded = PortalGunViewStateCodec.decode(
            new net.minecraft.nbt.CompoundTag());

        assertEquals(1, decoded.maximumSurfaceRange());
        assertEquals(1, decoded.remoteDistance());
        assertEquals(1, decoded.smartDistance());
        assertEquals(dev.riftgun.pairing.PortalFunctionMode.COORDINATE_TRAVEL,
            decoded.functionMode());
    }
}

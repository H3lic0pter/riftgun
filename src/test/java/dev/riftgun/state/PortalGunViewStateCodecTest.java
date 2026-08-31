package dev.riftgun.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    @Test
    void absentFuelProfileKeepsLegacyWireKeysAbsent() {
        PortalGunViewState state = new PortalGunViewState(
            null, null, PortalGunViewState.Fuel.EMPTY,
            PortalGunViewState.Navigation.EMPTY, PortalGunViewState.Placement.EMPTY,
            PortalGunViewState.Transit.EMPTY, PortalGunViewState.Modules.empty());

        var encoded = PortalGunViewStateCodec.encode(state);

        assertFalse(encoded.contains("Fluid"));
        assertFalse(encoded.contains("Rgb"));
        assertFalse(encoded.contains("CrossDimension"));
    }
}

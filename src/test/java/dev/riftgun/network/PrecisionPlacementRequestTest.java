package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PrecisionPlacementRequestTest {
    @Test
    void floatingPreviewPlacementRoundTripsForExactPairingCommit() {
        PortalPlacement preview = new PortalPlacement(new Vec3(3.25, 65.0, -7.5),
            PortalOrientation.VERTICAL, PortalGeometry.FLOATING_VERTICAL, 135.0F, null, null);
        PrecisionPlacementRequest request = PrecisionPlacementRequest
            .floating(PortalOrientation.VERTICAL).withPreviewPlacement(preview);
        CompoundTag tag = new CompoundTag();

        request.writeTo(tag);
        PrecisionPlacementRequest decoded = PrecisionPlacementRequest.decode(tag);

        assertEquals(request, decoded);
    }
}

package dev.riftgun.network;

import dev.riftgun.core.nbt.Nbt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

final class PortalPayloadTest {
    @Test
    void requestPayloadRoundTripsNbt() {
        CompoundTag data = new CompoundTag();
        data.putString("Action", PortalAction.CREATE_COORDINATE.name());
        data.putString("X", "~12.5");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.OTHER);

        PortalRequestPayload.STREAM_CODEC.encode(buffer, new PortalRequestPayload(data));
        PortalRequestPayload decoded = PortalRequestPayload.STREAM_CODEC.decode(buffer);

        assertEquals(PortalAction.CREATE_COORDINATE.name(), Nbt.getString(decoded.data(), "Action"));
        assertEquals("~12.5", Nbt.getString(decoded.data(), "X"));
        buffer.release();
    }

    @Test
    void surfaceFaceRequestRoundTripsAnchorAndFace() {
        SurfaceFaceRequest request = new SurfaceFaceRequest(
            new BlockPos(12, 64, -9), Direction.WEST);

        SurfaceFaceRequest decoded = SurfaceFaceRequest.decode(request.encode());

        assertEquals(request, decoded);
    }

    @Test
    void surfaceFaceRequestRejectsMissingOrInvalidFields() {
        assertThrows(PortalRequestException.class,
            () -> SurfaceFaceRequest.decode(new CompoundTag()));
        CompoundTag invalid = new CompoundTag();
        invalid.putInt("AnchorX", 0);
        invalid.putInt("AnchorY", 64);
        invalid.putInt("AnchorZ", 0);
        invalid.putString("Face", "SIDEWAYS");
        assertThrows(PortalRequestException.class,
            () -> SurfaceFaceRequest.decode(invalid));
    }
}

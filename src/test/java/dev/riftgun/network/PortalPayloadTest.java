package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class PortalPayloadTest {
    @Test
    void requestPayloadRoundTripsNbt() {
        CompoundTag data = new CompoundTag();
        data.putString("Action", PortalAction.CREATE_COORDINATE.name());
        data.putString("X", "~12.5");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        PortalRequestPayload.STREAM_CODEC.encode(buffer, new PortalRequestPayload(data));
        PortalRequestPayload decoded = PortalRequestPayload.STREAM_CODEC.decode(buffer);

        assertEquals(PortalAction.CREATE_COORDINATE.name(), decoded.data().getString("Action"));
        assertEquals("~12.5", decoded.data().getString("X"));
        buffer.release();
    }
}

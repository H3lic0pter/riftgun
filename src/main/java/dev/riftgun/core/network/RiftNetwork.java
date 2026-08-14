package dev.riftgun.core.network;

import java.util.Objects;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Once-installed common access point for the active loader transport. */
public final class RiftNetwork {
    private static volatile NetworkTransport transport;

    public static synchronized void install(NetworkTransport installed) {
        if (transport != null) throw new IllegalStateException("network transport already installed");
        transport = Objects.requireNonNull(installed, "installed");
    }

    public static void sendToServer(CustomPacketPayload payload) {
        current().sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        current().sendToPlayer(player, payload);
    }

    private static NetworkTransport current() {
        return Objects.requireNonNull(transport, "network transport has not been installed");
    }

    private RiftNetwork() {}
}

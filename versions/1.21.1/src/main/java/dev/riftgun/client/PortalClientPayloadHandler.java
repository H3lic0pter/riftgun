package dev.riftgun.client;

import net.minecraft.nbt.CompoundTag;

public final class PortalClientPayloadHandler {
    public static void handle(CompoundTag envelope) {
        PortalClientState.handle(envelope);
    }

    private PortalClientPayloadHandler() {}
}


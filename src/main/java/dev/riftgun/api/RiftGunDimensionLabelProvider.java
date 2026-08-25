package dev.riftgun.api;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Resolves a dynamic user-facing label for addon-owned Dimension IDs. */
public interface RiftGunDimensionLabelProvider {
    RiftResourceId id();
    Optional<Component> label(ServerPlayer viewer, RiftResourceId dimensionId);
}

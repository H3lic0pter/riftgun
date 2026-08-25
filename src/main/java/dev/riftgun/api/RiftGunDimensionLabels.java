package dev.riftgun.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Process-wide registration and lookup for dynamic Dimension presentation. */
public final class RiftGunDimensionLabels {
    private static final Map<RiftResourceId, RiftGunDimensionLabelProvider> PROVIDERS = new LinkedHashMap<>();

    public static synchronized void register(RiftGunDimensionLabelProvider provider) {
        Objects.requireNonNull(provider, "provider");
        RiftResourceId id = Objects.requireNonNull(provider.id(), "provider.id()");
        if (PROVIDERS.putIfAbsent(id, provider) != null) {
            throw new IllegalStateException("Rift Gun Dimension label provider already registered: " + id);
        }
    }

    public static synchronized List<RiftGunDimensionLabelProvider> providers() {
        return List.copyOf(PROVIDERS.values());
    }

    public static Optional<Component> label(ServerPlayer viewer, RiftResourceId dimensionId) {
        for (RiftGunDimensionLabelProvider provider : providers()) {
            Optional<Component> label = Objects.requireNonNull(
                provider.label(viewer, dimensionId), "provider label");
            if (label.isPresent()) return label;
        }
        return Optional.empty();
    }

    private RiftGunDimensionLabels() {}
}

package dev.riftgun.api;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** Process-wide registration and lookup for dynamic Dimension presentation. */
public final class RiftGunDimensionLabels {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<RiftResourceId, RiftGunDimensionLabelProvider> PROVIDERS = new LinkedHashMap<>();
    private static final Set<RiftResourceId> WARNED_PROVIDERS = ConcurrentHashMap.newKeySet();

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
        for (var entry : snapshot().entrySet()) {
            try {
                Optional<Component> label = Objects.requireNonNull(
                    entry.getValue().label(viewer, dimensionId), "provider label");
                if (label.isPresent()) return label;
            } catch (RuntimeException exception) {
                if (WARNED_PROVIDERS.add(entry.getKey())) {
                    LOGGER.warn("Rift Gun dimension label provider '{}' failed; skipping it",
                        entry.getKey(), exception);
                }
            }
        }
        return Optional.empty();
    }

    private static synchronized Map<RiftResourceId, RiftGunDimensionLabelProvider> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(PROVIDERS));
    }

    private RiftGunDimensionLabels() {}
}

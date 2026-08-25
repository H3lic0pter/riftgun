package dev.riftgun.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Process-wide registration point for external destination providers. */
public final class RiftGunDestinationProviders {
    private static final Map<RiftResourceId, RiftGunDestinationProvider> PROVIDERS = new LinkedHashMap<>();

    public static synchronized void register(RiftGunDestinationProvider provider) {
        Objects.requireNonNull(provider, "provider");
        RiftResourceId id = Objects.requireNonNull(provider.id(), "provider.id()");
        if (PROVIDERS.containsKey(id)) {
            throw new IllegalStateException("Rift Gun destination provider already registered: " + id);
        }
        PROVIDERS.put(id, provider);
    }

    public static synchronized Optional<RiftGunDestinationProvider> provider(RiftResourceId id) {
        return Optional.ofNullable(PROVIDERS.get(Objects.requireNonNull(id, "id")));
    }

    public static synchronized List<RiftGunDestinationProvider> providers() {
        return List.copyOf(PROVIDERS.values());
    }

    private RiftGunDestinationProviders() {}
}

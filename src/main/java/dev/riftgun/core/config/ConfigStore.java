package dev.riftgun.core.config;

import java.util.Objects;

/** Atomically publishes complete immutable configuration snapshots. */
final class ConfigStore<T> {
    private volatile T current;

    ConfigStore() {}

    ConfigStore(T initial) {
        current = Objects.requireNonNull(initial, "initial");
    }

    T current() {
        return Objects.requireNonNull(current, "configuration has not been installed");
    }

    void publish(T snapshot) {
        current = Objects.requireNonNull(snapshot, "snapshot");
    }
}

package dev.riftgun.core.runtime;

import java.util.Objects;

/** Thread-visible dependency slot that can be initialized exactly once. */
final class OnceInstalled<T> {
    private final String description;
    private volatile T value;

    OnceInstalled(String description) {
        this.description = Objects.requireNonNull(description, "description");
    }

    synchronized void install(T installed) {
        if (value != null) throw new IllegalStateException(description + " already installed");
        value = Objects.requireNonNull(installed, "installed");
    }

    T current() {
        T installed = value;
        if (installed == null) throw new IllegalStateException(description + " not installed");
        return installed;
    }
}

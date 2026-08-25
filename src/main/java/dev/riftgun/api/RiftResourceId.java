package dev.riftgun.api;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Version-stable namespaced identifier used at the Rift Gun API boundary.
 *
 * <p>This deliberately does not expose Minecraft's resource identifier class,
 * whose Java name differs between supported game versions.
 */
public record RiftResourceId(String namespace, String path) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public RiftResourceId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid resource namespace: " + namespace);
        }
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid resource path: " + path);
        }
    }

    public static RiftResourceId parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            throw new IllegalArgumentException("Expected a namespaced identifier: " + value);
        }
        return new RiftResourceId(value.substring(0, separator), value.substring(separator + 1));
    }

    @Override
    public String toString() {
        return namespace + ':' + path;
    }
}

package dev.riftgun.navigation;

import com.mojang.serialization.Codec;

/** Persisted operating mode of the Dimensional Traversal screen. */
public enum DimensionalTraversalMode {
    EXACT_COORDINATES,
    AUTOMATIC_SEARCH;

    public static final Codec<DimensionalTraversalMode> CODEC = Codec.STRING.xmap(
        DimensionalTraversalMode::parse, DimensionalTraversalMode::name);

    public static DimensionalTraversalMode parse(String value) {
        try {
            return value == null ? EXACT_COORDINATES : valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return EXACT_COORDINATES;
        }
    }
}

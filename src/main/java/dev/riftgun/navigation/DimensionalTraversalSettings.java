package dev.riftgun.navigation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Per-gun navigation selection; an empty dimension means the holder's current dimension. */
public record DimensionalTraversalSettings(
    String targetDimension,
    DimensionalTraversalMode mode
) {
    public static final Codec<DimensionalTraversalSettings> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("target_dimension", "")
                .forGetter(DimensionalTraversalSettings::targetDimension),
            DimensionalTraversalMode.CODEC.optionalFieldOf(
                    "mode", DimensionalTraversalMode.EXACT_COORDINATES)
                .forGetter(DimensionalTraversalSettings::mode)
        ).apply(instance, DimensionalTraversalSettings::new));

    public DimensionalTraversalSettings {
        targetDimension = targetDimension == null ? "" : targetDimension.strip();
        mode = mode == null ? DimensionalTraversalMode.EXACT_COORDINATES : mode;
    }

    public static DimensionalTraversalSettings defaults() {
        return new DimensionalTraversalSettings("", DimensionalTraversalMode.EXACT_COORDINATES);
    }

    public DimensionalTraversalSettings withTargetDimension(String value) {
        return new DimensionalTraversalSettings(value, mode);
    }

    public DimensionalTraversalSettings withMode(DimensionalTraversalMode value) {
        return new DimensionalTraversalSettings(targetDimension, value);
    }
}

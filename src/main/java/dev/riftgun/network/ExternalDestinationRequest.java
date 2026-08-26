package dev.riftgun.network;

import dev.riftgun.api.RiftResourceId;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.external.ExternalDestinationSelection;
import dev.riftgun.external.ExternalDestinationSource;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;

/** Codec and trust-boundary validation for client-supplied map waypoints. */
public final class ExternalDestinationRequest {
    public static final int MAXIMUM_STABLE_ID_LENGTH = 512;
    public static final int MAXIMUM_NAME_LENGTH = 48;
    public static final int MAXIMUM_DIMENSION_LENGTH = 256;

    public static CompoundTag encode(ExternalDestinationSelection selection) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Source", selection.source().name());
        tag.putString("StableId", selection.stableId());
        tag.putString("Name", selection.name());
        tag.putString("Dimension", selection.dimensionId());
        tag.putDouble("X", selection.x());
        tag.putDouble("Y", selection.y());
        tag.putDouble("Z", selection.z());
        return tag;
    }

    public static DecodeResult decode(
        CompoundTag tag,
        boolean integrationEnabled,
        Predicate<String> knownDimension
    ) {
        if (!integrationEnabled) return DecodeResult.failure(Error.DISABLED);

        String sourceText = Nbt.getString(tag, "Source");
        String stableId = Nbt.getString(tag, "StableId");
        String name = Nbt.getString(tag, "Name");
        String dimension = Nbt.getString(tag, "Dimension");
        if (stableId.isBlank() || name.isBlank() || dimension.isBlank()) {
            return DecodeResult.failure(Error.INVALID_TEXT);
        }
        if (stableId.length() > MAXIMUM_STABLE_ID_LENGTH
            || name.length() > MAXIMUM_NAME_LENGTH
            || dimension.length() > MAXIMUM_DIMENSION_LENGTH) {
            return DecodeResult.failure(Error.TEXT_TOO_LONG);
        }

        ExternalDestinationSource source;
        try {
            source = ExternalDestinationSource.valueOf(sourceText);
        } catch (IllegalArgumentException exception) {
            return DecodeResult.failure(Error.INVALID_SOURCE);
        }
        try {
            RiftResourceId.parse(dimension);
        } catch (IllegalArgumentException exception) {
            return DecodeResult.failure(Error.INVALID_DIMENSION);
        }
        if (!knownDimension.test(dimension)) {
            return DecodeResult.failure(Error.UNKNOWN_DIMENSION);
        }

        if (!Nbt.contains(tag, "X") || !Nbt.contains(tag, "Y") || !Nbt.contains(tag, "Z")) {
            return DecodeResult.failure(Error.MISSING_COORDINATE);
        }
        double x = Nbt.getDouble(tag, "X");
        double y = Nbt.getDouble(tag, "Y");
        double z = Nbt.getDouble(tag, "Z");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return DecodeResult.failure(Error.NON_FINITE_COORDINATE);
        }
        return new DecodeResult(new ExternalDestinationSelection(source, stableId, name, dimension,
            x, y, z), Error.NONE);
    }

    public record DecodeResult(ExternalDestinationSelection selection, Error error) {
        private static DecodeResult failure(Error error) {
            return new DecodeResult(null, error);
        }

        public boolean accepted() {
            return error == Error.NONE;
        }
    }

    public enum Error {
        NONE,
        DISABLED,
        INVALID_SOURCE,
        INVALID_TEXT,
        TEXT_TOO_LONG,
        INVALID_DIMENSION,
        UNKNOWN_DIMENSION,
        MISSING_COORDINATE,
        NON_FINITE_COORDINATE
    }

    private ExternalDestinationRequest() {}
}

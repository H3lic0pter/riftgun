package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.external.ExternalDestinationSelection;
import dev.riftgun.external.ExternalDestinationSource;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class ExternalDestinationRequestTest {
    @Test
    void selectionRoundTripsThroughNbt() {
        ExternalDestinationSelection selection = selection();

        CompoundTag encoded = ExternalDestinationRequest.encode(selection);
        ExternalDestinationRequest.DecodeResult decoded = ExternalDestinationRequest.decode(
            encoded, true, dimension -> dimension.equals("minecraft:overworld"));

        assertEquals(ExternalDestinationRequest.Error.NONE, decoded.error());
        assertEquals(selection, decoded.selection());
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        CompoundTag encoded = ExternalDestinationRequest.encode(selection());
        encoded.putDouble("X", Double.NaN);

        assertEquals(ExternalDestinationRequest.Error.NON_FINITE_COORDINATE,
            ExternalDestinationRequest.decode(encoded, true, ignored -> true).error());

        encoded.putDouble("X", Double.POSITIVE_INFINITY);
        assertEquals(ExternalDestinationRequest.Error.NON_FINITE_COORDINATE,
            ExternalDestinationRequest.decode(encoded, true, ignored -> true).error());
    }

    @Test
    void rejectsMissingCoordinateFieldsInsteadOfDefaultingThemToZero() {
        CompoundTag encoded = ExternalDestinationRequest.encode(selection());
        encoded.remove("Z");

        assertEquals(ExternalDestinationRequest.Error.MISSING_COORDINATE,
            ExternalDestinationRequest.decode(encoded, true, ignored -> true).error());
    }

    @Test
    void rejectsUnknownOrMalformedDimensions() {
        CompoundTag encoded = ExternalDestinationRequest.encode(selection());

        assertEquals(ExternalDestinationRequest.Error.UNKNOWN_DIMENSION,
            ExternalDestinationRequest.decode(encoded, true, ignored -> false).error());

        encoded.putString("Dimension", "not a resource id");
        assertEquals(ExternalDestinationRequest.Error.INVALID_DIMENSION,
            ExternalDestinationRequest.decode(encoded, true, ignored -> true).error());
    }

    @Test
    void rejectsLongTextAndDisabledServerIntegration() {
        CompoundTag encoded = ExternalDestinationRequest.encode(selection());
        encoded.putString("Name", "x".repeat(ExternalDestinationRequest.MAXIMUM_NAME_LENGTH + 1));
        assertEquals(ExternalDestinationRequest.Error.TEXT_TOO_LONG,
            ExternalDestinationRequest.decode(encoded, true, ignored -> true).error());

        ExternalDestinationRequest.DecodeResult disabled = ExternalDestinationRequest.decode(
            ExternalDestinationRequest.encode(selection()), false, ignored -> true);
        assertEquals(ExternalDestinationRequest.Error.DISABLED, disabled.error());
        assertTrue(disabled.selection() == null);
    }

    private static ExternalDestinationSelection selection() {
        return new ExternalDestinationSelection(ExternalDestinationSource.JOURNEYMAP, "village-id",
            "Village", "minecraft:overworld", 123.5, 64, -88.5);
    }
}

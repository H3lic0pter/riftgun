package dev.riftgun.api;

import java.util.Objects;
import net.minecraft.network.chat.Component;

/** Result returned after Rift Gun has attempted to create a coordinate note. */
public record CoordinateNoteResult(CoordinateNoteStatus status, Component message) {
    public CoordinateNoteResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(message, "message");
    }

    public boolean created() {
        return status == CoordinateNoteStatus.CREATED;
    }
}

package dev.riftgun.data;

import dev.riftgun.core.nbt.Nbt;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Bounded provenance retained by an imported destination. */
public record ShareProvenance(
    UUID sourceDestinationId,
    UUID originalAuthorId,
    String originalAuthorName,
    UUID sharedById,
    String sharedByName
) {
    public static ShareProvenance from(CoordinateSnapshot snapshot) {
        return new ShareProvenance(snapshot.sourceDestinationId(), snapshot.originalAuthorId(),
            snapshot.originalAuthorName(), snapshot.sharedById(), snapshot.sharedByName());
    }

    public CompoundTag save(UUID destinationId) {
        CompoundTag tag = new CompoundTag();
        Nbt.putUUID(tag, "Destination", destinationId);
        Nbt.putUUID(tag, "SourceDestination", sourceDestinationId);
        Nbt.putUUID(tag, "OriginalAuthor", originalAuthorId);
        tag.putString("OriginalAuthorName", originalAuthorName);
        Nbt.putUUID(tag, "SharedBy", sharedById);
        tag.putString("SharedByName", sharedByName);
        return tag;
    }

    public static ShareProvenance load(CompoundTag tag) {
        if (!Nbt.hasUUID(tag, "SourceDestination") || !Nbt.hasUUID(tag, "OriginalAuthor")
            || !Nbt.hasUUID(tag, "SharedBy")) return null;
        return new ShareProvenance(Nbt.getUUID(tag, "SourceDestination"),
            Nbt.getUUID(tag, "OriginalAuthor"), Nbt.getString(tag, "OriginalAuthorName"),
            Nbt.getUUID(tag, "SharedBy"), Nbt.getString(tag, "SharedByName"));
    }
}

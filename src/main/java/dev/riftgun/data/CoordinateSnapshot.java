package dev.riftgun.data;

import dev.riftgun.core.nbt.Nbt;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.level.Level;

/** Immutable, portable coordinate-share payload. */
public record CoordinateSnapshot(
    UUID snapshotId,
    UUID sourceDestinationId,
    String name,
    ResourceKey<Level> dimension,
    double x,
    double y,
    double z,
    float yaw,
    UUID originalAuthorId,
    String originalAuthorName,
    UUID sharedById,
    String sharedByName
) {
    public boolean valid() {
        return !name.isBlank() && name.length() <= 128
            && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && Float.isFinite(yaw);
    }

    public CoordinateSnapshot resharedBy(UUID playerId, String playerName) {
        return new CoordinateSnapshot(UUID.randomUUID(), sourceDestinationId, name, dimension,
            x, y, z, yaw, originalAuthorId, originalAuthorName, playerId, playerName);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        Nbt.putUUID(tag, "Snapshot", snapshotId);
        Nbt.putUUID(tag, "SourceDestination", sourceDestinationId);
        tag.putString("Name", name);
//? if >=1.21.11 {
        /*tag.putString("Dimension", dimension.identifier().toString());
*///?} else {
        tag.putString("Dimension", dimension.location().toString());
//?}
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("Yaw", yaw);
        Nbt.putUUID(tag, "OriginalAuthor", originalAuthorId);
        tag.putString("OriginalAuthorName", originalAuthorName);
        Nbt.putUUID(tag, "SharedBy", sharedById);
        tag.putString("SharedByName", sharedByName);
        return tag;
    }

    public static CoordinateSnapshot load(CompoundTag tag) {
//? if >=1.21.11 {
        /*Identifier dimensionId = Identifier.tryParse(Nbt.getString(tag, "Dimension"));
*///?} else {
        ResourceLocation dimensionId = ResourceLocation.tryParse(Nbt.getString(tag, "Dimension"));
//?}
        if (dimensionId == null || !Nbt.hasUUID(tag, "Snapshot")
            || !Nbt.hasUUID(tag, "SourceDestination") || !Nbt.hasUUID(tag, "OriginalAuthor")
            || !Nbt.hasUUID(tag, "SharedBy")) return null;
        CoordinateSnapshot snapshot = new CoordinateSnapshot(
            Nbt.getUUID(tag, "Snapshot"), Nbt.getUUID(tag, "SourceDestination"),
            Nbt.getString(tag, "Name"), ResourceKey.create(Registries.DIMENSION, dimensionId),
            Nbt.getDouble(tag, "X"), Nbt.getDouble(tag, "Y"), Nbt.getDouble(tag, "Z"),
            Nbt.getFloat(tag, "Yaw"), Nbt.getUUID(tag, "OriginalAuthor"),
            Nbt.getString(tag, "OriginalAuthorName"), Nbt.getUUID(tag, "SharedBy"),
            Nbt.getString(tag, "SharedByName"));
        return snapshot.valid() ? snapshot : null;
    }
}

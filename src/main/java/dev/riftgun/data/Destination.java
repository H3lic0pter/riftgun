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
import net.minecraft.world.phys.Vec3;

public record Destination(
    UUID id,
    String name,
    UUID groupId,
    ResourceKey<Level> dimension,
    double x,
    double y,
    double z,
    float yaw,
    long createdAt,
    long lastUsedAt,
    boolean pinned
) {
    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    public Destination withDetails(String nextName, UUID nextGroupId, ResourceKey<Level> nextDimension,
                                   double nextX, double nextY, double nextZ, float nextYaw) {
        return new Destination(id, nextName, nextGroupId, nextDimension, nextX, nextY, nextZ, nextYaw,
            createdAt, lastUsedAt, pinned);
    }

    public Destination withGroup(UUID nextGroupId) {
        return withDetails(name, nextGroupId, dimension, x, y, z, yaw);
    }

    public Destination withPinned(boolean nextPinned) {
        return new Destination(id, name, groupId, dimension, x, y, z, yaw, createdAt, lastUsedAt, nextPinned);
    }

    public Destination usedAt(long time) {
        return new Destination(id, name, groupId, dimension, x, y, z, yaw, createdAt, time, pinned);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        Nbt.putUUID(tag, "Id", id);
        tag.putString("Name", name);
        Nbt.putUUID(tag, "Group", groupId);
                //? if >=1.21.11 {
        /*tag.putString("Dimension", dimension.identifier().toString());
        *///?} else {
        tag.putString("Dimension", dimension.location().toString());
        //?}
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("Yaw", yaw);
        tag.putLong("CreatedAt", createdAt);
        tag.putLong("LastUsedAt", lastUsedAt);
        tag.putBoolean("Pinned", pinned);
        return tag;
    }

    public static Destination load(CompoundTag tag) {
//? if >=1.21.11 {
        /*Identifier dimensionId = Identifier.tryParse(Nbt.getString(tag, "Dimension"));
*///?} else {
        ResourceLocation dimensionId = ResourceLocation.tryParse(Nbt.getString(tag, "Dimension"));
//?}
                //? if >=1.21.11 {
        /*if (dimensionId == null) dimensionId = Level.OVERWORLD.identifier();
        *///?} else {
        if (dimensionId == null) dimensionId = Level.OVERWORLD.location();
        //?}
        return new Destination(
            Nbt.hasUUID(tag, "Id") ? Nbt.getUUID(tag, "Id") : UUID.randomUUID(),
            Nbt.getString(tag, "Name"),
            Nbt.hasUUID(tag, "Group") ? Nbt.getUUID(tag, "Group") : PortalPlayerData.DEFAULT_GROUP_ID,
            ResourceKey.create(Registries.DIMENSION, dimensionId),
            Nbt.getDouble(tag, "X"),
            Nbt.getDouble(tag, "Y"),
            Nbt.getDouble(tag, "Z"),
            Nbt.getFloat(tag, "Yaw"),
            Nbt.getLong(tag, "CreatedAt"),
            Nbt.getLong(tag, "LastUsedAt"),
            Nbt.getBoolean(tag, "Pinned")
        );
    }
}


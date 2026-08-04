package dev.riftgun.data;

import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putUUID("Group", groupId);
        tag.putString("Dimension", dimension.location().toString());
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
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimensionId == null) dimensionId = Level.OVERWORLD.location();
        return new Destination(
            tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID(),
            tag.getString("Name"),
            tag.hasUUID("Group") ? tag.getUUID("Group") : PortalPlayerData.DEFAULT_GROUP_ID,
            ResourceKey.create(Registries.DIMENSION, dimensionId),
            tag.getDouble("X"),
            tag.getDouble("Y"),
            tag.getDouble("Z"),
            tag.getFloat("Yaw"),
            tag.getLong("CreatedAt"),
            tag.getLong("LastUsedAt"),
            tag.getBoolean("Pinned")
        );
    }
}


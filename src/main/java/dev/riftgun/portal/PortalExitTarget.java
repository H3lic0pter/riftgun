package dev.riftgun.portal;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.data.Destination;
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
import org.jetbrains.annotations.Nullable;

/** Minimal destination data needed by an entrance whose exit does not exist yet. */
public record PortalExitTarget(
    UUID destinationId,
    ResourceKey<Level> dimension,
    Vec3 position,
    float yaw
) {
    public static PortalExitTarget from(Destination destination) {
        return new PortalExitTarget(destination.id(), destination.dimension(), destination.position(),
            destination.yaw());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        Nbt.putUUID(tag, "DestinationId", destinationId);
        tag.putString("Dimension", dimension.location().toString());
        tag.putDouble("X", position.x);
        tag.putDouble("Y", position.y);
        tag.putDouble("Z", position.z);
        tag.putFloat("Yaw", yaw);
        return tag;
    }

    public static @Nullable PortalExitTarget load(CompoundTag tag) {
        if (!Nbt.hasUUID(tag, "DestinationId")) return null;
//? if >=1.21.11 {
        /*Identifier dimensionId = Identifier.tryParse(Nbt.getString(tag, "Dimension"));
*///?} else {
        ResourceLocation dimensionId = ResourceLocation.tryParse(Nbt.getString(tag, "Dimension"));
//?}
        if (dimensionId == null) return null;
        return new PortalExitTarget(
            Nbt.getUUID(tag, "DestinationId"),
            ResourceKey.create(Registries.DIMENSION, dimensionId),
            new Vec3(Nbt.getDouble(tag, "X"), Nbt.getDouble(tag, "Y"), Nbt.getDouble(tag, "Z")),
            Nbt.getFloat(tag, "Yaw")
        );
    }
}

package dev.riftgun.network;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.service.SurfaceFaceSelection;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/** Validated wire contract for opening on an explicitly selected face of one anchor block. */
public record SurfaceFaceRequest(BlockPos anchor, Direction face) {
    public SurfaceFaceRequest {
        if (anchor == null || face == null) throw new IllegalArgumentException("anchor and face are required");
        anchor = anchor.immutable();
    }

    public CompoundTag encode() {
        CompoundTag tag = new CompoundTag();
        writeTo(tag);
        return tag;
    }

    public void writeTo(CompoundTag tag) {
        tag.putInt("AnchorX", anchor.getX());
        tag.putInt("AnchorY", anchor.getY());
        tag.putInt("AnchorZ", anchor.getZ());
        tag.putString("Face", face.getName());
    }

    public SurfaceFaceSelection toSelection() {
        return new SurfaceFaceSelection(anchor, face);
    }

    public static SurfaceFaceRequest decode(CompoundTag tag) {
        if (!tag.contains("AnchorX") || !tag.contains("AnchorY") || !tag.contains("AnchorZ")
            || !tag.contains("Face")) {
            throw PortalRequestFields.error("message.riftgun.invalid_request");
        }
        Direction face = Arrays.stream(Direction.values())
            .filter(value -> value.getName().equalsIgnoreCase(Nbt.getString(tag, "Face")))
            .findFirst().orElseThrow(() ->
                PortalRequestFields.error("message.riftgun.invalid_request"));
        return new SurfaceFaceRequest(new BlockPos(
            Nbt.getInt(tag, "AnchorX"), Nbt.getInt(tag, "AnchorY"),
            Nbt.getInt(tag, "AnchorZ")), face);
    }
}

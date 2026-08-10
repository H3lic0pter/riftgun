package dev.riftgun.portal;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** The complete, synchronized attachment state of a portal. */
public record PortalAttachment(@Nullable BlockPos anchor, @Nullable Direction face) {
    private static final String ANCHOR = "Anchor";
    private static final String FACE = "Face";
    private static final PortalAttachment NONE = new PortalAttachment(null, null);

    public PortalAttachment {
        if (anchor == null || face == null) {
            anchor = null;
            face = null;
        }
    }

    public static PortalAttachment none() {
        return NONE;
    }

    public static PortalAttachment of(@Nullable BlockPos anchor, @Nullable Direction face) {
        return anchor == null || face == null ? NONE : new PortalAttachment(anchor.immutable(), face);
    }

    public static PortalAttachment fromSynced(Optional<BlockPos> anchor, int faceOrdinal) {
        Direction[] directions = Direction.values();
        if (anchor.isEmpty() || faceOrdinal < 0 || faceOrdinal >= directions.length) return NONE;
        return of(anchor.get(), directions[faceOrdinal]);
    }

    public boolean anchored() {
        return anchor != null;
    }

    public Optional<BlockPos> syncedAnchor() {
        return Optional.ofNullable(anchor);
    }

    public int syncedFace() {
        return face == null ? -1 : face.ordinal();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (anchored()) {
            tag.putLong(ANCHOR, anchor.asLong());
            tag.putString(FACE, face.name());
        }
        return tag;
    }

    public static PortalAttachment load(CompoundTag tag) {
        if (!tag.contains(ANCHOR) || !tag.contains(FACE)) return NONE;
        try {
            return of(BlockPos.of(tag.getLong(ANCHOR)), Direction.valueOf(tag.getString(FACE)));
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}

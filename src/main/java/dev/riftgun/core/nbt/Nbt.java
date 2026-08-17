package dev.riftgun.core.nbt;
import dev.riftgun.core.nbt.Nbt;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Version seam for the 1.21.6 NBT accessor rework.
 *
 * <p>26.x replaced the scalar getters ({@code getString}/{@code getInt}/...) with
 * {@code Optional}-returning variants plus {@code getXOr(key, default)} defaults,
 * dropped {@code getList(key, type)}/{@code contains(key, type)} in favour of
 * untyped forms, and removed the UUID helpers entirely. The helpers below keep
 * the 1.21.1 semantics (defaults for missing scalars, presence checks, the
 * four-int UUID array format) so shared code stays version-neutral and save
 * data stays compatible.
 */
public final class Nbt {
    public static String getString(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getStringOr(key, "");
        *///?} else {
        return tag.getString(key);
        //?}
    }

    public static int getInt(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getIntOr(key, 0);
        *///?} else {
        return tag.getInt(key);
        //?}
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getBooleanOr(key, false);
        *///?} else {
        return tag.getBoolean(key);
        //?}
    }

    public static double getDouble(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getDoubleOr(key, 0.0);
        *///?} else {
        return tag.getDouble(key);
        //?}
    }

    public static long getLong(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getLongOr(key, 0L);
        *///?} else {
        return tag.getLong(key);
        //?}
    }

    public static short getShort(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getShortOr(key, (short) 0);
        *///?} else {
        return tag.getShort(key);
        //?}
    }

    public static float getFloat(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getFloatOr(key, 0.0F);
        *///?} else {
        return tag.getFloat(key);
        //?}
    }

    public static byte getByte(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getByteOr(key, (byte) 0);
        *///?} else {
        return tag.getByte(key);
        //?}
    }

    public static CompoundTag getCompound(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getCompoundOrEmpty(key);
        *///?} else {
        return tag.getCompound(key);
        //?}
    }

    public static ListTag getList(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getListOrEmpty(key);
        *///?} else {
        return tag.getList(key, Tag.TAG_COMPOUND);
        //?}
    }

    /** Presence check; on 26.x the typed two-argument contains() no longer exists. */
    public static boolean contains(CompoundTag tag, String key) {
        return tag.contains(key);
    }

    /** Typed presence check; 26.x only has the untyped form, so compare tag ids. */
    public static boolean contains(CompoundTag tag, String key, int type) {
        //? if >=1.21.11 {
        /*Tag value = tag.get(key);
        return value != null && value.getId() == type;
        *///?} else {
        return tag.contains(key, type);
        //?}
    }

    public static boolean hasUUID(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getIntArray(key).isPresent() || tag.getLongArray(key).isPresent();
        *///?} else {
        return tag.hasUUID(key);
        //?}
    }

    public static UUID getUUID(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*var ints = tag.getIntArray(key);
        if (ints.isPresent()) {
            return UUIDUtil.uuidFromIntArray(ints.get());
        }
        long[] longs = tag.getLongArray(key).orElse(null);
        if (longs != null && longs.length == 2) {
            // 26.x stores UUIDs as 4-int arrays; 1.21.1 wrote 2-long arrays.
            return new UUID(longs[0], longs[1]);
        }
        throw new IllegalArgumentException("Invalid UUID tag for " + key);
        *///?} else {
        return tag.getUUID(key);
        //?}
    }

    public static void putUUID(CompoundTag tag, String key, UUID uuid) {
        //? if >=1.21.11 {
        /*tag.putIntArray(key, UUIDUtil.uuidToIntArray(uuid));
        *///?} else {
        tag.putUUID(key, uuid);
        //?}
    }

    private Nbt() {}
}

package dev.riftgun.data;

import dev.riftgun.RiftGun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PortalDataStore {
    private static final String ROOT_KEY = RiftGun.MOD_ID + ":portal_data";

    public static PortalPlayerData load(Player player) {
        CompoundTag persistent = player.getPersistentData();
        return PortalPlayerData.load(persistent.getCompound(ROOT_KEY));
    }

    public static void save(Player player, PortalPlayerData data) {
        player.getPersistentData().put(ROOT_KEY, data.save());
    }

    public static void copy(Player original, Player replacement) {
        CompoundTag source = original.getPersistentData().getCompound(ROOT_KEY);
        replacement.getPersistentData().put(ROOT_KEY, source.copy());
    }

    public static CompoundTag snapshot(ServerPlayer player) {
        return load(player).save();
    }

    private PortalDataStore() {}
}

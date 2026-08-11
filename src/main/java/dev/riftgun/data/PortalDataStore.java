package dev.riftgun.data;

import dev.riftgun.RiftGun;
import dev.riftgun.config.ServerConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PortalDataStore {
    private static final String ROOT_KEY = RiftGun.MOD_ID + ":portal_data";

    public static PortalPlayerData load(Player player) {
        CompoundTag persistent = player.getPersistentData();
        return PortalPlayerData.load(persistent.getCompound(ROOT_KEY), permissionDefaults());
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

    private static Map<ResourceLocation, PortalPermissionPolicy> permissionDefaults() {
        Map<ResourceLocation, PortalPermissionPolicy> defaults = new HashMap<>();
        defaults.put(PortalPermissions.PLAYER_PORTAL,
            fromTargetPrivacy(ServerConfig.VALUES.defaultTargetPrivacy.get()));
        defaults.put(PortalPermissions.ENTITY_RELOCATION_DESTINATION,
            fromTargetPrivacy(ServerConfig.VALUES.defaultEntityRelocationDestinationPrivacy.get()));
        defaults.put(PortalPermissions.ENTITY_RELOCATION_SUBJECT,
            fromTargetPrivacy(ServerConfig.VALUES.defaultEntityRelocationSubjectPrivacy.get()));
        defaults.put(PortalPermissions.FOREIGN_EXIT_TRANSIT,
            ServerConfig.VALUES.defaultForeignExitTransitAllowed.get()
                ? PortalPermissionPolicy.ALLOW : PortalPermissionPolicy.DENY);
        return defaults;
    }

    private static PortalPermissionPolicy fromTargetPrivacy(TargetPrivacy privacy) {
        return switch (privacy) {
            case PUBLIC -> PortalPermissionPolicy.ALLOW;
            case REQUEST -> PortalPermissionPolicy.ASK;
            case PRIVATE -> PortalPermissionPolicy.DENY;
        };
    }

    private PortalDataStore() {}
}

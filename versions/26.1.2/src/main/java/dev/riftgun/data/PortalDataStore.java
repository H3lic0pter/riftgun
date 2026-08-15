package dev.riftgun.data;

import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.RiftConstants;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PortalDataStore {
    private static final String ROOT_KEY = RiftConstants.MOD_ID + ":portal_data";

    public static PortalPlayerData load(Player player) {
        CompoundTag persistent = player.getPersistentData();
        return PortalPlayerData.load(Nbt.getCompound(persistent, ROOT_KEY), permissionDefaults());
    }

    public static void save(Player player, PortalPlayerData data) {
        player.getPersistentData().put(ROOT_KEY, data.save());
    }

    public static void copy(Player original, Player replacement) {
        CompoundTag source = Nbt.getCompound(original.getPersistentData(), ROOT_KEY);
        replacement.getPersistentData().put(ROOT_KEY, source.copy());
    }

    public static CompoundTag snapshot(ServerPlayer player) {
        return load(player).save();
    }

    private static Map<Identifier, PortalPermissionPolicy> permissionDefaults() {
        Map<Identifier, PortalPermissionPolicy> defaults = new HashMap<>();
        defaults.put(PortalPermissions.PLAYER_PORTAL,
            fromTargetPrivacy(RiftConfigs.server().privacy().defaultTarget()));
        defaults.put(PortalPermissions.ENTITY_RELOCATION_DESTINATION,
            fromTargetPrivacy(RiftConfigs.server().privacy().defaultRelocationDestination()));
        defaults.put(PortalPermissions.ENTITY_RELOCATION_SUBJECT,
            fromTargetPrivacy(RiftConfigs.server().privacy().defaultRelocationSubject()));
        defaults.put(PortalPermissions.FOREIGN_EXIT_TRANSIT,
            RiftConfigs.server().privacy().foreignExitTransitAllowed()
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

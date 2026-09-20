package dev.riftgun.portal;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import dev.riftgun.pairing.PortalPairingEndpoint;
import dev.riftgun.pairing.PortalPairingLegacyMigration;
import dev.riftgun.service.PortalClientSync;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Owns replacement and clearing rules for each player's coordinate and pairing groups. */
public final class PortalInstances {
    private static final String KEY = "riftgun:portal_instances";

    public static PortalInstanceState state(ServerPlayer player) {
        if (!player.getPersistentData().contains(KEY)) {
            var legacy = PortalPairingLegacyMigration.fromGuns(player);
            store(player, legacy == null ? PortalInstanceState.EMPTY : PortalInstanceState.EMPTY.withPending(legacy));
        }
        var state = PortalInstanceState.load(Nbt.getCompound(player.getPersistentData(), KEY));
        if (state.pending() != null && !state.pending().belongsTo(player.getUUID())) {
            state = state.withPending(null);
            store(player, state);
        }
        return state;
    }

    public static @Nullable PortalPairingPendingEndpoint pending(ServerPlayer player) {
        return state(player).pending();
    }

    public static List<PortalEntity> opened(ServerPlayer player, PortalFunctionMode mode) {
        return PortalOwnerIndex.owned(server(player), player.getUUID()).stream()
            .filter(portal -> portal.functionMode() == mode && !portal.pairingDormant())
            .filter(portal -> portal.phase() != PortalLifecycle.Phase.CLOSING
                && portal.phase() != PortalLifecycle.Phase.CLOSED)
            .toList();
    }

    /** Called only after entities and fuel have committed successfully. */
    public static void replaceOpened(ServerPlayer player, PortalFunctionMode mode, Set<UUID> retained) {
        closeOpened(server(player), player.getUUID(), mode, retained);
        if (mode == PortalFunctionMode.PORTAL_PAIRING) clearPending(player);
    }

    public static void placePending(ServerPlayer player, PortalPairingPendingEndpoint marker) {
        if (!marker.belongsTo(player.getUUID())) throw new IllegalArgumentException("marker owner mismatch");
        closeOpened(server(player), player.getUUID(), PortalFunctionMode.PORTAL_PAIRING, Set.of());
        update(player, state(player).withPending(marker));
    }

    public static void clearPending(ServerPlayer player) {
        update(player, state(player).withPending(null));
    }

    public static void clearMode(ServerPlayer player, PortalFunctionMode mode) {
        replaceOpened(player, mode, Set.of());
    }

    public static void clearAll(ServerPlayer player) {
        closeOpened(server(player), player.getUUID());
        clearPending(player);
    }

    /** Logout/respawn closes entities, but preserves persistent markers. */
    public static void closeOpened(MinecraftServer server, UUID owner) {
        PortalOwnerIndex.closeOwned(server, owner, Set.of());
    }

    private static void closeOpened(MinecraftServer server, UUID owner,
                                   PortalFunctionMode mode, Set<UUID> retained) {
        PortalOwnerIndex.closeOwnedMatching(server, owner,
            portal -> portal.functionMode() == mode && !retained.contains(portal.getUUID()));
    }

    /** Convert old marker entities without requiring their original gun to still exist. */
    public static boolean migrateLegacyPortal(PortalEntity portal, UUID ownerId) {
        var endpoint = portal.pairingEndpoint();
        if (!portal.pairingDormant() || ownerId == null || endpoint == PortalPairingEndpoint.NONE
            || !(portal.level() instanceof ServerLevel level)) return false;
        var owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return false;
        importLegacy(owner, new PortalPairingPendingEndpoint(
            ownerId, level.dimension(), portal.placement(), endpoint));
        portal.discard();
        return true;
    }

    static void importLegacy(ServerPlayer player, PortalPairingPendingEndpoint marker) {
        var state = state(player);
        if (!state.pairingManaged() && marker.belongsTo(player.getUUID())) {
            update(player, state.withPending(marker));
        }
    }

    public static void copy(Player original, Player replacement) {
        if (original.getPersistentData().contains(KEY)) {
            replacement.getPersistentData().put(KEY, Nbt.getCompound(original.getPersistentData(), KEY).copy());
        }
    }

    private static void update(ServerPlayer player, PortalInstanceState next) {
        store(player, next);
        PortalClientSync.instances(player, next);
    }

    private static void store(ServerPlayer player, PortalInstanceState state) {
        player.getPersistentData().put(KEY, state.save());
    }

    private static MinecraftServer server(ServerPlayer player) {
        return ((ServerLevel) player.level()).getServer();
    }

    private PortalInstances() {}
}

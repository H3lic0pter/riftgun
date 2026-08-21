package dev.riftgun.service;

import dev.riftgun.core.config.RiftConfig;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.msg.Msg;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.network.PortalNetworking;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.border.WorldBorder;

/** Incremental random-destination search. At most one candidate is loaded per player per server tick. */
public final class RandomRiftManager {
    private static final int MAXIMUM_ATTEMPTS = 16;
    private static final int PREPARATION_TIMEOUT_TICKS = 100;
    private static final int PREPARATION_TICKET_RADIUS = 3;
    private static final int NETHER_ROOF_MARGIN = 8;
//? if >=1.21.11 {
    /*private static final TicketType PREPARATION_TICKET =
        dev.riftgun.portal.PortalChunkTickets.RANDOM_RIFT_PREPARATION.get();
*///?} else {
    private static final TicketType<UUID> PREPARATION_TICKET =
        TicketType.create("riftgun_random_rift_preparation", UUID::compareTo);
//?}
    private static final Map<UUID, Search> SEARCHES = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final UUID SEARCH_PROBE_ID = new UUID(0L, 0L);
    private static final VanillaDestinationSafetyInspector SAFETY_INSPECTOR =
        new VanillaDestinationSafetyInspector();

    public static void request(ServerPlayer player, PortalGunLocator.LocatedGun gun) {
        RiftConfig.RandomRiftConfig config = RiftConfigs.server().randomRift();
        if (!config.enabled()) {
            message(player, "message.riftgun.random_rift_disabled");
            return;
        }
        PortalPlayerData data = PortalDataStore.load(player);
        if (!PortalGunCapabilities.resolve(gun.stack(), data.settings().smartDistance()).coordinateOverride()) {
            message(player, "message.riftgun.coordinate_module_required");
            return;
        }
        UUID playerId = player.getUUID();
        if (SEARCHES.containsKey(playerId)) {
            message(player, "message.riftgun.random_rift_searching");
            return;
        }
        int cooldown = cooldownTicks(player);
        if (cooldown > 0) {
            message(player, "message.riftgun.random_rift_cooldown", (cooldown + 19) / 20);
            return;
        }
        SEARCHES.put(playerId, new Search(player.level().dimension(), player.getX(), player.getZ(),
            gun.saveReference()));
        message(player, "message.riftgun.random_rift_search_started");
        PortalNetworking.sendSnapshot(player, false, gun);
    }

    public static void tick(MinecraftServer server) {
        for (UUID playerId : new ArrayList<>(SEARCHES.keySet())) {
            Search search = SEARCHES.get(playerId);
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (search == null || player == null) {
                SEARCHES.remove(playerId);
                continue;
            }
            tickSearch(player, search);
        }
    }

    public static Snapshot snapshot(ServerPlayer player) {
        return new Snapshot(RiftConfigs.server().randomRift().enabled(),
            SEARCHES.containsKey(player.getUUID()), cooldownTicks(player));
    }

    public static void playerLeft(ServerPlayer player) {
        cancel(playerServer(player), player.getUUID());
        COOLDOWNS.remove(player.getUUID());
    }

    public static void playerChangedDimension(ServerPlayer player) {
        cancel(playerServer(player), player.getUUID());
    }

    public static void cancelAll(MinecraftServer server) {
        for (Search search : SEARCHES.values()) releasePreparation(server, search);
        SEARCHES.clear();
    }

    public static void reset() {
        for (MinecraftServer server : new ArrayList<>(SEARCHES.keySet())) {
            cancelAll(server);
        }
        SEARCHES.clear();
        COOLDOWNS.clear();
    }

    private static void tickSearch(ServerPlayer player, Search search) {
        RiftConfig.RandomRiftConfig config = RiftConfigs.server().randomRift();
        PortalGunLocator.LocatedGun gun = PortalGunLocator.resolveReference(player, search.gunReference)
            .orElse(null);
        if (!config.enabled() || !player.level().dimension().equals(search.dimension) || gun == null) {
            cancel(playerServer(player), player.getUUID());
            message(player, "message.riftgun.random_rift_canceled");
            return;
        }
        PortalPlayerData data = PortalDataStore.load(player);
        if (!PortalGunCapabilities.resolve(gun.stack(), data.settings().smartDistance()).coordinateOverride()) {
            cancel(playerServer(player), player.getUUID());
            message(player, "message.riftgun.coordinate_module_required");
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        if (!search.preparing()) {
            beginCandidate(level, search, config, now);
            return;
        }
        if (!level.isPositionEntityTicking(search.candidatePosition())) {
            if (now - search.preparationStartedAt >= PREPARATION_TIMEOUT_TICKS) {
                removePreparationTicket(level, search);
                search.clearCandidate();
                finishIfExhausted(player, gun, search);
            }
            return;
        }

        removePreparationTicket(level, search);
        BlockPos target = inspectCandidate(level, search.candidateX, search.candidateZ);
        search.clearCandidate();
        if (target != null) {
            long time = now;
            Destination destination = new Destination(UUID.randomUUID(), "Unknown Rift",
                PortalPlayerData.DEFAULT_GROUP_ID, level.dimension(), target.getX() + 0.5,
                target.getY(), target.getZ() + 0.5, player.getYRot(), time, 0L, false);
            SEARCHES.remove(player.getUUID());
            boolean opened = PortalOpenCoordinator.openTransient(player, data, destination,
                PortalPlacementMode.FRONT, gun, true);
            if (opened) {
                int cooldownTicks = config.cooldownTicks();
                if (cooldownTicks > 0) COOLDOWNS.put(player.getUUID(), time + cooldownTicks);
            }
            PortalNetworking.sendSnapshot(player, false, gun);
            if (opened) PortalNetworking.sendPortalOpened(player);
            return;
        }
        finishIfExhausted(player, gun, search);
    }

    private static void beginCandidate(ServerLevel level, Search search,
                                       RiftConfig.RandomRiftConfig config, long now) {
        RandomRiftGeometry.Offset offset = RandomRiftGeometry.sample(
            config.innerRadius(), config.outerRadius(), RANDOM::nextDouble);
        WorldBorder border = level.getWorldBorder();
        int x = (int) Math.floor(clamp(search.centerX + offset.x(),
            border.getMinX() + 1.0, border.getMaxX() - 1.0));
        int z = (int) Math.floor(clamp(search.centerZ + offset.z(),
            border.getMinZ() + 1.0, border.getMaxZ() - 1.0));
        search.beginCandidate(x, z, now);
        addPreparationTicket(level, search);
    }

    private static BlockPos inspectCandidate(ServerLevel level, int x, int z) {
        int top = Math.min(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z),
//? if >=1.21.11 {
            /*level.dimensionType().minY() + level.dimensionType().height() - 2);
        int bottom = level.dimensionType().minY() + 1;
        int maximumBuildHeight = level.dimensionType().minY() + level.dimensionType().height();
*///?} else {
            level.getMaxBuildHeight() - 2);
        int bottom = level.getMinBuildHeight() + 1;
        int maximumBuildHeight = level.getMaxBuildHeight();
//?}
        for (int y = top; y >= bottom; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos support = feet.below();
            if (level.dimension().equals(Level.NETHER)
                && support.getY() >= maximumBuildHeight - NETHER_ROOF_MARGIN
                && level.getBlockState(support).is(Blocks.BEDROCK)) continue;
            Destination probe = new Destination(SEARCH_PROBE_ID, "", PortalPlayerData.DEFAULT_GROUP_ID,
                level.dimension(), x + 0.5, y, z + 0.5, 0.0F, 0L, 0L, false);
            if (SAFETY_INSPECTOR.inspect(level, probe).safe()) return feet;
        }
        return null;
    }

    private static void finishIfExhausted(ServerPlayer player, PortalGunLocator.LocatedGun gun,
                                          Search search) {
        if (search.attempts < RiftConfigs.server().randomRift().maximumAttempts()) return;
        SEARCHES.remove(player.getUUID());
        message(player, "message.riftgun.random_rift_failed");
        PortalNetworking.sendSnapshot(player, false, gun);
    }

    private static void addPreparationTicket(ServerLevel level, Search search) {
//? if >=1.21.11 {
        /*level.getChunkSource().addTicketWithRadius(
            PREPARATION_TICKET, search.candidateChunk, PREPARATION_TICKET_RADIUS);
*///?} else {
        level.getChunkSource().addRegionTicket(PREPARATION_TICKET, search.candidateChunk,
            PREPARATION_TICKET_RADIUS, search.ticketId, true);
//?}
    }

    private static void removePreparationTicket(ServerLevel level, Search search) {
        if (!search.preparing()) return;
//? if >=1.21.11 {
        /*level.getChunkSource().removeTicketWithRadius(
            PREPARATION_TICKET, search.candidateChunk, PREPARATION_TICKET_RADIUS);
*///?} else {
        level.getChunkSource().removeRegionTicket(PREPARATION_TICKET, search.candidateChunk,
            PREPARATION_TICKET_RADIUS, search.ticketId, true);
//?}
    }

    private static void releasePreparation(MinecraftServer server, Search search) {
        if (!search.preparing()) return;
        ServerLevel level = server.getLevel(search.dimension);
        if (level != null) removePreparationTicket(level, search);
        search.clearCandidate();
    }

    private static void cancel(MinecraftServer server, UUID playerId) {
        Search search = SEARCHES.remove(playerId);
        if (search != null && server != null) releasePreparation(server, search);
    }

    private static MinecraftServer playerServer(ServerPlayer player) {
//? if >=1.21.11 {
        /*return player.level().getServer();
*///?} else {
        return player.getServer();
//?}
    }

    private static int cooldownTicks(ServerPlayer player) {
        long remaining = COOLDOWNS.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime();
        if (remaining <= 0) {
            COOLDOWNS.remove(player.getUUID());
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void message(ServerPlayer player, String key, Object... arguments) {
        Msg.displayClientMessage(player, Component.translatable(key, arguments), true);
    }

    public record Snapshot(boolean enabled, boolean searching, int cooldownTicks) {}

    private static final class Search {
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final double centerX;
        private final double centerZ;
        private final CompoundTag gunReference;
        private final UUID ticketId = UUID.randomUUID();
        private int attempts;
        private int candidateX;
        private int candidateZ;
        private ChunkPos candidateChunk;
        private long preparationStartedAt;

        private Search(net.minecraft.resources.ResourceKey<Level> dimension, double centerX,
                       double centerZ, CompoundTag gunReference) {
            this.dimension = dimension;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.gunReference = gunReference;
        }

        private boolean preparing() {
            return candidateChunk != null;
        }

        private void beginCandidate(int x, int z, long now) {
            attempts++;
            candidateX = x;
            candidateZ = z;
            candidateChunk = new ChunkPos(x >> 4, z >> 4);
            preparationStartedAt = now;
        }

        private BlockPos candidatePosition() {
            return new BlockPos(candidateX, 0, candidateZ);
        }

        private void clearCandidate() {
            candidateChunk = null;
            preparationStartedAt = 0L;
        }
    }

    private RandomRiftManager() {}
}

package dev.riftgun.service;

import dev.riftgun.core.config.RiftConfig;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.msg.Msg;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.navigation.DimensionalTraversalTargets;
import dev.riftgun.fuel.PortalFuelManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
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
    private static final int PREPARATION_TIMEOUT_TICKS = 100;
    private static final int PREPARATION_TICKET_RADIUS = 3;
    private static final int CEILING_BEDROCK_MARGIN = 8;
//? if >=1.21.11 {
    /*private static final TicketType PREPARATION_TICKET =
        dev.riftgun.portal.PortalChunkTickets.RANDOM_RIFT_PREPARATION.get();
*///?} else {
    private static final TicketType<UUID> PREPARATION_TICKET =
        TicketType.create("riftgun_random_rift_preparation", UUID::compareTo);
//?}
    private static final Map<MinecraftServer, Map<UUID, Search>> SEARCHES = new IdentityHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> COOLDOWNS = new IdentityHashMap<>();
    private static final Map<MinecraftServer, ReferenceCountedLeaseTracker<PreparationTicketKey>>
        PREPARATION_LEASES = new IdentityHashMap<>();
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
        startSearch(player, gun, config, () -> Search.local(
            player.level().dimension(), player.getX(), player.getZ(), gun.saveReference()));
    }

    public static void requestDimensional(ServerPlayer player, PortalGunLocator.LocatedGun gun,
                                          String dimensionId) {
        RiftConfig config = RiftConfigs.server();
        if (!config.dimensionalTraversal().enabled()) {
            message(player, "message.riftgun.dimensional_traversal_disabled");
            return;
        }
        if (!config.randomRift().enabled()) {
            message(player, "message.riftgun.random_rift_disabled");
            return;
        }
        PortalPlayerData data = PortalDataStore.load(player);
        if (!PortalGunCapabilities.resolve(gun.stack(), data.settings().smartDistance())
            .dimensionalTraversal()) {
            message(player, "message.riftgun.dimensional_traversal_module_required");
            return;
        }
        ServerLevel target = DimensionalTraversalTargets.resolve(player, dimensionId).orElse(null);
        if (target == null) {
            message(player, "message.riftgun.dimension_unavailable");
            return;
        }
        startSearch(player, gun, config.randomRift(), () -> {
            PortalFuelManager.Plan fuel = PortalFuelManager.plan(
                player, gun.stack(), target.dimension());
            if (!fuel.successful()) {
                message(player, fuel.errorKey());
                return null;
            }
            double centerX = DimensionalTraversalTargets.mapCoordinate(
                player.getX(), player.level(), target);
            double centerZ = DimensionalTraversalTargets.mapCoordinate(
                player.getZ(), player.level(), target);
            return Search.dimensional(player.level().dimension(), target.dimension(),
                centerX, centerZ, gun.saveReference());
        });
    }

    private static void startSearch(ServerPlayer player, PortalGunLocator.LocatedGun gun,
                                    RiftConfig.RandomRiftConfig config,
                                    Supplier<Search> searchFactory) {
        UUID playerId = player.getUUID();
        MinecraftServer server = playerServer(player);
        Map<UUID, Search> serverSearches = searches(server);
        if (serverSearches.containsKey(playerId)) {
            message(player, "message.riftgun.random_rift_searching");
            return;
        }
        int cooldown = cooldownTicks(player);
        if (cooldown > 0) {
            message(player, "message.riftgun.random_rift_cooldown", (cooldown + 19) / 20);
            return;
        }
        if (!RandomRiftSearchPolicy.hasCapacity(
            serverSearches.size(), config.maximumConcurrentSearches())) {
            message(player, "message.riftgun.random_rift_too_many_searches");
            return;
        }
        Search search = searchFactory.get();
        if (search == null) return;
        serverSearches.put(playerId, search);
        message(player, "message.riftgun.random_rift_search_started");
        PortalClientSync.snapshot(player, false, gun);
    }

    public static void tick(MinecraftServer server) {
        for (UUID playerId : new ArrayList<>(searches(server).keySet())) {
            Search search = searches(server).get(playerId);
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (search == null || player == null) {
                searches(server).remove(playerId);
                continue;
            }
            tickSearch(player, search);
        }
    }

    public static Snapshot snapshot(ServerPlayer player) {
        return new Snapshot(RiftConfigs.server().randomRift().enabled(),
            searches(playerServer(player)).containsKey(player.getUUID()), cooldownTicks(player));
    }

    public static void playerLeft(ServerPlayer player) {
        cancel(playerServer(player), player.getUUID());
        cooldowns(playerServer(player)).remove(player.getUUID());
    }

    public static void playerChangedDimension(ServerPlayer player) {
        cancel(playerServer(player), player.getUUID());
    }

    public static void cancelAll(MinecraftServer server) {
        Map<UUID, Search> searches = SEARCHES.remove(server);
        if (searches != null) {
            for (Search search : searches.values()) releasePreparation(server, search);
        }
        PREPARATION_LEASES.remove(server);
    }

    public static void reset() {
        for (MinecraftServer server : new ArrayList<>(SEARCHES.keySet())) {
            cancelAll(server);
        }
        SEARCHES.clear();
        COOLDOWNS.clear();
        PREPARATION_LEASES.clear();
    }

    private static void tickSearch(ServerPlayer player, Search search) {
        RiftConfig.RandomRiftConfig config = RiftConfigs.server().randomRift();
        PortalGunLocator.LocatedGun gun = PortalGunLocator.resolveReference(player, search.gunReference)
            .orElse(null);
        if (!config.enabled() || !player.level().dimension().equals(search.sourceDimension) || gun == null) {
            cancel(playerServer(player), player.getUUID());
            message(player, "message.riftgun.random_rift_canceled");
            return;
        }
        PortalPlayerData data = PortalDataStore.load(player);
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun.stack(), data.settings().smartDistance());
        String authorizationError = search.kind.authorizationError(capabilities);
        if (authorizationError != null) {
            cancel(playerServer(player), player.getUUID());
            message(player, authorizationError);
            return;
        }

        ServerLevel level = playerServer(player).getLevel(search.targetDimension);
        if (level == null) {
            cancel(playerServer(player), player.getUUID());
            message(player, "message.riftgun.dimension_unavailable");
            return;
        }
        long now = level.getGameTime();
        if (!search.preparing()) {
            beginCandidate(level, search, config, now);
            return;
        }
        RandomRiftSearchPolicy.CandidateProbe probe = RandomRiftSearchPolicy.candidateProbe(
            search.candidateX >> 4, search.candidateZ >> 4, minimumBuildHeight(level));
        if (!level.isPositionEntityTicking(new BlockPos(probe.x(), probe.y(), probe.z()))) {
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
            searches(playerServer(player)).remove(player.getUUID());
            boolean opened = PortalOpenCoordinator.openTransient(player, data, destination,
                PortalPlacementMode.FRONT, gun, true);
            if (opened) {
                int cooldownTicks = config.cooldownTicks();
                if (cooldownTicks > 0) cooldowns(playerServer(player)).put(player.getUUID(), time + cooldownTicks);
            }
            PortalClientSync.snapshot(player, false, gun);
            if (opened) PortalClientSync.portalOpened(player);
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
        int minimumBuildHeight = minimumBuildHeight(level);
        int maximumBuildHeight = maximumBuildHeight(level);
        boolean ceilingDimension = level.dimensionType().hasCeiling();
        int searchCeiling = RandomRiftSearchPolicy.searchCeiling(ceilingDimension, minimumBuildHeight,
            level.dimensionType().logicalHeight(), maximumBuildHeight);
        int top = Math.min(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z),
            searchCeiling - 2);
        int bottom = minimumBuildHeight + 1;
        for (int y = top; y >= bottom; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos support = feet.below();
            if (ceilingDimension
                && support.getY() >= searchCeiling - CEILING_BEDROCK_MARGIN
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
        searches(playerServer(player)).remove(player.getUUID());
        message(player, "message.riftgun.random_rift_failed");
        PortalClientSync.snapshot(player, false, gun);
    }

    private static void addPreparationTicket(ServerLevel level, Search search) {
//? if >=1.21.11 {
        /*PreparationTicketKey lease = new PreparationTicketKey(
            level.dimension(), search.candidateChunk, PREPARATION_TICKET_RADIUS);
        if (preparationLeases(level.getServer()).acquire(lease)) {
            level.getChunkSource().addTicketWithRadius(
                PREPARATION_TICKET, search.candidateChunk, PREPARATION_TICKET_RADIUS);
        }
        search.preparationLease = lease;
*///?} else {
        level.getChunkSource().addRegionTicket(PREPARATION_TICKET, search.candidateChunk,
            PREPARATION_TICKET_RADIUS, search.ticketId, true);
//?}
    }

    private static void removePreparationTicket(ServerLevel level, Search search) {
        if (!search.preparing()) return;
//? if >=1.21.11 {
        /*PreparationTicketKey lease = search.preparationLease;
        if (lease == null) return;
        if (preparationLeases(level.getServer()).release(lease)) {
            level.getChunkSource().removeTicketWithRadius(
                PREPARATION_TICKET, search.candidateChunk, PREPARATION_TICKET_RADIUS);
        }
        search.preparationLease = null;
*///?} else {
        level.getChunkSource().removeRegionTicket(PREPARATION_TICKET, search.candidateChunk,
            PREPARATION_TICKET_RADIUS, search.ticketId, true);
//?}
    }

    private static void releasePreparation(MinecraftServer server, Search search) {
        if (!search.preparing()) return;
        ServerLevel level = server.getLevel(search.targetDimension);
        if (level != null) removePreparationTicket(level, search);
//? if >=1.21.11 {
        /*else if (search.preparationLease != null) {
            preparationLeases(server).release(search.preparationLease);
            search.preparationLease = null;
        }
*///?}
        search.clearCandidate();
    }

    private static void cancel(MinecraftServer server, UUID playerId) {
        Search search = searches(server).remove(playerId);
        if (search != null && server != null) releasePreparation(server, search);
    }

    private static MinecraftServer playerServer(ServerPlayer player) {
//? if >=1.21.11 {
        /*return player.level().getServer();
*///?} else {
        return player.getServer();
//?}
    }

    private static Map<UUID, Search> searches(MinecraftServer server) {
        return SEARCHES.computeIfAbsent(server, ignored -> new HashMap<>());
    }

    private static Map<UUID, Long> cooldowns(MinecraftServer server) {
        return COOLDOWNS.computeIfAbsent(server, ignored -> new HashMap<>());
    }

    private static ReferenceCountedLeaseTracker<PreparationTicketKey> preparationLeases(
        MinecraftServer server
    ) {
        return PREPARATION_LEASES.computeIfAbsent(server,
            ignored -> new ReferenceCountedLeaseTracker<>());
    }

    private static int cooldownTicks(ServerPlayer player) {
        long remaining = cooldowns(playerServer(player)).getOrDefault(player.getUUID(), 0L) - player.level().getGameTime();
        if (remaining <= 0) {
            cooldowns(playerServer(player)).remove(player.getUUID());
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static int minimumBuildHeight(ServerLevel level) {
//? if >=1.21.11 {
        /*return level.dimensionType().minY();
*///?} else {
        return level.getMinBuildHeight();
//?}
    }

    private static int maximumBuildHeight(ServerLevel level) {
//? if >=1.21.11 {
        /*return level.dimensionType().minY() + level.dimensionType().height();
*///?} else {
        return level.getMaxBuildHeight();
//?}
    }

    private static void message(ServerPlayer player, String key, Object... arguments) {
        Msg.displayClientMessage(player, Component.translatable(key, arguments), true);
    }

    public record Snapshot(boolean enabled, boolean searching, int cooldownTicks) {}

    private record PreparationTicketKey(net.minecraft.resources.ResourceKey<Level> dimension,
                                        ChunkPos chunk, int radius) {}

    private enum SearchKind {
        LOCAL {
            @Override
            String authorizationError(PortalGunCapabilities capabilities) {
                return capabilities.coordinateOverride()
                    ? null : "message.riftgun.coordinate_module_required";
            }
        },
        DIMENSIONAL {
            @Override
            String authorizationError(PortalGunCapabilities capabilities) {
                if (!RiftConfigs.server().dimensionalTraversal().enabled()) {
                    return "message.riftgun.dimensional_traversal_disabled";
                }
                return capabilities.dimensionalTraversal()
                    ? null : "message.riftgun.dimensional_traversal_module_required";
            }
        };

        abstract String authorizationError(PortalGunCapabilities capabilities);
    }

    private static final class Search {
        private final net.minecraft.resources.ResourceKey<Level> sourceDimension;
        private final net.minecraft.resources.ResourceKey<Level> targetDimension;
        private final double centerX;
        private final double centerZ;
        private final CompoundTag gunReference;
        private final SearchKind kind;
        private final UUID ticketId = UUID.randomUUID();
        private int attempts;
        private int candidateX;
        private int candidateZ;
        private ChunkPos candidateChunk;
        private PreparationTicketKey preparationLease;
        private long preparationStartedAt;

        private static Search local(net.minecraft.resources.ResourceKey<Level> dimension,
                                    double centerX, double centerZ, CompoundTag gunReference) {
            return new Search(dimension, dimension, centerX, centerZ, gunReference, SearchKind.LOCAL);
        }

        private static Search dimensional(
            net.minecraft.resources.ResourceKey<Level> sourceDimension,
            net.minecraft.resources.ResourceKey<Level> targetDimension,
            double centerX, double centerZ, CompoundTag gunReference
        ) {
            return new Search(sourceDimension, targetDimension, centerX, centerZ,
                gunReference, SearchKind.DIMENSIONAL);
        }

        private Search(net.minecraft.resources.ResourceKey<Level> sourceDimension,
                       net.minecraft.resources.ResourceKey<Level> targetDimension,
                       double centerX, double centerZ, CompoundTag gunReference,
                       SearchKind kind) {
            this.sourceDimension = sourceDimension;
            this.targetDimension = targetDimension;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.gunReference = gunReference;
            this.kind = kind;
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

        private void clearCandidate() {
            candidateChunk = null;
            preparationStartedAt = 0L;
        }
    }

    private RandomRiftManager() {}
}

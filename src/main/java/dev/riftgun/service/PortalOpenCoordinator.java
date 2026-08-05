package dev.riftgun.service;

import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.fuel.PortalFuelManager;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.portal.PortalEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Owns the complete non-blocking open transaction for every placement mode and dimension. */
public final class PortalOpenCoordinator {
    private static final int LOADING_NOTICE_TICKS = 24;
    private static final int PREPARE_TIMEOUT_TICKS = 100;
    private static final int UNSAFE_CONFIRM_TICKS = 300;
    private static final TicketType<UUID> PREPARE_TICKET =
        TicketType.create("riftgun_portal_prepare", UUID::compareTo,
            PREPARE_TIMEOUT_TICKS + UNSAFE_CONFIRM_TICKS + 40);
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static long nextRequestId;

    public static void request(ServerPlayer player, PortalPlayerData data, UUID destinationId,
                               boolean fromGui, boolean confirmedUnsafe, PortalPlacementMode mode,
                               PortalGunLocator.LocatedGun locatedGun) {
        Destination destination = data.destination(destinationId).orElse(null);
        if (destination == null) {
            failMessage(player, "message.riftgun.destination_missing");
            return;
        }

        Pending current = PENDING.get(player.getUUID());
        if (current != null && current.destinationId.equals(destinationId)) {
            if (confirmedUnsafe && current.stage == Stage.WAITING_CONFIRMATION) {
                completePrepared(player, current, true);
            }
            return;
        }
        cancel(player.getUUID(), null);

        var dimensionResult = PortalServices.DIMENSION_POLICY.validate(player, destination);
        if (!dimensionResult.allowed()) {
            player.displayClientMessage(dimensionResult.message(), true);
            return;
        }
        MinecraftServer server = player.getServer();
        ServerLevel targetLevel = server == null ? null : server.getLevel(destination.dimension());
        if (targetLevel == null) {
            failMessage(player, "message.riftgun.dimension_unavailable");
            return;
        }

        PortalPlacementCapture capture = PortalServices.PLACEMENT_RESOLVER.capture(
            player, mode, data.settings().smartDistance());
        if (!capture.successful()) {
            failMessage(player, capture.errorKey());
            return;
        }

        long now = serverTime(server);
        BlockPos targetPosition = BlockPos.containing(destination.position());
        Pending pending = new Pending(++nextRequestId, player.getUUID(), destination.id(),
            DestinationSafetyFingerprint.of(destination), player.level().dimension(), fromGui,
            confirmedUnsafe, capture.intent(), locatedGun.saveReference(), locatedGun.stack(),
            targetLevel, targetPosition, UUID.randomUUID(), now);
        pending.acquire();
        PENDING.put(player.getUUID(), pending);
        PortalNetworking.sendPortalPending(player, destinationId, "Started", null);
        tickOne(server, player, pending, now);
    }

    /** Selection changes cancel work only when the destination actually differs. */
    public static void cancelIfDifferent(ServerPlayer player, UUID destinationId) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending != null && !pending.destinationId.equals(destinationId)) {
            cancel(player.getUUID(), player);
        }
    }

    public static void cancel(ServerPlayer player) {
        cancel(player.getUUID(), player);
    }

    public static void tick(MinecraftServer server) {
        long now = serverTime(server);
        for (Pending pending : new ArrayList<>(PENDING.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId);
            if (player == null) {
                cancel(pending.playerId, null);
                continue;
            }
            tickOne(server, player, pending, now);
        }
    }

    private static void tickOne(MinecraftServer server, ServerPlayer player, Pending pending, long now) {
        if (!isCurrent(pending) || !validPlayerAndGun(player, pending)) {
            cancel(pending.playerId, player);
            return;
        }
        if (pending.stage == Stage.WAITING_CONFIRMATION) {
            if (now >= pending.expiresAt) cancel(pending.playerId, player);
            return;
        }
        long elapsed = now - pending.startedAt;
        if (elapsed >= PREPARE_TIMEOUT_TICKS) {
            fail(player, pending, "message.riftgun.destination_load_timeout");
            return;
        }
        if (!pending.targetLevel.isPositionEntityTicking(pending.targetPosition)) {
            if (!pending.loadingNotified && elapsed >= LOADING_NOTICE_TICKS) {
                pending.loadingNotified = true;
                PortalNetworking.sendPortalPending(player, pending.destinationId, "Loading", null);
                player.displayClientMessage(Component.translatable("message.riftgun.destination_loading"), true);
            }
            return;
        }
        completePrepared(player, pending, pending.confirmedUnsafe);
    }

    private static void completePrepared(ServerPlayer player, Pending pending, boolean confirmedUnsafe) {
        if (!isCurrent(pending) || !validPlayerAndGun(player, pending)) {
            cancel(pending.playerId, player);
            return;
        }
        PortalPlayerData data = PortalDataStore.load(player);
        Destination destination = data.destination(pending.destinationId).orElse(null);
        if (destination == null
            || !DestinationSafetyFingerprint.of(destination).equals(pending.fingerprint)) {
            fail(player, pending, "message.riftgun.destination_changed");
            return;
        }

        SafetyReport report = SafetyReport.SAFE;
        if (data.settings().safetyCheckEnabled()) {
            report = DestinationSafetyCache.inspectPrepared(player, pending.targetLevel, destination);
            if (!report.safe() && pending.fromGui && !confirmedUnsafe) {
                pending.stage = Stage.WAITING_CONFIRMATION;
                pending.expiresAt = serverTime(player.getServer()) + UNSAFE_CONFIRM_TICKS;
                PortalNetworking.sendSafety(player, destination.id(), report.flags(), true);
                PortalNetworking.sendPortalPending(player, destination.id(), "AwaitingConfirmation", null);
                return;
            }
            if (!report.safe()) {
                player.displayClientMessage(Component.translatable("message.riftgun.destination_unsafe"), true);
            }
        }

        Destination resolved = PortalServices.SAFE_DESTINATION_RESOLVER.resolve(
            pending.targetLevel, destination, report);
        PortalPlacementResult placement = PortalServices.PLACEMENT_RESOLVER.resolvePrepared(
            player, resolved, pending.placementIntent);
        if (!placement.successful()) {
            fail(player, pending, placement.errorKey());
            return;
        }
        ItemStack gun = resolveGun(player, pending);
        if (gun == null) {
            cancel(pending.playerId, player);
            return;
        }
        PortalFuelManager.Plan fuelPlan = PortalFuelManager.plan(player, gun, destination.dimension());
        if (!fuelPlan.successful()) {
            fail(player, pending, fuelPlan.errorKey());
            return;
        }
        boolean opened = PortalEntity.openPair(player, placement.pair(), fuelPlan.use().profile(),
            () -> PortalFuelManager.consume(gun, fuelPlan.use()));
        if (!opened) {
            fail(player, pending, "message.riftgun.portal_open_failed");
            return;
        }

        data.selectedDestinationId(destination.id());
        data.replaceDestination(destination.usedAt(player.level().getGameTime()));
        PortalDataStore.save(player, data);
        finish(pending);
        PortalNetworking.sendSnapshot(player, false);
        if (pending.fromGui) PortalNetworking.sendPortalOpened(player);
    }

    private static boolean validPlayerAndGun(ServerPlayer player, Pending pending) {
        return player.isAlive() && !player.isSpectator()
            && player.level().dimension().equals(pending.sourceDimension)
            && resolveGun(player, pending) != null;
    }

    private static ItemStack resolveGun(ServerPlayer player, Pending pending) {
        return PortalGunLocator.resolveReference(player, pending.gunReference)
            .map(PortalGunLocator.LocatedGun::stack)
            .filter(stack -> stack == pending.originalGun)
            .orElse(null);
    }

    private static void fail(ServerPlayer player, Pending pending, String translationKey) {
        finish(pending);
        failMessage(player, translationKey);
        PortalNetworking.sendPortalPending(player, pending.destinationId, "Failed", translationKey);
    }

    private static void failMessage(ServerPlayer player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    private static void finish(Pending pending) {
        if (PENDING.remove(pending.playerId, pending)) pending.release();
    }

    private static void cancel(UUID playerId, ServerPlayer player) {
        Pending removed = PENDING.remove(playerId);
        if (removed == null) return;
        removed.release();
        if (player != null) {
            PortalNetworking.sendPortalPending(player, removed.destinationId, "Cancelled", null);
        }
    }

    private static boolean isCurrent(Pending pending) {
        Pending current = PENDING.get(pending.playerId);
        return current == pending && current.requestId == pending.requestId;
    }

    private static long serverTime(MinecraftServer server) {
        return server == null ? 0L : server.overworld().getGameTime();
    }

    private enum Stage {
        PREPARING,
        WAITING_CONFIRMATION
    }

    private static final class Pending {
        private final long requestId;
        private final UUID playerId;
        private final UUID destinationId;
        private final DestinationSafetyFingerprint fingerprint;
        private final ResourceKey<Level> sourceDimension;
        private final boolean fromGui;
        private final boolean confirmedUnsafe;
        private final PortalPlacementIntent placementIntent;
        private final net.minecraft.nbt.CompoundTag gunReference;
        private final ItemStack originalGun;
        private final ServerLevel targetLevel;
        private final BlockPos targetPosition;
        private final UUID ticketKey;
        private final long startedAt;
        private Stage stage = Stage.PREPARING;
        private boolean ticketHeld;
        private boolean loadingNotified;
        private long expiresAt;

        private Pending(long requestId, UUID playerId, UUID destinationId,
                        DestinationSafetyFingerprint fingerprint, ResourceKey<Level> sourceDimension,
                        boolean fromGui, boolean confirmedUnsafe, PortalPlacementIntent placementIntent,
                        net.minecraft.nbt.CompoundTag gunReference, ItemStack originalGun,
                        ServerLevel targetLevel, BlockPos targetPosition, UUID ticketKey, long startedAt) {
            this.requestId = requestId;
            this.playerId = playerId;
            this.destinationId = destinationId;
            this.fingerprint = fingerprint;
            this.sourceDimension = sourceDimension;
            this.fromGui = fromGui;
            this.confirmedUnsafe = confirmedUnsafe;
            this.placementIntent = placementIntent;
            this.gunReference = gunReference;
            this.originalGun = originalGun;
            this.targetLevel = targetLevel;
            this.targetPosition = targetPosition;
            this.ticketKey = ticketKey;
            this.startedAt = startedAt;
        }

        private void acquire() {
            if (ticketHeld) return;
            targetLevel.getChunkSource().addRegionTicket(
                PREPARE_TICKET, new ChunkPos(targetPosition), 3, ticketKey, true);
            ticketHeld = true;
        }

        private void release() {
            if (!ticketHeld) return;
            targetLevel.getChunkSource().removeRegionTicket(
                PREPARE_TICKET, new ChunkPos(targetPosition), 3, ticketKey, true);
            ticketHeld = false;
        }
    }

    private PortalOpenCoordinator() {}
}

package dev.riftgun.service;
import dev.riftgun.core.msg.Msg;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.portal.PortalChunkGuard;

import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.core.fuel.RiftFuelStores;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.fuel.PortalFuelManager;
import dev.riftgun.crisis.PortalCrisisConfigurationSnapshot;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalExitTarget;
import dev.riftgun.portal.PortalExclusions;
import dev.riftgun.portal.PortalRuntimeOptions;
import dev.riftgun.sound.PortalSoundSnapshot;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.module.PlayerExcludeMode;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Synchronous portal-open router. Unloaded cross-dimension exits are created after first transit. */
public final class PortalOpenCoordinator {
    public static void request(ServerPlayer player, PortalPlayerData data,
                               UUID destinationId, boolean fromGui,
                               PortalPlacementMode mode, PortalGunLocator.LocatedGun locatedGun) {
        Destination destination = data.destination(destinationId).orElse(null);
        if (destination == null) {
            failMessage(player, "message.riftgun.destination_missing");
            return;
        }
        if (open(player, data, destination, mode, locatedGun, null, null, true, fromGui)) {
            PortalDataStore.save(player, data);
            PortalNetworking.sendSnapshot(player, false, locatedGun);
            if (fromGui) PortalNetworking.sendPortalOpened(player);
        }
    }

    /** Opens a temporary destination without changing the player's saved or selected destinations. */
    public static boolean openTransient(ServerPlayer player, PortalPlayerData data,
                                        Destination destination, PortalPlacementMode mode,
                                        PortalGunLocator.LocatedGun locatedGun, boolean fromGui) {
        return open(player, data, destination, mode, locatedGun, null, null, false, fromGui);
    }

    /** Opens a portal whose exit lands next to the given online player. */
    public static void requestPlayerTarget(ServerPlayer player, PortalPlayerData data,
                                           UUID targetPlayerId, boolean fromGui,
                                           PortalPlacementMode mode, PortalGunLocator.LocatedGun locatedGun) {
//? if >=1.21.11 {
        /*MinecraftServer server = player.level().getServer();
*///?} else {
        MinecraftServer server = player.getServer();
//?}
        ServerPlayer target = server == null ? null : server.getPlayerList().getPlayer(targetPlayerId);
        if (target == null) {
            failMessage(player, "message.riftgun.player_target_offline");
            return;
        }
        PortalGunModuleSettings.ensure(locatedGun.stack(), data.settings().smartDistance());
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            locatedGun.stack(), data.settings().smartDistance());
        if (!capabilities.playerTarget()) {
            failMessage(player, "message.riftgun.player_target_module_required");
            return;
        }

        // Privacy gate. Self-targeting bypasses all privacy rules; transit protection also
        // never applies to the player's own exit doors.
        boolean selfTarget = target.getUUID().equals(player.getUUID());
        boolean consumeOneShotGrant = false;
        if (!selfTarget) {
            PortalPrivacyService.Access access = PortalPrivacyService.checkPortalAccess(server, target, player);
            switch (access.outcome()) {
                case DENIED, DENIED_ONCE, ALWAYS_DENIED -> {
                    PortalPrivacyService.notifyDenied(player, target, access);
                    return;
                }
                case REQUESTED -> {
                    PortalPrivacyService.promptRequest(server, target, player);
                    return;
                }
                case GRANTED_ONCE -> consumeOneShotGrant = true;
                case ALLOWED -> {
                }
            }
        }

        long time = player.level().getGameTime();
        Destination destination = new Destination(
//? if >=1.21.11 {
            /*UUID.randomUUID(), target.getGameProfile().name(), PortalPlayerData.DEFAULT_GROUP_ID,
*///?} else {
            UUID.randomUUID(), target.getGameProfile().getName(), PortalPlayerData.DEFAULT_GROUP_ID,
//?}
            target.level().dimension(), target.getX(), target.getY(), target.getZ(),
            target.getYRot(), time, 0L, false);
        PlayerExcludeMode excludeMode = capabilities.playerExcludeMode();
        UUID entryExclude = excludeMode == PlayerExcludeMode.ENTRY_AND_EXIT
            ? targetPlayerId : null;
        boolean transitProtects = !selfTarget
            && !PortalPrivacyService.allowsForeignExitTransit(target, player.getUUID());
        UUID exitExclude = transitProtects
            ? targetPlayerId
            : excludeMode != PlayerExcludeMode.OFF ? targetPlayerId : null;
        if (open(player, data, destination, mode, locatedGun, entryExclude, exitExclude, false, fromGui)) {
            if (consumeOneShotGrant) {
                PortalPrivacyService.consumeGrant(server, target.getUUID(), player.getUUID());
            }
            data.recordPlayerUse(targetPlayerId, time);
            PortalDataStore.save(player, data);
            PortalNetworking.sendSnapshot(player, false, locatedGun);
            if (fromGui) PortalNetworking.sendPortalOpened(player);
        }
    }

    private static boolean open(ServerPlayer player, PortalPlayerData data,
                                Destination destination, PortalPlacementMode mode,
                                PortalGunLocator.LocatedGun locatedGun, @Nullable UUID entryExclude,
                                @Nullable UUID exitExclude, boolean recordAsDestination, boolean fromGui) {
        var dimensionResult = RiftRuntime.current().dimensionPolicy().validate(player, destination);
        if (!dimensionResult.allowed()) {
            Msg.displayClientMessage(player, dimensionResult.message(), true);
            return false;
        }
//? if >=1.21.11 {
        /*MinecraftServer server = player.level().getServer();
*///?} else {
        MinecraftServer server = player.getServer();
//?}
        ServerLevel targetLevel = server == null ? null : server.getLevel(destination.dimension());
        if (targetLevel == null) {
            failMessage(player, "message.riftgun.dimension_unavailable");
            return false;
        }
        if (!PortalChunkGuard.inWorldBounds(targetLevel, BlockPos.containing(destination.position()))) {
            failMessage(player, "message.riftgun.coordinate_out_of_bounds");
            return false;
        }

        PortalGunModuleSettings.ensure(locatedGun.stack(), data.settings().smartDistance());
        PortalGunCapabilities gunCapabilities = PortalGunCapabilities.resolve(
            locatedGun.stack(), data.settings().smartDistance());
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            gunCapabilities.smartDistance(), gunCapabilities.configuredSurfaceRange(),
            data.settings().predictionMode(), gunCapabilities.aperture(),
            RiftConfigs.server().prediction().frontProjectionFactor(),
            RiftConfigs.server().prediction().downshotProjectionFactor());
        PortalPlacementCapture capture = RiftRuntime.current().placementResolver().capture(player, mode, constraints);
        if (!capture.successful()) {
            failMessage(player, capture.errorKey());
            return false;
        }
        PortalEntryPlacementResult entry = RiftRuntime.current().placementResolver().resolveEntry(
            player, capture.intent(), constraints);
        if (!entry.successful()) {
            failMessage(player, entry.errorKey());
            return false;
        }
        if (!PortalChunkGuard.inWorldBounds((ServerLevel) player.level(),
            BlockPos.containing(entry.placement().center()))) {
            com.mojang.logging.LogUtils.getLogger().warn(
                "Portal open rejected: entry placement out of bounds center={}",
                entry.placement().center());
            failMessage(player, "message.riftgun.coordinate_out_of_bounds");
            return false;
        }

        PortalFuelManager.Plan fuelPlan = PortalFuelManager.plan(
            player, locatedGun.stack(), destination.dimension());
        if (!fuelPlan.successful()) {
            failMessage(player, fuelPlan.errorKey());
            return false;
        }

        boolean crossDimension = !player.level().dimension().equals(destination.dimension());
        BlockPos targetPosition = BlockPos.containing(destination.position());
        boolean targetTicksEntities = targetLevel.isPositionEntityTicking(targetPosition);
        PortalRuntimeOptions runtimeOptions = new PortalRuntimeOptions(
            gunCapabilities.entityAccess(), gunCapabilities.openDurationTicks(),
            gunCapabilities.aperture(), gunCapabilities.transitCooldownTicks(),
            gunCapabilities.fallGuard(), gunCapabilities.entityFallGuard(),
            RiftConfigs.server().portal().horizontalTriggerExtend(),
            PortalSoundSnapshot.from(data.settings().portalSounds()),
            PortalCrisisConfigurationSnapshot.capture(
                RiftFuelStores.open(locatedGun.stack()).content().fluid()));
        PortalExclusions exclusions = new PortalExclusions(entryExclude, exitExclude);
        SafetyReport safetyReport = null;
        boolean opened;
        if (PortalOpenRoute.decide(crossDimension, targetTicksEntities) == PortalOpenRoute.DEFERRED_EXIT) {
            opened = PortalEntity.openDeferredExit(
                player, entry.placement(), fuelPlan.use().profile(), PortalExitTarget.from(destination),
                runtimeOptions, exclusions,
                () -> PortalFuelManager.consume(locatedGun.stack(), fuelPlan.use()));
        } else {
            Destination resolved = destination;
            if (!crossDimension && data.settings().safetyCheckEnabled()) {
                safetyReport = RiftRuntime.current().safetyInspector().inspect(targetLevel, destination);
                if (!safetyReport.safe()) {
                    Msg.displayClientMessage(player,
                        Component.translatable("message.riftgun.destination_unsafe"), true);
                }
                resolved = RiftRuntime.current().safeDestinationResolver().resolve(
                    targetLevel, destination, safetyReport);
            }

            PortalPlacementResult placement = RiftRuntime.current().placementResolver().resolveExitPrepared(
                targetLevel, PortalExitTarget.from(resolved), entry.placement(), gunCapabilities.aperture());
            if (!placement.successful()) {
                failMessage(player, placement.errorKey());
                return false;
            }
            if (!PortalChunkGuard.inWorldBounds(targetLevel,
                BlockPos.containing(placement.pair().exit().center()))) {
                com.mojang.logging.LogUtils.getLogger().warn(
                    "Portal open rejected: exit placement out of bounds center={}",
                    placement.pair().exit().center());
                failMessage(player, "message.riftgun.coordinate_out_of_bounds");
                return false;
            }
            opened = PortalEntity.openPair(player, placement.pair(), fuelPlan.use().profile(),
                runtimeOptions, exclusions,
                () -> PortalFuelManager.consume(locatedGun.stack(), fuelPlan.use()));
        }

        if (!opened) {
            failMessage(player, "message.riftgun.portal_open_failed");
            return false;
        }

        if (recordAsDestination) {
            if (crossDimension) data.clearSafetyResult(destination.id());
            else if (safetyReport != null) data.recordSafetyResult(destination.id(), safetyReport.safe());
            data.selectedDestinationId(destination.id());
            data.replaceDestination(destination.usedAt(player.level().getGameTime()));
        }
        return true;
    }

    private static void failMessage(ServerPlayer player, String translationKey) {
        Msg.displayClientMessage(player, Component.translatable(translationKey), true);
    }

    private PortalOpenCoordinator() {}
}

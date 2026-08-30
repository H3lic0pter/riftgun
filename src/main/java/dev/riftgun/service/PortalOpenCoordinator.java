package dev.riftgun.service;
import dev.riftgun.api.PortalOpenResult;
import dev.riftgun.api.PortalOpenStatus;
import dev.riftgun.api.PortalTransitAuthorization;
import dev.riftgun.api.RiftGunPortalOpenPolicies;
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
import dev.riftgun.network.PrecisionPlacementRequest;
import dev.riftgun.network.SurfaceFaceRequestValidator;
import dev.riftgun.network.SurfaceFaceRequest;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalExitTarget;
import dev.riftgun.portal.PortalExclusions;
import dev.riftgun.portal.PortalRuntimeOptions;
import dev.riftgun.pairing.PortalPairingPendingEndpoints;
import dev.riftgun.sound.PortalSoundSnapshot;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.module.PlayerExcludeMode;
import java.util.UUID;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Routes unloaded destinations through deferred exit creation when required by the game node. */
public final class PortalOpenCoordinator {
    public static void request(ServerPlayer player, PortalPlayerData data,
                               UUID destinationId, boolean fromGui,
                               PortalPlacementMode mode, PortalGunLocator.LocatedGun locatedGun) {
        Destination destination = data.destination(destinationId).orElse(null);
        if (destination == null) {
            failMessage(player, "message.riftgun.destination_missing");
            return;
        }
        PortalOpenResult result = open(
            player, data, destination, mode, locatedGun, null, null, true, fromGui,
            Optional.empty(), null);
        if (result.opened()) {
            PortalDataStore.save(player, data);
            PortalNetworking.sendSnapshot(player, false, locatedGun);
            if (fromGui) PortalNetworking.sendPortalOpened(player);
        } else {
            displayFailure(player, result);
        }
    }

    public static void requestSurfaceFace(ServerPlayer player, PortalPlayerData data,
                                          UUID destinationId, PortalPlacementMode mode,
                                          PortalGunLocator.LocatedGun locatedGun,
                                          SurfaceFaceRequest request) {
        Destination destination = data.destination(destinationId).orElse(null);
        if (destination == null) {
            failMessage(player, "message.riftgun.destination_missing");
            return;
        }
        PortalOpenResult result = open(player, data, destination, mode, locatedGun,
            null, null, true, false, Optional.empty(), PrecisionPlacementRequest.surface(request));
        if (result.opened()) {
            PortalDataStore.save(player, data);
            PortalNetworking.sendSnapshot(player, false, locatedGun);
        } else {
            displayFailure(player, result);
        }
    }

    public static void requestPrecision(ServerPlayer player, PortalPlayerData data,
                                        UUID destinationId, PortalPlacementMode mode,
                                        PortalGunLocator.LocatedGun locatedGun,
                                        PrecisionPlacementRequest request) {
        Destination destination = data.destination(destinationId).orElse(null);
        if (destination == null) {
            failMessage(player, "message.riftgun.destination_missing");
            return;
        }
        PortalOpenResult result = open(player, data, destination, mode, locatedGun,
            null, null, true, false, Optional.empty(), request);
        if (result.opened()) {
            PortalDataStore.save(player, data);
            PortalNetworking.sendSnapshot(player, false, locatedGun);
        } else displayFailure(player, result);
    }

    /** Opens a temporary destination without changing the player's saved or selected destinations. */
    public static boolean openTransient(ServerPlayer player, PortalPlayerData data,
                                        Destination destination, PortalPlacementMode mode,
                                        PortalGunLocator.LocatedGun locatedGun, boolean fromGui) {
        PortalOpenResult result = openTransientResult(
            player, data, destination, mode, locatedGun, fromGui);
        if (!result.opened()) displayFailure(player, result);
        return result.opened();
    }

    /** Internal structured variant used by the public Rift Gun integration adapter. */
    static PortalOpenResult openTransientResult(ServerPlayer player, PortalPlayerData data,
                                                 Destination destination, PortalPlacementMode mode,
                                                 PortalGunLocator.LocatedGun locatedGun, boolean fromGui) {
        return open(player, data, destination, mode, locatedGun, null, null, false, fromGui,
            Optional.empty(), null);
    }

    public static boolean openTransientSurfaceFace(
        ServerPlayer player, PortalPlayerData data, Destination destination,
        PortalPlacementMode mode, PortalGunLocator.LocatedGun locatedGun,
        SurfaceFaceRequest request
    ) {
        PortalOpenResult result = open(player, data, destination, mode, locatedGun,
            null, null, false, false, Optional.empty(), PrecisionPlacementRequest.surface(request));
        if (!result.opened()) displayFailure(player, result);
        return result.opened();
    }

    public static boolean openTransientPrecision(
        ServerPlayer player, PortalPlayerData data, Destination destination,
        PortalPlacementMode mode, PortalGunLocator.LocatedGun locatedGun,
        PrecisionPlacementRequest request
    ) {
        PortalOpenResult result = open(player, data, destination, mode, locatedGun,
            null, null, false, false, Optional.empty(), request);
        if (!result.opened()) displayFailure(player, result);
        return result.opened();
    }

    static PortalOpenResult openTransientResult(
        ServerPlayer player,
        PortalPlayerData data,
        Destination destination,
        PortalPlacementMode mode,
        PortalGunLocator.LocatedGun locatedGun,
        boolean fromGui,
        Optional<PortalTransitAuthorization> transitAuthorization
    ) {
        return open(player, data, destination, mode, locatedGun, null, null, false, fromGui,
            transitAuthorization, null);
    }

    /** Opens a portal whose exit lands next to the given online player. */
    public static void requestPlayerTarget(ServerPlayer player, PortalPlayerData data,
                                           UUID targetPlayerId, boolean fromGui,
                                           PortalPlacementMode mode, PortalGunLocator.LocatedGun locatedGun) {
        requestPlayerTarget(player, data, targetPlayerId, fromGui, mode, locatedGun, null);
    }

    public static void requestPlayerTargetSurfaceFace(
        ServerPlayer player, PortalPlayerData data, UUID targetPlayerId,
        PortalPlacementMode mode, PortalGunLocator.LocatedGun locatedGun,
        SurfaceFaceRequest request
    ) {
        requestPlayerTarget(player, data, targetPlayerId, false, mode, locatedGun,
            PrecisionPlacementRequest.surface(request));
    }

    public static void requestPlayerTargetPrecision(
        ServerPlayer player, PortalPlayerData data, UUID targetPlayerId,
        PortalPlacementMode mode, PortalGunLocator.LocatedGun locatedGun,
        PrecisionPlacementRequest request
    ) {
        requestPlayerTarget(player, data, targetPlayerId, false, mode, locatedGun, request);
    }

    private static void requestPlayerTarget(ServerPlayer player, PortalPlayerData data,
                                            UUID targetPlayerId, boolean fromGui,
                                            PortalPlacementMode mode,
                                            PortalGunLocator.LocatedGun locatedGun,
                                            @Nullable PrecisionPlacementRequest precisionRequest) {
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
        PortalOpenResult result = open(
            player, data, destination, mode, locatedGun, entryExclude, exitExclude, false, fromGui,
            Optional.empty(), precisionRequest);
        if (result.opened()) {
            if (consumeOneShotGrant) {
                PortalPrivacyService.consumeGrant(server, target.getUUID(), player.getUUID());
            }
            data.recordPlayerUse(targetPlayerId, time);
            PortalDataStore.save(player, data);
            PortalNetworking.sendSnapshot(player, false, locatedGun);
            if (fromGui) PortalNetworking.sendPortalOpened(player);
        } else {
            displayFailure(player, result);
        }
    }

    private static PortalOpenResult open(ServerPlayer player, PortalPlayerData data,
                                         Destination destination, PortalPlacementMode mode,
                                         PortalGunLocator.LocatedGun locatedGun, @Nullable UUID entryExclude,
                                         @Nullable UUID exitExclude, boolean recordAsDestination,
                                         boolean fromGui,
                                         Optional<PortalTransitAuthorization> transitAuthorization,
                                         @Nullable PrecisionPlacementRequest precisionRequest) {
        var sourcePolicy = RiftGunPortalOpenPolicies.evaluate(player);
        if (!sourcePolicy.allowed()) {
            return reject(PortalOpenStatus.SOURCE_POLICY_REJECTED, sourcePolicy.message());
        }
        var dimensionResult = RiftRuntime.current().dimensionPolicy().validate(player, destination);
        if (!dimensionResult.allowed()) {
            return reject(PortalOpenStatus.TARGET_DIMENSION_REJECTED, dimensionResult.message());
        }
//? if >=1.21.11 {
        /*MinecraftServer server = player.level().getServer();
*///?} else {
        MinecraftServer server = player.getServer();
//?}
        ServerLevel targetLevel = server == null ? null : server.getLevel(destination.dimension());
        if (targetLevel == null) {
            return reject(PortalOpenStatus.TARGET_DIMENSION_UNAVAILABLE,
                "message.riftgun.dimension_unavailable");
        }
        if (!PortalChunkGuard.inWorldBounds(targetLevel, BlockPos.containing(destination.position()))) {
            return reject(PortalOpenStatus.TARGET_OUT_OF_BOUNDS,
                "message.riftgun.coordinate_out_of_bounds");
        }

        PortalGunModuleSettings.ensure(locatedGun.stack(), data.settings().smartDistance());
        PortalGunCapabilities gunCapabilities = PortalGunCapabilities.resolve(
            locatedGun.stack(), data.settings().smartDistance());
        if (precisionRequest != null && !gunCapabilities.precisionPlacement()) {
            return reject(PortalOpenStatus.ENTRY_PLACEMENT_REJECTED,
                "message.riftgun.precision_placement_module_required");
        }
        mode = gunCapabilities.effectivePlacementMode(mode);
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            gunCapabilities.smartDistance(), gunCapabilities.maximumSurfaceRange(),
            gunCapabilities.remoteDistance(),
            data.settings().predictionMode(), gunCapabilities.aperture(),
            RiftConfigs.server().prediction().frontProjectionFactor(),
            RiftConfigs.server().prediction().downshotProjectionFactor(),
            gunCapabilities.activeSmartFallback());
        if (precisionRequest != null && precisionRequest.kind() == PrecisionPlacementRequest.Kind.FLOATING) {
            constraints = constraints.withFloatingOrientation(precisionRequest.orientation());
        }
        PortalPlacementCapture capture = precisionRequest == null
            || precisionRequest.kind() == PrecisionPlacementRequest.Kind.FLOATING
            ? RiftRuntime.current().placementResolver().capture(player, mode, constraints)
            : RiftRuntime.current().placementResolver().captureSurfaceFace(
                player, precisionRequest.surface(), constraints);
        if (!capture.successful()) {
            return reject(PortalOpenStatus.ENTRY_PLACEMENT_REJECTED, capture.errorKey());
        }
        if (precisionRequest != null && precisionRequest.kind() == PrecisionPlacementRequest.Kind.SURFACE) {
            SurfaceFaceRequestValidator.validate(mode, capture.intent());
        }
        PortalEntryPlacementResult entry = RiftRuntime.current().placementResolver().resolveEntry(
            player, capture.intent(), constraints);
        if (!entry.successful()) {
            return reject(PortalOpenStatus.ENTRY_PLACEMENT_REJECTED, entry.errorKey());
        }
        if (!PortalChunkGuard.inWorldBounds((ServerLevel) player.level(),
            BlockPos.containing(entry.placement().center()))) {
            com.mojang.logging.LogUtils.getLogger().warn(
                "Portal open rejected: entry placement out of bounds center={}",
                entry.placement().center());
            return reject(PortalOpenStatus.ENTRY_PLACEMENT_REJECTED,
                "message.riftgun.coordinate_out_of_bounds");
        }

        PortalFuelManager.Plan fuelPlan = PortalFuelManager.plan(
            player, locatedGun.stack(), destination.dimension());
        if (!fuelPlan.successful()) {
            return reject(PortalOpenStatus.INSUFFICIENT_FUEL, fuelPlan.errorKey());
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
                RiftFuelStores.open(locatedGun.stack()).content().fluid()),
            transitAuthorization);
        PortalExclusions exclusions = new PortalExclusions(entryExclude, exitExclude);
        SafetyReport safetyReport = null;
        boolean opened;
        if (PortalOpenRoute.decide(crossDimension, targetTicksEntities,
                PortalEntity.deferUnloadedSameDimensionExit()) == PortalOpenRoute.DEFERRED_EXIT) {
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
                return reject(PortalOpenStatus.EXIT_PLACEMENT_REJECTED, placement.errorKey());
            }
            if (!PortalChunkGuard.inWorldBounds(targetLevel,
                BlockPos.containing(placement.pair().exit().center()))) {
                com.mojang.logging.LogUtils.getLogger().warn(
                    "Portal open rejected: exit placement out of bounds center={}",
                    placement.pair().exit().center());
                return reject(PortalOpenStatus.EXIT_PLACEMENT_REJECTED,
                    "message.riftgun.coordinate_out_of_bounds");
            }
            opened = PortalEntity.openPair(player, placement.pair(), fuelPlan.use().profile(),
                runtimeOptions, exclusions,
                () -> PortalFuelManager.consume(locatedGun.stack(), fuelPlan.use()));
        }

        if (!opened) {
            return reject(PortalOpenStatus.PORTAL_OPEN_FAILED, "message.riftgun.portal_open_failed");
        }

        PortalPairingPendingEndpoints.clearAll(player);

        if (recordAsDestination) {
            if (crossDimension) data.clearSafetyResult(destination.id());
            else if (safetyReport != null) data.recordSafetyResult(destination.id(), safetyReport.safe());
            data.selectedDestinationId(destination.id());
            data.replaceDestination(destination.usedAt(player.level().getGameTime()));
        }
        return PortalOpenResult.success();
    }

    private static PortalOpenResult reject(PortalOpenStatus status, String translationKey) {
        return reject(status, Component.translatable(translationKey));
    }

    private static PortalOpenResult reject(PortalOpenStatus status, Component message) {
        return PortalOpenResult.rejected(status, message);
    }

    private static void displayFailure(ServerPlayer player, PortalOpenResult result) {
        Msg.displayClientMessage(player, result.message(), true);
    }

    private static void failMessage(ServerPlayer player, String translationKey) {
        Msg.displayClientMessage(player, Component.translatable(translationKey), true);
    }

    private PortalOpenCoordinator() {}
}

package dev.riftgun.service;

import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.fuel.PortalFuelManager;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalExitTarget;
import dev.riftgun.portal.PortalExclusions;
import dev.riftgun.portal.PortalRuntimeOptions;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.module.PlayerExcludeMode;
import dev.riftgun.config.ServerConfig;
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

    /** Opens a portal whose exit lands next to the given online player. */
    public static void requestPlayerTarget(ServerPlayer player, PortalPlayerData data,
                                           UUID targetPlayerId, boolean fromGui,
                                           PortalPlacementMode mode, PortalGunLocator.LocatedGun locatedGun) {
        MinecraftServer server = player.getServer();
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
        if (!selfTarget) {
            PortalPrivacyService.Access access = PortalPrivacyService.checkPortalAccess(server, target, player);
            if (access == PortalPrivacyService.Access.DENIED) {
                failMessage(player, "message.riftgun.player_privacy_denied");
                return;
            }
            if (access == PortalPrivacyService.Access.REQUESTED) {
                boolean fresh = PortalPrivacyService.promptRequest(server, target, player);
                failMessage(player, fresh
                    ? "message.riftgun.player_privacy_request_sent"
                    : "message.riftgun.player_privacy_request_pending");
                return;
            }
        }

        long time = player.level().getGameTime();
        Destination destination = new Destination(
            UUID.randomUUID(), target.getGameProfile().getName(), PortalPlayerData.DEFAULT_GROUP_ID,
            target.level().dimension(), target.getX(), target.getY(), target.getZ(),
            target.getYRot(), time, 0L, false);
        PlayerExcludeMode excludeMode = capabilities.playerExcludeMode();
        UUID entryExclude = excludeMode == PlayerExcludeMode.ENTRY_AND_EXIT
            ? targetPlayerId : null;
        boolean transitProtects = !selfTarget && PortalPrivacyService.transitProtectsTarget(target);
        UUID exitExclude = transitProtects
            ? targetPlayerId
            : excludeMode != PlayerExcludeMode.OFF ? targetPlayerId : null;
        if (open(player, data, destination, mode, locatedGun, entryExclude, exitExclude, false, fromGui)) {
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
        var dimensionResult = PortalServices.DIMENSION_POLICY.validate(player, destination);
        if (!dimensionResult.allowed()) {
            player.displayClientMessage(dimensionResult.message(), true);
            return false;
        }
        MinecraftServer server = player.getServer();
        ServerLevel targetLevel = server == null ? null : server.getLevel(destination.dimension());
        if (targetLevel == null) {
            failMessage(player, "message.riftgun.dimension_unavailable");
            return false;
        }

        PortalGunModuleSettings.ensure(locatedGun.stack(), data.settings().smartDistance());
        PortalGunCapabilities gunCapabilities = PortalGunCapabilities.resolve(
            locatedGun.stack(), data.settings().smartDistance());
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            gunCapabilities.smartDistance(), gunCapabilities.configuredSurfaceRange(),
            data.settings().predictionMode(), gunCapabilities.aperture(),
            ServerConfig.VALUES.frontProjectionFactor.get(),
            ServerConfig.VALUES.downshotProjectionFactor.get());
        PortalPlacementCapture capture = PortalServices.PLACEMENT_RESOLVER.capture(player, mode, constraints);
        if (!capture.successful()) {
            failMessage(player, capture.errorKey());
            return false;
        }
        PortalEntryPlacementResult entry = PortalServices.PLACEMENT_RESOLVER.resolveEntry(
            player, capture.intent(), constraints);
        if (!entry.successful()) {
            failMessage(player, entry.errorKey());
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
            gunCapabilities.fallGuard(), ServerConfig.VALUES.horizontalTriggerExtend.get());
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
                safetyReport = PortalServices.SAFETY_INSPECTOR.inspect(targetLevel, destination);
                if (!safetyReport.safe()) {
                    player.displayClientMessage(
                        Component.translatable("message.riftgun.destination_unsafe"), true);
                }
                resolved = PortalServices.SAFE_DESTINATION_RESOLVER.resolve(
                    targetLevel, destination, safetyReport);
            }

            PortalPlacementResult placement = PortalServices.PLACEMENT_RESOLVER.resolveExitPrepared(
                targetLevel, PortalExitTarget.from(resolved), entry.placement(), gunCapabilities.aperture());
            if (!placement.successful()) {
                failMessage(player, placement.errorKey());
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
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    private PortalOpenCoordinator() {}
}

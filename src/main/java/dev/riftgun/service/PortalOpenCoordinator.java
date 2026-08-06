package dev.riftgun.service;

import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.fuel.PortalFuelManager;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalExitTarget;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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

        PortalGunModuleSettings.ensure(locatedGun.stack(), data.settings().smartDistance());
        PortalGunCapabilities gunCapabilities = PortalGunCapabilities.resolve(
            locatedGun.stack(), data.settings().smartDistance());
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            gunCapabilities.smartDistance(), gunCapabilities.configuredSurfaceRange(),
            data.settings().motionPredictionEnabled(), gunCapabilities.aperture());
        PortalPlacementCapture capture = PortalServices.PLACEMENT_RESOLVER.capture(player, mode, constraints);
        if (!capture.successful()) {
            failMessage(player, capture.errorKey());
            return;
        }
        PortalEntryPlacementResult entry = PortalServices.PLACEMENT_RESOLVER.resolveEntry(
            player, capture.intent(), constraints);
        if (!entry.successful()) {
            failMessage(player, entry.errorKey());
            return;
        }

        PortalFuelManager.Plan fuelPlan = PortalFuelManager.plan(
            player, locatedGun.stack(), destination.dimension());
        if (!fuelPlan.successful()) {
            failMessage(player, fuelPlan.errorKey());
            return;
        }

        boolean crossDimension = !player.level().dimension().equals(destination.dimension());
        BlockPos targetPosition = BlockPos.containing(destination.position());
        boolean targetTicksEntities = targetLevel.isPositionEntityTicking(targetPosition);
        SafetyReport safetyReport = null;
        boolean opened;
        if (PortalOpenRoute.decide(crossDimension, targetTicksEntities) == PortalOpenRoute.DEFERRED_EXIT) {
            opened = PortalEntity.openDeferredExit(
                player, entry.placement(), fuelPlan.use().profile(), PortalExitTarget.from(destination),
                gunCapabilities.entityAccess(), gunCapabilities.openDurationTicks(), gunCapabilities.aperture(),
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
                return;
            }
            opened = PortalEntity.openPair(player, placement.pair(), fuelPlan.use().profile(),
                gunCapabilities.entityAccess(), gunCapabilities.openDurationTicks(), gunCapabilities.aperture(),
                () -> PortalFuelManager.consume(locatedGun.stack(), fuelPlan.use()));
        }

        if (!opened) {
            failMessage(player, "message.riftgun.portal_open_failed");
            return;
        }

        if (crossDimension) data.clearSafetyResult(destination.id());
        else if (safetyReport != null) data.recordSafetyResult(destination.id(), safetyReport.safe());
        data.selectedDestinationId(destination.id());
        data.replaceDestination(destination.usedAt(player.level().getGameTime()));
        PortalDataStore.save(player, data);
        PortalNetworking.sendSnapshot(player, false, locatedGun);
        if (fromGui) PortalNetworking.sendPortalOpened(player);
    }

    private static void failMessage(ServerPlayer player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    private PortalOpenCoordinator() {}
}

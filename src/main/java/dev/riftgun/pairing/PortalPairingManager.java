package dev.riftgun.pairing;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.api.RiftGunPortalOpenPolicies;
import dev.riftgun.core.fuel.RiftFuelStores;
import dev.riftgun.core.msg.Msg;
import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.crisis.PortalCrisisConfigurationSnapshot;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.fuel.PortalFuelManager;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalPairPlacement;
import dev.riftgun.portal.PortalRuntimeOptions;
import dev.riftgun.service.PortalEntryPlacementResult;
import dev.riftgun.service.PortalGunIdentity;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalPlacementCapture;
import dev.riftgun.service.PortalPlacementConstraints;
import dev.riftgun.service.PortalStoredPlacementValidator;
import dev.riftgun.sound.PortalSoundSnapshot;
import dev.riftgun.sound.PortalSounds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import dev.riftgun.service.PrecisionPlacementIntent;
import dev.riftgun.service.SurfaceFacePlacementPolicy;
import dev.riftgun.service.SurfaceFaceSelection;

/** Server-authoritative orchestration for directly placed A/B portal pairs. */
public final class PortalPairingManager {
    public static boolean place(ServerPlayer player, PortalPlayerData data,
                                PortalGunLocator.LocatedGun locatedGun,
                                PortalPlacementMode requestedMode,
                                PortalPairingEndpoint endpoint) {
        return place(player, data, locatedGun, requestedMode, endpoint, null,
            PortalPairingInvocation.MODE_BOUND);
    }

    public static boolean placeFromShortcut(ServerPlayer player, PortalPlayerData data,
                                            PortalGunLocator.LocatedGun locatedGun,
                                            PortalPlacementMode requestedMode,
                                            PortalPairingEndpoint endpoint) {
        return place(player, data, locatedGun, requestedMode, endpoint, null,
            PortalPairingInvocation.SHORTCUT);
    }

    public static boolean placeSurfaceFace(ServerPlayer player, PortalPlayerData data,
                                           PortalGunLocator.LocatedGun locatedGun,
                                           PortalPlacementMode requestedMode,
                                           PortalPairingEndpoint endpoint,
                                           SurfaceFaceSelection selection) {
        return place(player, data, locatedGun, requestedMode, endpoint,
            PrecisionPlacementIntent.surface(selection),
            PortalPairingInvocation.MODE_BOUND);
    }

    public static boolean placePrecision(ServerPlayer player, PortalPlayerData data,
                                         PortalGunLocator.LocatedGun locatedGun,
                                         PortalPlacementMode requestedMode,
                                         PortalPairingEndpoint endpoint,
                                         PrecisionPlacementIntent intent) {
        return place(player, data, locatedGun, requestedMode, endpoint, intent,
            PortalPairingInvocation.SHORTCUT);
    }

    private static boolean place(ServerPlayer player, PortalPlayerData data,
                                 PortalGunLocator.LocatedGun locatedGun,
                                 PortalPlacementMode requestedMode,
                                 PortalPairingEndpoint endpoint,
                                 PrecisionPlacementIntent precisionRequest,
                                 PortalPairingInvocation invocation) {
        if (!sourceAllowed(player)) return false;
        if (endpoint == PortalPairingEndpoint.NONE || endpoint == PortalPairingEndpoint.ENTITY_TARGET) {
            return fail(player, "message.riftgun.invalid_request");
        }
        PortalGunModuleSettings.ensure(locatedGun.stack(), data.settings().smartDistance());
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            locatedGun.stack(), data.settings().smartDistance());
        if (!capabilities.portalPairing()) {
            return fail(player, "message.riftgun.portal_pairing_module_required");
        }
        if (precisionRequest != null && !capabilities.precisionPlacement()) {
            return fail(player, "message.riftgun.precision_placement_module_required");
        }
        if (!invocation.allows(capabilities.functionMode())) {
            return fail(player, "message.riftgun.pairing_mode_required");
        }

        PortalPlacementMode mode = capabilities.effectivePlacementMode(requestedMode);
        if (mode == PortalPlacementMode.ENTITY_RELOCATION) {
            return fail(player, "message.riftgun.pairing_target_required");
        }
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            capabilities.smartDistance(), capabilities.maximumSurfaceRange(),
            capabilities.remoteDistance(),
            data.settings().predictionMode(), capabilities.aperture(),
            RiftConfigs.server().prediction().frontProjectionFactor(),
            RiftConfigs.server().prediction().downshotProjectionFactor(),
            capabilities.pairingSmartFallback());
        if (precisionRequest != null && precisionRequest.kind() == PrecisionPlacementIntent.Kind.FLOATING) {
            constraints = constraints.forPrecisionFloating(precisionRequest.orientation());
        }
        PortalPlacementCapture capture = precisionRequest == null
            || precisionRequest.kind() == PrecisionPlacementIntent.Kind.FLOATING
            ? RiftRuntime.current().placementResolver().capture(player, mode, constraints)
            : RiftRuntime.current().placementResolver().captureSurfaceFace(
                player, precisionRequest.surface(), constraints);
        if (!capture.successful()) return fail(player, capture.errorKey());
        if (precisionRequest != null && precisionRequest.kind() == PrecisionPlacementIntent.Kind.SURFACE
            && !SurfaceFacePlacementPolicy.accepts(mode, capture.intent())) {
            return fail(player, "message.riftgun.surface_mode_required");
        }
        PortalEntryPlacementResult placement = RiftRuntime.current().placementResolver()
            .resolveEntry(player, capture.intent(), constraints);
        if (!placement.successful()) return fail(player, placement.errorKey());
        if (precisionRequest != null && precisionRequest.previewPlacement() != null
            && mode == PortalPlacementMode.FRONT
            && validFrontPreview(player, placement.placement(),
                precisionRequest.previewPlacement())) {
            placement = PortalEntryPlacementResult.success(precisionRequest.previewPlacement());
        }

        MinecraftServer server = server(player);
        if (server == null) return fail(player, "message.riftgun.portal_open_failed");
        UUID gunId = PortalGunIdentity.ensure(locatedGun.stack());
        long now = server.overworld().getGameTime();
        var active = dev.riftgun.portal.PortalOwnerIndex.owned(server, player.getUUID()).stream()
            .filter(portal -> gunId.equals(portal.pairingGunId()))
            .filter(portal -> !portal.pairingDormant())
            .filter(PortalPairingManager::usable)
            .toList();
        PortalPairingPendingEndpoint pending = PortalPairingPendingEndpoints.getValid(
            locatedGun.stack(), player.getUUID(), gunId, now);
        if (pending != null && !pending.pairEndpoint()) pending = null;
        boolean hasA = pending != null && pending.endpoint() == PortalPairingEndpoint.A
            || active.stream().anyMatch(
            portal -> portal.pairingEndpoint() == PortalPairingEndpoint.A);
        boolean hasB = pending != null && pending.endpoint() == PortalPairingEndpoint.B
            || active.stream().anyMatch(
            portal -> portal.pairingEndpoint() == PortalPairingEndpoint.B);
        PortalPairingStateMachine.Decision decision = PortalPairingStateMachine.place(
            PortalPairingStateMachine.State.from(hasA, hasB), endpoint);
        PortalEntity opposite = active.stream()
            .filter(portal -> portal.pairingEndpoint() == endpoint.opposite())
            .findFirst().orElse(null);
        PortalPairingPendingEndpoint pendingOpposite = pending != null
            && pending.endpoint() == endpoint.opposite() ? pending : null;

        if (decision.connectsPair() && opposite == null && pendingOpposite == null) {
            return fail(player, "message.riftgun.portal_open_failed");
        }
        PortalRuntimeOptions options = runtimeOptions(data, capabilities, locatedGun);
        if (pendingOpposite != null && !validPendingPlacement(player, server, pendingOpposite)) {
            var recognized = PortalFuelManager.recognizedProfile(locatedGun.stack());
            if (!recognized.successful()) return fail(player, recognized.errorKey());
            savePending(player, data, server, locatedGun, placement.placement(), endpoint,
                now, options.openDurationTicks());
            Msg.displayClientMessage(player,
                Component.translatable("message.riftgun.pairing_pending_replaced",
                    PortalPairingLabels.forEndpoint(endpoint)), true);
            return true;
        }

        var fuelPlan = decision.connectsPair()
            ? PortalFuelManager.plan(player, locatedGun.stack(), opposite != null
                ? opposite.level().dimension() : pendingOpposite.dimension())
            : PortalFuelManager.recognizedProfile(locatedGun.stack());
        if (!fuelPlan.successful()) return fail(player, fuelPlan.errorKey());
        boolean opened;
        if (!decision.connectsPair()) {
            savePending(player, data, server, locatedGun, placement.placement(), endpoint,
                now, options.openDurationTicks());
            opened = true;
        } else {
            PortalPairPlacement pair = new PortalPairPlacement(
                opposite != null ? opposite.level().dimension() : pendingOpposite.dimension(),
                placement.placement(), opposite != null ? opposite.placement() : pendingOpposite.placement());
            opened = PortalEntity.openPairing(player, pair, fuelPlan.use().profile(), options,
                () -> PortalFuelManager.consume(locatedGun.stack(), fuelPlan.use()), gunId, endpoint);
            if (opened) {
                PortalPairingPendingEndpoints.clearAll(player);
            }
        }
        if (!opened) return fail(player, "message.riftgun.portal_open_failed");
        if (decision.connectsPair()) {
            Msg.displayClientMessage(player,
                Component.translatable("message.riftgun.pairing_connected"), true);
        }
        return true;
    }

    public static boolean setRelocationTarget(ServerPlayer player, PortalPlayerData data,
                                              PortalGunLocator.LocatedGun locatedGun) {
        return setRelocationTarget(player, data, locatedGun,
            PortalPairingInvocation.MODE_BOUND);
    }

    public static boolean setRelocationTargetFromShortcut(
        ServerPlayer player, PortalPlayerData data, PortalGunLocator.LocatedGun locatedGun
    ) {
        return setRelocationTarget(player, data, locatedGun,
            PortalPairingInvocation.SHORTCUT);
    }

    private static boolean setRelocationTarget(ServerPlayer player, PortalPlayerData data,
                                               PortalGunLocator.LocatedGun locatedGun,
                                               PortalPairingInvocation invocation) {
        if (!sourceAllowed(player)) return false;
        PortalGunModuleSettings.ensure(locatedGun.stack(), data.settings().smartDistance());
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            locatedGun.stack(), data.settings().smartDistance());
        if (!capabilities.portalPairing()) {
            return fail(player, "message.riftgun.portal_pairing_module_required");
        }
        if (!invocation.allows(capabilities.functionMode())) {
            return fail(player, "message.riftgun.pairing_mode_required");
        }
        if (!capabilities.entityRelocation()) {
            return fail(player, "message.riftgun.entity_relocation_module_required");
        }
        // Keep the full module range for finding a surface. If that ray misses, an installed
        // Remote module supplies its adjustable floating distance; legacy guns keep full range.
        int floatingDistance = capabilities.remote()
            ? capabilities.remoteDistance() : capabilities.maximumSurfaceRange();
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            capabilities.maximumSurfaceRange(), capabilities.maximumSurfaceRange(),
            floatingDistance,
            PortalPredictionMode.OFF, capabilities.aperture(),
            RiftConfigs.server().prediction().frontProjectionFactor(),
            RiftConfigs.server().prediction().downshotProjectionFactor(),
            PortalFloatingFallback.REMOTE);
        PortalPlacementCapture capture = RiftRuntime.current().placementResolver()
            .capture(player, PortalPlacementMode.SMART, constraints);
        if (!capture.successful()) return fail(player, capture.errorKey());
        PortalEntryPlacementResult placement = RiftRuntime.current().placementResolver()
            .resolveEntry(player, capture.intent(), constraints);
        if (!placement.successful()) return fail(player, placement.errorKey());
        var fuelPlan = PortalFuelManager.recognizedProfile(locatedGun.stack());
        if (!fuelPlan.successful()) return fail(player, fuelPlan.errorKey());
        MinecraftServer server = server(player);
        if (server == null) return fail(player, "message.riftgun.portal_open_failed");
        UUID gunId = PortalGunIdentity.ensure(locatedGun.stack());
        PortalRuntimeOptions options = runtimeOptions(data, capabilities, locatedGun);
        long now = server.overworld().getGameTime();
        savePending(player, data, server, locatedGun, placement.placement(),
            PortalPairingEndpoint.ENTITY_TARGET, now, options.openDurationTicks());
        PortalSounds.playShot(player, options.sounds());
        PortalSounds.playOpening((ServerLevel) player.level(), placement.placement().center(), options.sounds());
        Msg.displayClientMessage(player,
            Component.translatable("message.riftgun.pairing_relocation_target_set"), true);
        return true;
    }

    private static PortalRuntimeOptions runtimeOptions(PortalPlayerData data,
                                                        PortalGunCapabilities capabilities,
                                                        PortalGunLocator.LocatedGun gun) {
        return new PortalRuntimeOptions(
            capabilities.entityAccess(), capabilities.openDurationTicks(), capabilities.aperture(),
            capabilities.transitCooldownTicks(), capabilities.fallGuard(),
            capabilities.entityFallGuard(), RiftConfigs.server().portal().horizontalTriggerExtend(),
            PortalSoundSnapshot.from(data.settings().portalSounds()),
            PortalCrisisConfigurationSnapshot.capture(RiftFuelStores.open(gun.stack()).content().fluid()),
            Optional.empty());
    }

    private static boolean validFrontPreview(ServerPlayer player,
                                             dev.riftgun.portal.PortalPlacement expected,
                                             dev.riftgun.portal.PortalPlacement preview) {
        return expected != null && preview != null && !preview.anchored()
            && preview.orientation() == expected.orientation()
            && preview.geometry() == expected.geometry()
            && preview.center().distanceToSqr(expected.center()) <= 1.0
            && Math.abs(Mth.wrapDegrees(preview.yaw() - expected.yaw())) <= 15.0F
            && PortalStoredPlacementValidator.valid(
                player, (ServerLevel) player.level(), preview);
    }

    private static boolean fail(ServerPlayer player, String key) {
        Msg.displayClientMessage(player, Component.translatable(key), true);
        return false;
    }

    private static boolean sourceAllowed(ServerPlayer player) {
        var decision = RiftGunPortalOpenPolicies.evaluate(player);
        if (decision.allowed()) return true;
        Msg.displayClientMessage(player, decision.message(), true);
        return false;
    }

    private static boolean usable(PortalEntity portal) {
        return portal.phase() != dev.riftgun.portal.PortalLifecycle.Phase.CLOSING
            && portal.phase() != dev.riftgun.portal.PortalLifecycle.Phase.CLOSED;
    }

    private static boolean validPendingPlacement(ServerPlayer player, MinecraftServer server,
                                                 PortalPairingPendingEndpoint pending) {
        ServerLevel level = server.getLevel(pending.dimension());
        return level != null
            && PortalStoredPlacementValidator.valid(player, level, pending.placement());
    }

    private static void savePending(ServerPlayer player, PortalPlayerData data,
                                    MinecraftServer server,
                                    PortalGunLocator.LocatedGun gun,
                                    dev.riftgun.portal.PortalPlacement placement,
                                    PortalPairingEndpoint endpoint, long startedAt,
                                    int durationTicks) {
        PortalPairingPendingEndpoints.clearAll(player);
        UUID gunId = PortalGunIdentity.ensure(gun.stack());
        PortalPairingPendingEndpoints.save(
            gun.stack(), player.getUUID(), gunId, player.level().dimension(), placement,
            endpoint, startedAt, durationTicks);
        dev.riftgun.portal.PortalOwnerIndex.closeOwned(
            server, player.getUUID(), java.util.Set.of());
    }

    private static MinecraftServer server(ServerPlayer player) {
//? if >=1.21.11 {
        /*return player.level().getServer();
*///?} else {
        return player.getServer();
//?}
    }

    private PortalPairingManager() {}
}

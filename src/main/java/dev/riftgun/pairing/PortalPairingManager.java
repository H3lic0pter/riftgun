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
import dev.riftgun.sound.PortalSoundSnapshot;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative orchestration for directly placed A/B portal pairs. */
public final class PortalPairingManager {
    public static boolean place(ServerPlayer player, PortalPlayerData data,
                                PortalGunLocator.LocatedGun locatedGun,
                                PortalPlacementMode requestedMode,
                                PortalPairingEndpoint endpoint) {
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
        if (capabilities.functionMode() != PortalFunctionMode.PORTAL_PAIRING) {
            return fail(player, "message.riftgun.pairing_mode_required");
        }

        PortalPlacementMode mode = capabilities.effectivePlacementMode(requestedMode);
        if (mode == PortalPlacementMode.ENTITY_RELOCATION) {
            return fail(player, "message.riftgun.pairing_target_required");
        }
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            capabilities.smartDistance(), capabilities.configuredSurfaceRange(),
            data.settings().predictionMode(), capabilities.aperture(),
            RiftConfigs.server().prediction().frontProjectionFactor(),
            RiftConfigs.server().prediction().downshotProjectionFactor(),
            capabilities.pairingSmartFallback());
        PortalPlacementCapture capture = RiftRuntime.current().placementResolver()
            .capture(player, mode, constraints);
        if (!capture.successful()) return fail(player, capture.errorKey());
        PortalEntryPlacementResult placement = RiftRuntime.current().placementResolver()
            .resolveEntry(player, capture.intent(), constraints);
        if (!placement.successful()) return fail(player, placement.errorKey());

        MinecraftServer server = server(player);
        if (server == null) return fail(player, "message.riftgun.portal_open_failed");
        UUID gunId = PortalGunIdentity.ensure(locatedGun.stack());
        var active = dev.riftgun.portal.PortalOwnerIndex.owned(server, player.getUUID()).stream()
            .filter(portal -> gunId.equals(portal.pairingGunId()))
            .filter(PortalPairingManager::usable)
            .toList();
        boolean hasA = active.stream().anyMatch(
            portal -> portal.pairingEndpoint() == PortalPairingEndpoint.A);
        boolean hasB = active.stream().anyMatch(
            portal -> portal.pairingEndpoint() == PortalPairingEndpoint.B);
        PortalPairingStateMachine.State state = hasA && hasB
            ? PortalPairingStateMachine.State.CONNECTED
            : hasA ? PortalPairingStateMachine.State.A_ONLY
            : hasB ? PortalPairingStateMachine.State.B_ONLY
            : PortalPairingStateMachine.State.EMPTY;
        PortalPairingStateMachine.Decision decision = PortalPairingStateMachine.place(state, endpoint);
        PortalEntity opposite = active.stream()
            .filter(portal -> portal.pairingEndpoint() == endpoint.opposite())
            .findFirst().orElse(null);

        var fuelPlan = decision.consumesPairFuel()
            ? PortalFuelManager.plan(player, locatedGun.stack(), opposite.level().dimension())
            : PortalFuelManager.recognizedProfile(locatedGun.stack());
        if (!fuelPlan.successful()) return fail(player, fuelPlan.errorKey());
        PortalRuntimeOptions options = runtimeOptions(data, capabilities, locatedGun);
        boolean opened;
        if (!decision.consumesPairFuel()) {
            opened = PortalEntity.openDormant(player, placement.placement(),
                fuelPlan.use().profile(), options, gunId, endpoint);
        } else {
            if (opposite == null) return fail(player, "message.riftgun.portal_open_failed");
            PortalPairPlacement pair = new PortalPairPlacement(opposite.level().dimension(),
                placement.placement(), opposite.placement());
            opened = PortalEntity.openPairing(player, pair, fuelPlan.use().profile(), options,
                () -> PortalFuelManager.consume(locatedGun.stack(), fuelPlan.use()), gunId, endpoint);
        }
        if (!opened) return fail(player, "message.riftgun.portal_open_failed");
        if (opposite != null) {
            Msg.displayClientMessage(player,
                Component.translatable("message.riftgun.pairing_connected"), true);
        }
        return true;
    }

    public static boolean setRelocationTarget(ServerPlayer player, PortalPlayerData data,
                                              PortalGunLocator.LocatedGun locatedGun) {
        if (!sourceAllowed(player)) return false;
        PortalGunModuleSettings.ensure(locatedGun.stack(), data.settings().smartDistance());
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            locatedGun.stack(), data.settings().smartDistance());
        if (!capabilities.portalPairing()) {
            return fail(player, "message.riftgun.portal_pairing_module_required");
        }
        if (capabilities.functionMode() != PortalFunctionMode.PORTAL_PAIRING) {
            return fail(player, "message.riftgun.pairing_mode_required");
        }
        if (!capabilities.entityRelocation()) {
            return fail(player, "message.riftgun.entity_relocation_module_required");
        }
        // A full-range SMART capture means a valid face is preferred; a ray miss routes to
        // Fixed REMOTE target. Prediction is deliberately disabled for a fixed target.
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            capabilities.configuredSurfaceRange(), capabilities.configuredSurfaceRange(),
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
        UUID gunId = PortalGunIdentity.ensure(locatedGun.stack());
        boolean opened = PortalEntity.openDormant(player, placement.placement(),
            fuelPlan.use().profile(), runtimeOptions(data, capabilities, locatedGun), gunId,
            PortalPairingEndpoint.ENTITY_TARGET);
        if (!opened) return fail(player, "message.riftgun.portal_open_failed");
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

    private static MinecraftServer server(ServerPlayer player) {
//? if >=1.21.11 {
        /*return player.level().getServer();
*///?} else {
        return player.getServer();
//?}
    }

    private PortalPairingManager() {}
}

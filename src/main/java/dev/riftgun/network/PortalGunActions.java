package dev.riftgun.network;
import dev.riftgun.core.msg.Msg;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.data.DestinationSort;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlayerSettings;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.fuel.PortalGunTank;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.relocation.EntityRelocationRouting;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.sound.PortalSoundSettings;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.navigation.DimensionalTraversalTargets;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Player preferences and settings stored directly on a Portal Gun. */
final class PortalGunActions {
    static boolean cyclePlacementMode(ServerPlayer player, PortalPlayerData data, ItemStack gun,
                                      boolean reverse) {
        PortalPlayerSettings old = data.settings();
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun, old.smartDistance());
        PortalPlacementMode next = adjacentAvailableMode(
            old.placementMode(), reverse, capabilities.entityRelocation(), capabilities.remote());
        data.settings(new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
            old.confirmDiscardedChanges(), old.confirmClearFluid(), old.animationsEnabled(),
            old.soundsEnabled(), old.sort(), next, old.smartDistance(), old.predictionMode(),
            old.portalSounds()));
        Msg.displayClientMessage(player, Component.translatable(
            "message.riftgun.placement_mode", Component.translatable("screen.riftgun.placement_mode."
                + next.name().toLowerCase(Locale.ROOT))), true);
        return true;
    }

    static PortalPlacementMode adjacentAvailableMode(PortalPlacementMode current, boolean reverse,
                                                       boolean relocationAvailable) {
        return adjacentAvailableMode(current, reverse, relocationAvailable, true);
    }

    static PortalPlacementMode adjacentAvailableMode(PortalPlacementMode current, boolean reverse,
                                                       boolean relocationAvailable,
                                                       boolean remoteAvailable) {
        PortalPlacementMode candidate = reverse ? current.previous() : current.next();
        while ((candidate == PortalPlacementMode.ENTITY_RELOCATION && !relocationAvailable)
            || (candidate == PortalPlacementMode.REMOTE && !remoteAvailable)) {
            candidate = reverse ? candidate.previous() : candidate.next();
        }
        return candidate;
    }

    static boolean setRadialMode(ServerPlayer player, PortalPlayerData data, ItemStack gun,
                                 CompoundTag request) {
        PortalPlayerSettings old = data.settings();
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun, old.smartDistance());
        PortalGunModuleSettings gunSettings = PortalGunModuleSettings.ensure(gun, old.smartDistance());
        PortalFunctionMode requestedFunction = gunSettings.portalPairing().functionMode();
        if (request.contains("FunctionMode")) {
            try {
                requestedFunction = PortalFunctionMode.valueOf(Nbt.getString(request, "FunctionMode"));
            } catch (IllegalArgumentException exception) {
                throw PortalRequestFields.error("message.riftgun.invalid_request");
            }
            if (requestedFunction == PortalFunctionMode.PORTAL_PAIRING && !capabilities.portalPairing()) {
                throw PortalRequestFields.error("message.riftgun.portal_pairing_module_required");
            }
        }
        String page = Nbt.getString(request, "Page");
        String value = Nbt.getString(request, "Mode");
        PortalPlayerSettings nextPlayer = old;
        if (page.equals("PLACEMENT")) {
            PortalPlacementMode mode;
            try {
                mode = PortalPlacementMode.valueOf(value);
            } catch (IllegalArgumentException exception) {
                throw PortalRequestFields.error("message.riftgun.invalid_request");
            }
            if (mode == PortalPlacementMode.ENTITY_RELOCATION
                && !capabilities.entityRelocation()) {
                throw PortalRequestFields.error("message.riftgun.entity_relocation_module_required");
            }
            if (mode == PortalPlacementMode.REMOTE && !capabilities.remote()) {
                throw PortalRequestFields.error("message.riftgun.remote_module_required");
            }
            nextPlayer = old.withPlacementMode(mode);
        } else if (page.equals("PREDICTION")) {
            PortalPredictionMode mode;
            try {
                mode = PortalPredictionMode.valueOf(value);
            } catch (IllegalArgumentException exception) {
                throw PortalRequestFields.error("message.riftgun.invalid_request");
            }
            nextPlayer = old.withPredictionMode(mode);
        } else if (!page.isEmpty()) {
            throw PortalRequestFields.error("message.riftgun.invalid_request");
        }
        boolean playerChanged = !nextPlayer.equals(old);
        boolean functionChanged = requestedFunction != gunSettings.portalPairing().functionMode();
        if (playerChanged) data.settings(nextPlayer);
        if (functionChanged) {
            gunSettings.withPortalPairing(
                gunSettings.portalPairing().withFunctionMode(requestedFunction)).save(gun);
        }
        if (page.equals("PREDICTION") && playerChanged) {
            RiftRuntime.current().motionHistory().setPredictionEnabled(player,
                nextPlayer.predictionMode() != PortalPredictionMode.OFF);
        }
        return playerChanged || functionChanged;
    }

    static boolean toggleFunctionMode(ServerPlayer player, PortalPlayerData data, ItemStack gun) {
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun, data.settings().smartDistance());
        if (!capabilities.portalPairing()) {
            throw PortalRequestFields.error("message.riftgun.portal_pairing_module_required");
        }
        PortalGunModuleSettings settings = PortalGunModuleSettings.ensure(gun, data.settings().smartDistance());
        PortalFunctionMode next = settings.portalPairing().functionMode().toggle();
        settings.withPortalPairing(settings.portalPairing().withFunctionMode(next)).save(gun);
        Msg.displayClientMessage(player, Component.translatable("message.riftgun.pairing_mode",
            Component.translatable(next == PortalFunctionMode.PORTAL_PAIRING
                ? "screen.riftgun.on" : "screen.riftgun.off")), true);
        return true;
    }

    static boolean updatePlayerSettings(ServerPlayer player, PortalPlayerData data,
                                        CompoundTag request, ItemStack gun) {
        DestinationSort sort;
        try {
            sort = DestinationSort.valueOf(Nbt.getString(request, "Sort"));
        } catch (IllegalArgumentException ignored) {
            sort = DestinationSort.RECENT;
        }
        PortalPlacementMode placementMode = PortalPlacementMode.parse(
            Nbt.getString(request, "PlacementMode"));
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun, data.settings().smartDistance());
        if (placementMode == PortalPlacementMode.REMOTE && !capabilities.remote()) {
            throw PortalRequestFields.error("message.riftgun.remote_module_required");
        }
        if (placementMode == PortalPlacementMode.ENTITY_RELOCATION
            && !capabilities.entityRelocation()) {
            throw PortalRequestFields.error("message.riftgun.entity_relocation_module_required");
        }
        PortalPlayerSettings settings = new PortalPlayerSettings(
            Nbt.getBoolean(request, "SafetyCheck"),
            Nbt.getBoolean(request, "ConfirmDeletion"),
            Nbt.getBoolean(request, "ConfirmDiscardedChanges"),
            Nbt.getBoolean(request, "ConfirmClearFluid"),
            Nbt.getBoolean(request, "Animations"),
            Nbt.getBoolean(request, "Sounds"),
            sort,
            placementMode,
            data.settings().smartDistance(),
            PortalPredictionMode.parse(Nbt.getString(request, "MotionPrediction"), PortalPredictionMode.OFF),
            Nbt.contains(request, "PortalSounds", Tag.TAG_COMPOUND)
                ? PortalSoundSettings.load(Nbt.getCompound(request, "PortalSounds"))
                : data.settings().portalSounds()
        );
        data.settings(settings);
        RiftRuntime.current().motionHistory().setPredictionEnabled(player,
            settings.predictionMode() != PortalPredictionMode.OFF);
        return true;
    }

    static boolean updateModuleSettings(ServerPlayer player, PortalPlayerData data,
                                        CompoundTag request, ItemStack gun) {
        PortalGunModuleSettings settings = PortalGunModuleSettings.ensure(gun, data.settings().smartDistance());
        PortalModuleRules rules = PortalModuleRules.current();
        String setting = Nbt.getString(request, "Setting");
        switch (setting) {
            case "SmartDistance" -> {
                int maximum = PortalGunCapabilities.resolve(
                    gun, data.settings().smartDistance()).maximumSurfaceRange();
                settings = settings.withSmartDistance(Math.clamp(Nbt.getInt(request, "Value"), 1, maximum));
            }
            case "RemoteDistance", "SurfaceRange" -> {
                int maximum = rules.maximumSurfaceRangeFor(
                    PortalGunModules.activeCount(gun, PortalModuleKind.SURFACE_RANGE, rules));
                settings = settings.withDesiredRemoteDistance(
                    Math.clamp(Nbt.getInt(request, "Value"), 1, maximum));
            }
            case "PortalDuration" -> settings = settings.withPortalDurationSeconds(
                PortalGunCapabilities.configuredDurationSeconds(gun, Nbt.getInt(request, "Value")));
            case "TransitCooldown" -> settings = settings.withTransitCooldownTenths(Nbt.getInt(request, "Value"));
            case "ExpandedAperture" -> {
                requireModule(gun, PortalModuleKind.APERTURE_EXPANSION, rules,
                    "message.riftgun.aperture_module_required");
                settings = settings.withExpandedApertureEnabled(Nbt.getBoolean(request, "Enabled"));
            }
            case "PassiveTransit", "HostileTransit", "BossTransit", "ProjectileTransit" -> {
                PortalModuleKind kind = switch (setting) {
                    case "PassiveTransit" -> PortalModuleKind.PASSIVE_TRANSIT;
                    case "HostileTransit" -> PortalModuleKind.HOSTILE_TRANSIT;
                    case "BossTransit" -> PortalModuleKind.BOSS_TRANSIT;
                    default -> PortalModuleKind.PROJECTILE_TRANSIT;
                };
                requireModule(gun, kind, rules, "message.riftgun.entity_module_required");
                settings = settings.withTransit(kind, Nbt.getBoolean(request, "Enabled"));
            }
            case "PlayerTarget" -> {
                requireModule(gun, PortalModuleKind.PLAYER_TARGET, rules,
                    "message.riftgun.player_target_module_required");
                settings = settings.withPlayerTargetEnabled(Nbt.getBoolean(request, "Enabled"));
            }
            case "PlayerExclude" -> {
                requireModule(gun, PortalModuleKind.PLAYER_TARGET, rules,
                    "message.riftgun.player_target_module_required");
                settings = settings.withPlayerExcludeMode(
                    settings.playerExcludeMode().step(Nbt.getInt(request, "Step")));
            }
            case "FallGuard" -> {
                requireModule(gun, PortalModuleKind.FALL_GUARD, rules,
                    "message.riftgun.fall_guard_module_required");
                settings = settings.withFallGuardEnabled(Nbt.getBoolean(request, "Enabled"));
            }
            case "FallGuardEntities" -> {
                requireModule(gun, PortalModuleKind.FALL_GUARD, rules,
                    "message.riftgun.fall_guard_module_required");
                settings = settings.withFallGuardEntitiesEnabled(Nbt.getBoolean(request, "Enabled"));
            }
            case "EntityRelocation" -> {
                requireModule(gun, PortalModuleKind.ENTITY_RELOCATION, rules,
                    "message.riftgun.entity_relocation_module_required");
                boolean enabled = Nbt.getBoolean(request, "Enabled");
                settings = settings.withEntityRelocationEnabled(enabled);
                PortalPlacementMode normalized = EntityRelocationRouting.normalizePlacementMode(
                    data.settings().placementMode(), enabled);
                if (normalized != data.settings().placementMode()) {
                    data.settings(data.settings().withPlacementMode(normalized));
                }
            }
            case "EntityRelocationSmartRouting" -> {
                requireModule(gun, PortalModuleKind.ENTITY_RELOCATION, rules,
                    "message.riftgun.entity_relocation_module_required");
                settings = settings.withEntityRelocationSmartRouting(Nbt.getBoolean(request, "Enabled"));
            }
            case "CoordinateSmartFallback", "PairingSmartFallback" -> {
                PortalModuleKind required = setting.equals("CoordinateSmartFallback")
                    ? PortalModuleKind.REMOTE : PortalModuleKind.PORTAL_PAIRING;
                requireModule(gun, required, rules, setting.equals("CoordinateSmartFallback")
                    ? "message.riftgun.remote_module_required"
                    : "message.riftgun.portal_pairing_module_required");
                PortalFloatingFallback fallback;
                try {
                    fallback = PortalFloatingFallback.valueOf(Nbt.getString(request, "Value"));
                } catch (IllegalArgumentException exception) {
                    throw PortalRequestFields.error("message.riftgun.invalid_request");
                }
                if (fallback == PortalFloatingFallback.REMOTE) {
                    requireModule(gun, PortalModuleKind.REMOTE, rules,
                        "message.riftgun.remote_module_required");
                }
                var pairing = settings.portalPairing();
                settings = setting.equals("CoordinateSmartFallback")
                    ? settings.withRemote(settings.remote().withCoordinateSmartFallback(fallback))
                    : settings.withPortalPairing(pairing.withSmartFallback(fallback));
            }
            case "RemoteScrollAdjustment" -> {
                requireModule(gun, PortalModuleKind.REMOTE, rules,
                    "message.riftgun.remote_module_required");
                settings = settings.withRemote(settings.remote().withScrollAdjustmentEnabled(
                    Nbt.getBoolean(request, "Enabled")));
            }
            case "RemoteRadialSlider" -> {
                requireModule(gun, PortalModuleKind.REMOTE, rules,
                    "message.riftgun.remote_module_required");
                settings = settings.withRemote(settings.remote().withRadialSliderEnabled(
                    Nbt.getBoolean(request, "Enabled")));
            }
            case "RemotePlacementPreview" -> {
                requireModule(gun, PortalModuleKind.REMOTE, rules,
                    "message.riftgun.remote_module_required");
                settings = settings.withRemote(settings.remote().withPlacementPreviewEnabled(
                    Nbt.getBoolean(request, "Enabled")));
            }
            case "DimensionalTraversalDimension" -> {
                requireDimensionalTraversal(gun, rules);
                String dimension = Nbt.getString(request, "Value");
                if (DimensionalTraversalTargets.resolve(player, dimension).isEmpty()) {
                    throw PortalRequestFields.error("message.riftgun.dimension_unavailable");
                }
                settings = settings.withDimensionalTraversal(
                    settings.dimensionalTraversal().withTargetDimension(dimension));
            }
            case "DimensionalTraversalMode" -> {
                requireDimensionalTraversal(gun, rules);
                DimensionalTraversalMode mode;
                try {
                    mode = DimensionalTraversalMode.valueOf(Nbt.getString(request, "Value"));
                } catch (IllegalArgumentException exception) {
                    throw PortalRequestFields.error("message.riftgun.invalid_request");
                }
                settings = settings.withDimensionalTraversal(
                    settings.dimensionalTraversal().withMode(mode));
            }
            default -> throw PortalRequestFields.error("message.riftgun.invalid_request");
        }
        settings.save(gun);
        return true;
    }

    private static void requireDimensionalTraversal(ItemStack gun, PortalModuleRules rules) {
        if (!RiftConfigs.server().dimensionalTraversal().enabled()) {
            throw PortalRequestFields.error("message.riftgun.dimensional_traversal_disabled");
        }
        requireModule(gun, PortalModuleKind.DIMENSIONAL_TRAVERSAL, rules,
            "message.riftgun.dimensional_traversal_module_required");
    }

    static boolean adjustRemoteDistance(ServerPlayer player, PortalPlayerData data,
                                        ItemStack gun, int requestedStep) {
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun, data.settings().smartDistance());
        if (!capabilities.remote()) {
            throw PortalRequestFields.error("message.riftgun.remote_module_required");
        }
        if (!capabilities.remoteScrollAdjustment()
            || !PortalGunCapabilities.usesRemoteDistanceControls(
                data.settings().placementMode(), capabilities.functionMode())) return false;
        PortalGunModuleSettings settings = PortalGunModuleSettings.ensure(
            gun, data.settings().smartDistance());
        int current = Math.clamp(settings.desiredRemoteDistance(), 1,
            capabilities.maximumSurfaceRange());
        int step = Integer.signum(requestedStep);
        int next = Math.clamp(current + step, 1, capabilities.maximumSurfaceRange());
        Msg.displayClientMessage(player, Component.translatable(
            "message.riftgun.remote_distance_adjusted", next, capabilities.maximumSurfaceRange()), true);
        if (step == 0 || next == current) return false;
        settings.withDesiredRemoteDistance(next).save(gun);
        return true;
    }

    static boolean toggleBucketMode(ServerPlayer player, ItemStack gun) {
        boolean enabled = !PortalGunMode.bucketMode(gun);
        PortalGunMode.bucketMode(gun, enabled);
        Msg.displayClientMessage(player, Component.translatable(enabled
            ? "message.riftgun.bucket_mode_enabled" : "message.riftgun.bucket_mode_disabled"), true);
        return true;
    }

    static boolean clearFluid(ServerPlayer player, ItemStack gun) {
        PortalGunTank tank = new PortalGunTank(gun);
        int amount = tank.getFluid().getAmount();
        if (amount <= 0) return false;
        tank.clear();
        Msg.displayClientMessage(player, Component.translatable("message.riftgun.fluid_cleared", amount), true);
        return true;
    }

    private static void requireModule(ItemStack gun, PortalModuleKind kind,
                                      PortalModuleRules rules, String errorKey) {
        if (PortalGunModules.activeCount(gun, kind, rules) <= 0) {
            throw PortalRequestFields.error(errorKey);
        }
    }

    private PortalGunActions() {}
}

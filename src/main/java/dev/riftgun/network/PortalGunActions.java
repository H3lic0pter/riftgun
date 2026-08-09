package dev.riftgun.network;

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
import dev.riftgun.service.PortalServices;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Player preferences and settings stored directly on a Portal Gun. */
final class PortalGunActions {
    static boolean cyclePlacementMode(ServerPlayer player, PortalPlayerData data) {
        PortalPlayerSettings old = data.settings();
        PortalPlacementMode next = old.placementMode().next();
        data.settings(new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
            old.confirmDiscardedChanges(), old.confirmClearFluid(), old.animationsEnabled(),
            old.soundsEnabled(), old.sort(), next, old.smartDistance(), old.predictionMode()));
        player.displayClientMessage(Component.translatable(
            "message.riftgun.placement_mode", Component.translatable("screen.riftgun.placement_mode."
                + next.name().toLowerCase(Locale.ROOT))), true);
        return true;
    }

    static boolean updatePlayerSettings(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
        DestinationSort sort;
        try {
            sort = DestinationSort.valueOf(request.getString("Sort"));
        } catch (IllegalArgumentException ignored) {
            sort = DestinationSort.RECENT;
        }
        PortalPlayerSettings settings = new PortalPlayerSettings(
            request.getBoolean("SafetyCheck"),
            request.getBoolean("ConfirmDeletion"),
            request.getBoolean("ConfirmDiscardedChanges"),
            request.getBoolean("ConfirmClearFluid"),
            request.getBoolean("Animations"),
            request.getBoolean("Sounds"),
            sort,
            PortalPlacementMode.parse(request.getString("PlacementMode")),
            data.settings().smartDistance(),
            PortalPredictionMode.parse(request.getString("MotionPrediction"), PortalPredictionMode.OFF)
        );
        data.settings(settings);
        PortalServices.MOTION_HISTORY.setPredictionEnabled(player,
            settings.predictionMode() != PortalPredictionMode.OFF);
        return true;
    }

    static boolean updateModuleSettings(PortalPlayerData data, CompoundTag request, ItemStack gun) {
        PortalGunModuleSettings settings = PortalGunModuleSettings.ensure(gun, data.settings().smartDistance());
        PortalModuleRules rules = PortalModuleRules.current();
        String setting = request.getString("Setting");
        switch (setting) {
            case "SmartDistance" -> {
                int maximum = PortalGunCapabilities.resolve(
                    gun, data.settings().smartDistance()).configuredSurfaceRange();
                settings = settings.withSmartDistance(Math.clamp(request.getInt("Value"), 1, maximum));
            }
            case "SurfaceRange" -> {
                requireModule(gun, PortalModuleKind.SURFACE_RANGE, rules,
                    "message.riftgun.surface_range_module_required");
                int maximum = rules.maximumSurfaceRangeFor(
                    PortalGunModules.activeCount(gun, PortalModuleKind.SURFACE_RANGE, rules));
                settings = settings.withDesiredSurfaceRange(
                    Math.clamp(request.getInt("Value"), rules.baseSurfaceRange(), maximum));
            }
            case "PortalDuration" -> settings = settings.withPortalDurationSeconds(
                PortalGunCapabilities.configuredDurationSeconds(gun, request.getInt("Value")));
            case "TransitCooldown" -> settings = settings.withTransitCooldownTenths(request.getInt("Value"));
            case "ExpandedAperture" -> {
                requireModule(gun, PortalModuleKind.APERTURE_EXPANSION, rules,
                    "message.riftgun.aperture_module_required");
                settings = settings.withExpandedApertureEnabled(request.getBoolean("Enabled"));
            }
            case "PassiveTransit", "HostileTransit", "BossTransit" -> {
                PortalModuleKind kind = switch (setting) {
                    case "PassiveTransit" -> PortalModuleKind.PASSIVE_TRANSIT;
                    case "HostileTransit" -> PortalModuleKind.HOSTILE_TRANSIT;
                    default -> PortalModuleKind.BOSS_TRANSIT;
                };
                requireModule(gun, kind, rules, "message.riftgun.entity_module_required");
                settings = settings.withTransit(kind, request.getBoolean("Enabled"));
            }
            case "PlayerTarget" -> {
                requireModule(gun, PortalModuleKind.PLAYER_TARGET, rules,
                    "message.riftgun.player_target_module_required");
                settings = settings.withPlayerTargetEnabled(request.getBoolean("Enabled"));
            }
            case "PlayerExclude" -> {
                requireModule(gun, PortalModuleKind.PLAYER_TARGET, rules,
                    "message.riftgun.player_target_module_required");
                settings = settings.withPlayerExcludeMode(
                    settings.playerExcludeMode().step(request.getInt("Step")));
            }
            case "FallGuard" -> {
                requireModule(gun, PortalModuleKind.FALL_GUARD, rules,
                    "message.riftgun.fall_guard_module_required");
                settings = settings.withFallGuardEnabled(request.getBoolean("Enabled"));
            }
            default -> throw PortalRequestFields.error("message.riftgun.invalid_request");
        }
        settings.save(gun);
        return true;
    }

    static boolean toggleBucketMode(ServerPlayer player, ItemStack gun) {
        boolean enabled = !PortalGunMode.bucketMode(gun);
        PortalGunMode.bucketMode(gun, enabled);
        player.displayClientMessage(Component.translatable(enabled
            ? "message.riftgun.bucket_mode_enabled" : "message.riftgun.bucket_mode_disabled"), true);
        return true;
    }

    static boolean clearFluid(ServerPlayer player, ItemStack gun) {
        PortalGunTank tank = new PortalGunTank(gun);
        int amount = tank.getFluid().getAmount();
        if (amount <= 0) return false;
        tank.clear();
        player.displayClientMessage(Component.translatable("message.riftgun.fluid_cleared", amount), true);
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

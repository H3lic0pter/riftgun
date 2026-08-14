package dev.riftgun.network;

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
import dev.riftgun.sound.PortalSoundSettings;
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
        boolean relocationAvailable = PortalGunCapabilities.resolve(
            gun, old.smartDistance()).entityRelocation();
        PortalPlacementMode next = adjacentAvailableMode(
            old.placementMode(), reverse, relocationAvailable);
        data.settings(new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
            old.confirmDiscardedChanges(), old.confirmClearFluid(), old.animationsEnabled(),
            old.soundsEnabled(), old.sort(), next, old.smartDistance(), old.predictionMode(),
            old.portalSounds()));
        player.displayClientMessage(Component.translatable(
            "message.riftgun.placement_mode", Component.translatable("screen.riftgun.placement_mode."
                + next.name().toLowerCase(Locale.ROOT))), true);
        return true;
    }

    static PortalPlacementMode adjacentAvailableMode(PortalPlacementMode current, boolean reverse,
                                                       boolean relocationAvailable) {
        PortalPlacementMode candidate = reverse ? current.previous() : current.next();
        if (candidate == PortalPlacementMode.ENTITY_RELOCATION && !relocationAvailable) {
            candidate = reverse ? candidate.previous() : candidate.next();
        }
        return candidate;
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
            PortalPredictionMode.parse(request.getString("MotionPrediction"), PortalPredictionMode.OFF),
            request.contains("PortalSounds", Tag.TAG_COMPOUND)
                ? PortalSoundSettings.load(request.getCompound("PortalSounds"))
                : data.settings().portalSounds()
        );
        data.settings(settings);
        RiftRuntime.current().motionHistory().setPredictionEnabled(player,
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
            case "PassiveTransit", "HostileTransit", "BossTransit", "ProjectileTransit" -> {
                PortalModuleKind kind = switch (setting) {
                    case "PassiveTransit" -> PortalModuleKind.PASSIVE_TRANSIT;
                    case "HostileTransit" -> PortalModuleKind.HOSTILE_TRANSIT;
                    case "BossTransit" -> PortalModuleKind.BOSS_TRANSIT;
                    default -> PortalModuleKind.PROJECTILE_TRANSIT;
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
            case "FallGuardEntities" -> {
                requireModule(gun, PortalModuleKind.FALL_GUARD, rules,
                    "message.riftgun.fall_guard_module_required");
                settings = settings.withFallGuardEntitiesEnabled(request.getBoolean("Enabled"));
            }
            case "EntityRelocation" -> {
                requireModule(gun, PortalModuleKind.ENTITY_RELOCATION, rules,
                    "message.riftgun.entity_relocation_module_required");
                boolean enabled = request.getBoolean("Enabled");
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
                settings = settings.withEntityRelocationSmartRouting(request.getBoolean("Enabled"));
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

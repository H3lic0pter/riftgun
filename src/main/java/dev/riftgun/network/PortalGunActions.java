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
        Msg.displayClientMessage(player, Component.translatable(
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
            sort = DestinationSort.valueOf(Nbt.getString(request, "Sort"));
        } catch (IllegalArgumentException ignored) {
            sort = DestinationSort.RECENT;
        }
        PortalPlayerSettings settings = new PortalPlayerSettings(
            Nbt.getBoolean(request, "SafetyCheck"),
            Nbt.getBoolean(request, "ConfirmDeletion"),
            Nbt.getBoolean(request, "ConfirmDiscardedChanges"),
            Nbt.getBoolean(request, "ConfirmClearFluid"),
            Nbt.getBoolean(request, "Animations"),
            Nbt.getBoolean(request, "Sounds"),
            sort,
            PortalPlacementMode.parse(Nbt.getString(request, "PlacementMode")),
            data.settings().smartDistance(),
            PortalPredictionMode.parse(Nbt.getString(request, "MotionPrediction"), PortalPredictionMode.OFF),
            Nbt.contains(request, "PortalSounds")
                ? PortalSoundSettings.load(Nbt.getCompound(request, "PortalSounds"))
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
        String setting = Nbt.getString(request, "Setting");
        switch (setting) {
            case "SmartDistance" -> {
                int maximum = PortalGunCapabilities.resolve(
                    gun, data.settings().smartDistance()).configuredSurfaceRange();
                settings = settings.withSmartDistance(Math.clamp(Nbt.getInt(request, "Value"), 1, maximum));
            }
            case "SurfaceRange" -> {
                requireModule(gun, PortalModuleKind.SURFACE_RANGE, rules,
                    "message.riftgun.surface_range_module_required");
                int maximum = rules.maximumSurfaceRangeFor(
                    PortalGunModules.activeCount(gun, PortalModuleKind.SURFACE_RANGE, rules));
                settings = settings.withDesiredSurfaceRange(
                    Math.clamp(Nbt.getInt(request, "Value"), rules.baseSurfaceRange(), maximum));
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
            default -> throw PortalRequestFields.error("message.riftgun.invalid_request");
        }
        settings.save(gun);
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

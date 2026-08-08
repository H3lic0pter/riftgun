package dev.riftgun.network;

import dev.riftgun.config.ServerConfig;
import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.DestinationSort;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlayerSettings;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.fuel.PortalGunTank;
import dev.riftgun.module.PortalModuleMenu;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.portal.PortalOpenDuration;
import dev.riftgun.service.CoordinateParser;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalOpenCoordinator;
import dev.riftgun.service.PortalServices;
import dev.riftgun.service.ServerPlayerRoster;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class PortalRequestHandler {
    public static void handle(ServerPlayer player, CompoundTag request) {
        PortalAction action;
        try {
            action = PortalAction.valueOf(request.getString("Action"));
        } catch (IllegalArgumentException ignored) {
            return;
        }

        Optional<PortalGunLocator.LocatedGun> locatedGun = request.contains("GunReference")
            ? PortalGunLocator.resolveReference(player, request.getCompound("GunReference"))
            : PortalGunLocator.first(player);
        if (player.isSpectator() || locatedGun.isEmpty()) {
            if (action == PortalAction.CYCLE_PLACEMENT_MODE) return;
            player.displayClientMessage(Component.translatable(
                player.isSpectator() ? "message.riftgun.spectator_denied" : "message.riftgun.no_portal_gun"
            ), true);
            return;
        }

        if (action == PortalAction.OPEN_GUI) {
            PortalNetworking.sendSnapshot(player, true, locatedGun.get());
            return;
        }
        if (action == PortalAction.OPEN_MODULES) {
            PortalModuleMenu.open(player, locatedGun.get());
            return;
        }

        PortalPlayerData data = PortalDataStore.load(player);
        try {
            boolean changed = switch (action) {
                case CREATE_CURRENT -> createCurrent(player, data, request);
                case CREATE_COORDINATE -> createCoordinate(player, data, request, locatedGun.get().stack());
                case EDIT_DESTINATION -> editDestination(player, data, request, locatedGun.get().stack());
                case DELETE_DESTINATION -> deleteDestination(data, request);
                case TOGGLE_PIN -> togglePin(data, request);
                case VIEW_DESTINATION -> viewDestination(data, request);
                case SELECT_DESTINATION -> selectDestination(player, data, request);
                case SELECT_PLAYER -> selectPlayerTarget(player, data, request);
                case OPEN_PORTAL -> {
                    PortalOpenCoordinator.request(player, data, id(request, "Destination"), true,
                        PortalPlacementMode.FRONT, locatedGun.get());
                    yield false;
                }
                case OPEN_SELECTED -> {
                    openSelected(player, data, requestedPlacement(request), locatedGun.get());
                    yield false;
                }
                case CYCLE_PLACEMENT_MODE -> cyclePlacementMode(player, data);
                case CREATE_GROUP -> createGroup(data, request);
                case RENAME_GROUP -> renameGroup(data, request);
                case DELETE_GROUP -> deleteGroup(data, request);
                case MOVE_GROUP -> moveGroup(data, request);
                case MOVE_DESTINATION_GROUP -> moveDestinationGroup(data, request);
                case SET_GROUP_EXPANDED -> setExpanded(data, request);
                case SET_SETTINGS -> setSettings(player, data, request);
                case SET_GUN_MODULE_SETTINGS -> setGunModuleSettings(
                    data, request, locatedGun.get().stack());
                case TOGGLE_BUCKET_MODE -> toggleBucketMode(player, locatedGun.get().stack());
                case CLEAR_GUN_FLUID -> clearGunFluid(player, locatedGun.get().stack());
                case REQUEST_PLAYERS -> {
                    sendPlayerList(player, locatedGun.get().stack());
                    yield false;
                }
                case OPEN_PLAYER_PORTAL -> {
                    openPlayerPortal(player, data, request, locatedGun.get());
                    yield false;
                }
                case TOGGLE_PLAYER_PIN -> togglePlayerPin(data, request);
                case OPEN_GUI, OPEN_MODULES -> false;
            };
            if (changed) {
                PortalDataStore.save(player, data);
                if (action == PortalAction.SELECT_DESTINATION) {
                    PortalNetworking.sendSelectionAccepted(player, data.selectedDestinationId());
                } else {
                    PortalNetworking.sendSnapshot(player, false, locatedGun.get());
                }
            }
        } catch (UserInputException exception) {
            player.displayClientMessage(Component.translatable(exception.translationKey), true);
        } catch (NumberFormatException exception) {
            player.displayClientMessage(Component.translatable("message.riftgun.invalid_coordinate"), true);
        }
    }

    public static boolean canUse(ServerPlayer player) {
        return !player.isSpectator() && PortalGunLocator.anyHasPortalGun(player);
    }

    public static void openSelectedFromItem(ServerPlayer player, InteractionHand hand) {
        if (player.isSpectator()) return;
        PortalPlayerData data = PortalDataStore.load(player);
        UUID playerTarget = data.selectedPlayerId();
        if (playerTarget != null) {
            PortalGunLocator.LocatedGun gun = PortalGunLocator.first(player).orElse(null);
            if (gun == null || gun.stack() != player.getItemInHand(hand)) return;
            openPlayerTargetFromItem(player, data, gun);
            return;
        }
        UUID selected = data.selectedDestinationId();
        if (selected == null) {
            player.displayClientMessage(Component.translatable("message.riftgun.no_destination_selected"), true);
            return;
        }
        PortalGunLocator.LocatedGun gun = PortalGunLocator.first(player).orElse(null);
        if (gun == null || gun.stack() != player.getItemInHand(hand)) return;
        PortalOpenCoordinator.request(player, data, selected, false,
            data.settings().placementMode(), gun);
    }

    private static void openPlayerTargetFromItem(ServerPlayer player, PortalPlayerData data,
                                                 PortalGunLocator.LocatedGun gun) {
        if (player.getServer() == null || player.getServer().getPlayerList().getPlayer(data.selectedPlayerId()) == null) {
            player.displayClientMessage(Component.translatable("message.riftgun.player_target_offline"), true);
            return;
        }
        PortalOpenCoordinator.requestPlayerTarget(player, data, data.selectedPlayerId(), false,
            data.settings().placementMode(), gun);
    }

    private static void openSelected(ServerPlayer player, PortalPlayerData data, PortalPlacementMode mode,
                                     PortalGunLocator.LocatedGun gun) {
        UUID playerTarget = data.selectedPlayerId();
        if (playerTarget != null) {
            if (player.getServer() == null || player.getServer().getPlayerList().getPlayer(playerTarget) == null) {
                throw error("message.riftgun.player_target_offline");
            }
            PortalOpenCoordinator.requestPlayerTarget(player, data, playerTarget, false, mode, gun);
            return;
        }
        UUID selected = data.selectedDestinationId();
        if (selected == null) throw error("message.riftgun.no_destination_selected");
        PortalOpenCoordinator.request(player, data, selected, false, mode, gun);
    }

    private static boolean cyclePlacementMode(ServerPlayer player, PortalPlayerData data) {
        PortalPlayerSettings old = data.settings();
        PortalPlacementMode next = old.placementMode().next();
        data.settings(new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
            old.confirmDiscardedChanges(), old.confirmClearFluid(), old.animationsEnabled(), old.soundsEnabled(), old.sort(),
            next, old.smartDistance(), old.motionPredictionEnabled()));
        player.displayClientMessage(Component.translatable(
            "message.riftgun.placement_mode", Component.translatable("screen.riftgun.placement_mode."
                + next.name().toLowerCase(Locale.ROOT))), true);
        return true;
    }

    private static boolean createCurrent(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
        requireDestinationCapacity(data);
        UUID group = validGroup(data, optionalId(request, "Group"));
        String name = destinationName(data, request.getString("Name"), true);
        long time = player.level().getGameTime();
        UUID destinationId = UUID.randomUUID();
        data.destinations().add(new Destination(
            destinationId, name, group, player.level().dimension(),
            player.getX(), player.getY(), player.getZ(), player.getYRot(), time, 0L, false
        ));
        data.selectedDestinationId(destinationId);
        data.lastViewedDestinationId(destinationId);
        return true;
    }

    private static boolean createCoordinate(ServerPlayer player, PortalPlayerData data, CompoundTag request,
                                            ItemStack gun) {
        if (!PortalGunCapabilities.resolve(gun, data.settings().smartDistance()).coordinateOverride()) {
            throw error("message.riftgun.coordinate_module_required");
        }
        requireDestinationCapacity(data);
        requireCoordinateLengths(request);
        UUID group = validGroup(data, optionalId(request, "Group"));
        String name = destinationName(data, request.getString("Name"), true);
        double x = CoordinateParser.parse(request.getString("X"), player.getX());
        double y = CoordinateParser.parse(request.getString("Y"), player.getY());
        double z = CoordinateParser.parse(request.getString("Z"), player.getZ());
        float yaw = CoordinateParser.parseYaw(request.getString("Yaw"), player.getYRot());
        long time = player.level().getGameTime();
        UUID destinationId = UUID.randomUUID();
        data.destinations().add(new Destination(
            destinationId, name, group, player.level().dimension(), x, y, z, yaw, time, 0L, false
        ));
        data.selectedDestinationId(destinationId);
        data.lastViewedDestinationId(destinationId);
        return true;
    }

    private static boolean editDestination(ServerPlayer player, PortalPlayerData data, CompoundTag request,
                                           ItemStack gun) {
        UUID destinationId = id(request, "Destination");
        Destination current = data.destination(destinationId).orElseThrow(() -> error("message.riftgun.destination_missing"));
        UUID group = validGroup(data, optionalId(request, "Group"));
        String name = destinationName(data, request.getString("Name"), false);
        boolean coordinateOverride = PortalGunCapabilities.resolve(
            gun, data.settings().smartDistance()).coordinateOverride();
        if (coordinateOverride) requireCoordinateLengths(request);
        double x = coordinateOverride ? CoordinateParser.parse(request.getString("X"), player.getX()) : current.x();
        double y = coordinateOverride ? CoordinateParser.parse(request.getString("Y"), player.getY()) : current.y();
        double z = coordinateOverride ? CoordinateParser.parse(request.getString("Z"), player.getZ()) : current.z();
        float yaw = coordinateOverride
            ? CoordinateParser.parseYaw(request.getString("Yaw"), player.getYRot()) : current.yaw();
        data.replaceDestination(current.withDetails(name, group, current.dimension(), x, y, z, yaw));
        return true;
    }

    private static boolean deleteDestination(PortalPlayerData data, CompoundTag request) {
        UUID destinationId = id(request, "Destination");
        boolean removed = data.destinations().removeIf(destination -> destination.id().equals(destinationId));
        if (!removed) throw error("message.riftgun.destination_missing");
        if (destinationId.equals(data.selectedDestinationId())) data.selectedDestinationId(null);
        if (destinationId.equals(data.lastViewedDestinationId())) data.lastViewedDestinationId(null);
        data.clearSafetyResult(destinationId);
        return true;
    }

    private static boolean togglePin(PortalPlayerData data, CompoundTag request) {
        Destination destination = data.destination(id(request, "Destination"))
            .orElseThrow(() -> error("message.riftgun.destination_missing"));
        data.replaceDestination(destination.withPinned(!destination.pinned()));
        return true;
    }

    private static boolean viewDestination(PortalPlayerData data, CompoundTag request) {
        UUID destinationId = id(request, "Destination");
        if (data.destination(destinationId).isEmpty()) throw error("message.riftgun.destination_missing");
        data.lastViewedDestinationId(destinationId);
        return true;
    }

    private static boolean selectDestination(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
        UUID destinationId = id(request, "Destination");
        if (data.destination(destinationId).isEmpty()) {
            throw error("message.riftgun.destination_missing");
        }
        data.selectedPlayerId(null);
        data.selectedDestinationId(destinationId);
        data.lastViewedDestinationId(destinationId);
        return true;
    }

    private static boolean selectPlayerTarget(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
        UUID playerId = id(request, "Target");
        if (player.getServer() == null || player.getServer().getPlayerList().getPlayer(playerId) == null) {
            throw error("message.riftgun.player_target_offline");
        }
        data.selectedDestinationId(null);
        data.selectedPlayerId(playerId);
        data.lastViewedDestinationId(null);
        return true;
    }

    private static boolean createGroup(PortalPlayerData data, CompoundTag request) {
        if (data.groups().size() >= ServerConfig.VALUES.maxGroups.get()) {
            throw error("message.riftgun.group_limit");
        }
        String name = groupName(data, request.getString("Name"), null);
        int order = data.groups().stream().mapToInt(DestinationGroup::order).max().orElse(-1) + 1;
        DestinationGroup group = new DestinationGroup(UUID.randomUUID(), name, order);
        data.groups().add(group);
        data.expandedGroups().add(group.id());
        return true;
    }

    private static boolean renameGroup(PortalPlayerData data, CompoundTag request) {
        UUID groupId = id(request, "Group");
        if (groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) throw error("message.riftgun.default_group_immutable");
        DestinationGroup group = data.group(groupId).orElseThrow(() -> error("message.riftgun.group_missing"));
        String name = groupName(data, request.getString("Name"), groupId);
        data.replaceGroup(group.withName(name));
        return true;
    }

    private static boolean deleteGroup(PortalPlayerData data, CompoundTag request) {
        UUID groupId = id(request, "Group");
        if (groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) throw error("message.riftgun.default_group_immutable");
        if (data.group(groupId).isEmpty()) throw error("message.riftgun.group_missing");
        data.groups().removeIf(group -> group.id().equals(groupId));
        for (int index = 0; index < data.destinations().size(); index++) {
            Destination destination = data.destinations().get(index);
            if (destination.groupId().equals(groupId)) {
                data.destinations().set(index, destination.withGroup(PortalPlayerData.DEFAULT_GROUP_ID));
            }
        }
        data.expandedGroups().remove(groupId);
        normalizeGroupOrder(data);
        return true;
    }

    private static boolean moveGroup(PortalPlayerData data, CompoundTag request) {
        UUID groupId = id(request, "Group");
        if (groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) return false;
        List<DestinationGroup> ordered = new ArrayList<>(data.groups());
        ordered.sort(Comparator.comparingInt(DestinationGroup::order));
        int from = -1;
        for (int index = 0; index < ordered.size(); index++) {
            if (ordered.get(index).id().equals(groupId)) from = index;
        }
        if (from < 0) throw error("message.riftgun.group_missing");
        int to = request.contains("TargetIndex")
            ? Math.max(0, Math.min(ordered.size() - 1, request.getInt("TargetIndex")))
            : Math.max(0, Math.min(ordered.size() - 1, from + Math.max(-1, Math.min(1, request.getInt("Delta")))));
        if (to == from) return false;
        DestinationGroup moved = ordered.remove(from);
        ordered.add(to, moved);
        for (int index = 0; index < ordered.size(); index++) data.replaceGroup(ordered.get(index).withOrder(index));
        return true;
    }

    private static boolean moveDestinationGroup(PortalPlayerData data, CompoundTag request) {
        Destination destination = data.destination(id(request, "Destination"))
            .orElseThrow(() -> error("message.riftgun.destination_missing"));
        UUID groupId = validGroup(data, optionalId(request, "Group"));
        if (destination.groupId().equals(groupId)) return false;
        data.replaceDestination(destination.withGroup(groupId));
        data.selectedDestinationId(destination.id());
        data.lastViewedDestinationId(destination.id());
        return true;
    }

    private static void sendPlayerList(ServerPlayer player, ItemStack gun) {
        if (PortalGunModules.activeCount(gun, PortalModuleKind.PLAYER_TARGET,
            PortalModuleRules.current()) <= 0) return;
        PortalPlayerData data = PortalDataStore.load(player);
        data.prunePlayerTargets(ServerPlayerRoster.onlinePlayerIds(player.getServer()));
        PortalDataStore.save(player, data);
        PortalNetworking.sendPlayerList(player);
    }

    private static void openPlayerPortal(ServerPlayer player, PortalPlayerData data,
                                         CompoundTag request, PortalGunLocator.LocatedGun gun) {
        PortalOpenCoordinator.requestPlayerTarget(player, data,
            id(request, "Target"), true, data.settings().placementMode(), gun);
    }

    private static boolean togglePlayerPin(PortalPlayerData data, CompoundTag request) {
        UUID playerId = id(request, "Target");
        if (data.isPlayerPinned(playerId)) data.pinnedPlayers().remove(playerId);
        else data.pinnedPlayers().add(playerId);
        return true;
    }

    private static boolean setExpanded(PortalPlayerData data, CompoundTag request) {
        UUID groupId = id(request, "Group");
        if (!groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID) && data.group(groupId).isEmpty()) {
            throw error("message.riftgun.group_missing");
        }
        if (request.getBoolean("Expanded")) data.expandedGroups().add(groupId);
        else data.expandedGroups().remove(groupId);
        return true;
    }

    private static boolean setSettings(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
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
            request.getBoolean("MotionPrediction")
        );
        data.settings(settings);
        PortalServices.MOTION_HISTORY.setPredictionEnabled(player, settings.motionPredictionEnabled());
        return true;
    }

    private static boolean setGunModuleSettings(PortalPlayerData data, CompoundTag request, ItemStack gun) {
        PortalGunModuleSettings settings = PortalGunModuleSettings.ensure(gun, data.settings().smartDistance());
        PortalModuleRules rules = PortalModuleRules.current();
        String setting = request.getString("Setting");
        switch (setting) {
            case "SmartDistance" -> {
                int maximum = PortalGunCapabilities.resolve(
                    gun, data.settings().smartDistance()).configuredSurfaceRange();
                settings = settings.withSmartDistance(Math.max(1, Math.min(maximum, request.getInt("Value"))));
            }
            case "SurfaceRange" -> {
                if (PortalGunModules.activeCount(gun, PortalModuleKind.SURFACE_RANGE, rules) <= 0) {
                    throw error("message.riftgun.surface_range_module_required");
                }
                int maximum = rules.maximumSurfaceRangeFor(
                    PortalGunModules.activeCount(gun, PortalModuleKind.SURFACE_RANGE, rules));
                settings = settings.withDesiredSurfaceRange(
                    Math.max(rules.baseSurfaceRange(), Math.min(maximum, request.getInt("Value"))));
            }
            case "PortalDuration" -> settings = settings.withPortalDurationSeconds(
                PortalOpenDuration.effectiveSeconds(request.getInt("Value"),
                    ServerConfig.VALUES.maximumPortalDurationSeconds.get()));
            case "TransitCooldown" -> settings = settings.withTransitCooldownTenths(request.getInt("Value"));
            case "ExpandedAperture" -> {
                if (PortalGunModules.activeCount(gun, PortalModuleKind.APERTURE_EXPANSION, rules) <= 0) {
                    throw error("message.riftgun.aperture_module_required");
                }
                settings = settings.withExpandedApertureEnabled(request.getBoolean("Enabled"));
            }
            case "PassiveTransit", "HostileTransit", "BossTransit" -> {
                PortalModuleKind kind = switch (setting) {
                    case "PassiveTransit" -> PortalModuleKind.PASSIVE_TRANSIT;
                    case "HostileTransit" -> PortalModuleKind.HOSTILE_TRANSIT;
                    default -> PortalModuleKind.BOSS_TRANSIT;
                };
                if (PortalGunModules.activeCount(gun, kind, rules) <= 0) {
                    throw error("message.riftgun.entity_module_required");
                }
                settings = settings.withTransit(kind, request.getBoolean("Enabled"));
            }
            case "PlayerTarget" -> {
                if (PortalGunModules.activeCount(gun, PortalModuleKind.PLAYER_TARGET, rules) <= 0) {
                    throw error("message.riftgun.player_target_module_required");
                }
                settings = settings.withPlayerTargetEnabled(request.getBoolean("Enabled"));
            }
            case "PlayerExclude" -> {
                if (PortalGunModules.activeCount(gun, PortalModuleKind.PLAYER_TARGET, rules) <= 0) {
                    throw error("message.riftgun.player_target_module_required");
                }
                settings = settings.withPlayerExcludeEnabled(request.getBoolean("Enabled"));
            }
            default -> throw error("message.riftgun.invalid_request");
        }
        settings.save(gun);
        return true;
    }

    private static boolean toggleBucketMode(ServerPlayer player, ItemStack gun) {
        boolean enabled = !PortalGunMode.bucketMode(gun);
        PortalGunMode.bucketMode(gun, enabled);
        player.displayClientMessage(Component.translatable(enabled
            ? "message.riftgun.bucket_mode_enabled" : "message.riftgun.bucket_mode_disabled"), true);
        return true;
    }

    private static boolean clearGunFluid(ServerPlayer player, ItemStack gun) {
        PortalGunTank tank = new PortalGunTank(gun);
        int amount = tank.getFluid().getAmount();
        if (amount <= 0) return false;
        tank.clear();
        player.displayClientMessage(Component.translatable("message.riftgun.fluid_cleared", amount), true);
        return true;
    }

    private static PortalPlacementMode requestedPlacement(CompoundTag request) {
        PortalPlacementMode mode = PortalPlacementMode.parse(request.getString("PlacementMode"));
        return mode == PortalPlacementMode.SMART ? PortalPlacementMode.FRONT : mode;
    }

    private static void requireDestinationCapacity(PortalPlayerData data) {
        if (data.destinations().size() >= ServerConfig.VALUES.maxDestinations.get()) {
            throw error("message.riftgun.destination_limit");
        }
    }

    private static void requireCoordinateLengths(CompoundTag request) {
        for (String key : List.of("X", "Y", "Z", "Yaw")) {
            if (request.getString(key).length() > 64) throw error("message.riftgun.invalid_coordinate");
        }
    }

    private static String destinationName(PortalPlayerData data, String raw, boolean allowDefault) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty() && allowDefault) name = data.nextLocationName();
        if (name.isEmpty()) throw error("message.riftgun.name_empty");
        if (name.length() > ServerConfig.VALUES.maxDestinationNameLength.get()) {
            throw error("message.riftgun.name_too_long");
        }
        return name;
    }

    private static String groupName(PortalPlayerData data, String raw, UUID ignoredGroup) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty()) throw error("message.riftgun.name_empty");
        if (name.length() > ServerConfig.VALUES.maxGroupNameLength.get()) throw error("message.riftgun.name_too_long");
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.equals("default")) throw error("message.riftgun.group_duplicate");
        boolean duplicate = data.groups().stream()
            .filter(group -> ignoredGroup == null || !group.id().equals(ignoredGroup))
            .anyMatch(group -> group.name().strip().toLowerCase(Locale.ROOT).equals(normalized));
        if (duplicate) throw error("message.riftgun.group_duplicate");
        return name;
    }

    private static UUID validGroup(PortalPlayerData data, UUID groupId) {
        if (groupId == null || groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) return PortalPlayerData.DEFAULT_GROUP_ID;
        return data.group(groupId).isPresent() ? groupId : PortalPlayerData.DEFAULT_GROUP_ID;
    }

    private static UUID id(CompoundTag tag, String key) {
        if (!tag.hasUUID(key)) throw error("message.riftgun.invalid_request");
        return tag.getUUID(key);
    }

    private static UUID optionalId(CompoundTag tag, String key) {
        return tag.hasUUID(key) ? tag.getUUID(key) : PortalPlayerData.DEFAULT_GROUP_ID;
    }

    private static void normalizeGroupOrder(PortalPlayerData data) {
        List<DestinationGroup> groups = new ArrayList<>(data.groups());
        groups.sort(Comparator.comparingInt(DestinationGroup::order));
        for (int index = 0; index < groups.size(); index++) data.replaceGroup(groups.get(index).withOrder(index));
    }

    private static UserInputException error(String translationKey) {
        return new UserInputException(translationKey);
    }

    private static final class UserInputException extends RuntimeException {
        private final String translationKey;

        private UserInputException(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private PortalRequestHandler() {}
}

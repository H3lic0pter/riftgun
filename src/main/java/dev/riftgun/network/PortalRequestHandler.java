package dev.riftgun.network;

import dev.riftgun.config.ServerConfig;
import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.DestinationSort;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlayerSettings;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.service.CoordinateParser;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalServices;
import dev.riftgun.service.SafetyReport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class PortalRequestHandler {
    public static void handle(ServerPlayer player, CompoundTag request) {
        PortalAction action;
        try {
            action = PortalAction.valueOf(request.getString("Action"));
        } catch (IllegalArgumentException ignored) {
            return;
        }

        if (!canUse(player)) {
            player.displayClientMessage(Component.translatable(
                player.isSpectator() ? "message.riftgun.spectator_denied" : "message.riftgun.no_portal_gun"
            ), true);
            return;
        }

        if (action == PortalAction.OPEN_GUI) {
            PortalNetworking.sendSnapshot(player, true);
            return;
        }

        PortalPlayerData data = PortalDataStore.load(player);
        try {
            boolean changed = switch (action) {
                case CREATE_CURRENT -> createCurrent(player, data, request);
                case CREATE_COORDINATE -> createCoordinate(player, data, request);
                case EDIT_DESTINATION -> editDestination(player, data, request);
                case DELETE_DESTINATION -> deleteDestination(data, request);
                case TOGGLE_PIN -> togglePin(data, request);
                case VIEW_DESTINATION -> viewDestination(data, request);
                case SELECT_DESTINATION -> selectDestination(player, data, request);
                case OPEN_PORTAL -> {
                    yield openDestination(player, data, id(request, "Destination"), true,
                        request.getBoolean("ConfirmedUnsafe"));
                }
                case CHECK_SAFETY -> {
                    checkSafety(player, data, id(request, "Destination"), false);
                    yield false;
                }
                case CREATE_GROUP -> createGroup(data, request);
                case RENAME_GROUP -> renameGroup(data, request);
                case DELETE_GROUP -> deleteGroup(data, request);
                case MOVE_GROUP -> moveGroup(data, request);
                case SET_GROUP_EXPANDED -> setExpanded(data, request);
                case SET_SETTINGS -> setSettings(data, request);
                case OPEN_GUI -> false;
            };
            if (changed) {
                PortalDataStore.save(player, data);
                PortalNetworking.sendSnapshot(player, false);
                if (action == PortalAction.OPEN_PORTAL) PortalNetworking.sendPortalOpened(player);
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

    public static void openSelectedFromItem(ServerPlayer player) {
        if (!canUse(player)) return;
        PortalPlayerData data = PortalDataStore.load(player);
        UUID selected = data.selectedDestinationId();
        if (selected == null) {
            player.displayClientMessage(Component.translatable("message.riftgun.no_destination_selected"), true);
            return;
        }
        try {
            if (openDestination(player, data, selected, false, true)) {
                PortalDataStore.save(player, data);
                PortalNetworking.sendSnapshot(player, false);
            }
        } catch (UserInputException exception) {
            player.displayClientMessage(Component.translatable(exception.translationKey), true);
        }
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

    private static boolean createCoordinate(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
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

    private static boolean editDestination(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
        requireCoordinateLengths(request);
        UUID destinationId = id(request, "Destination");
        Destination current = data.destination(destinationId).orElseThrow(() -> error("message.riftgun.destination_missing"));
        UUID group = validGroup(data, optionalId(request, "Group"));
        String name = destinationName(data, request.getString("Name"), false);
        double x = CoordinateParser.parse(request.getString("X"), player.getX());
        double y = CoordinateParser.parse(request.getString("Y"), player.getY());
        double z = CoordinateParser.parse(request.getString("Z"), player.getZ());
        float yaw = CoordinateParser.parseYaw(request.getString("Yaw"), player.getYRot());
        data.replaceDestination(current.withDetails(name, group, current.dimension(), x, y, z, yaw));
        return true;
    }

    private static boolean deleteDestination(PortalPlayerData data, CompoundTag request) {
        UUID destinationId = id(request, "Destination");
        boolean removed = data.destinations().removeIf(destination -> destination.id().equals(destinationId));
        if (!removed) throw error("message.riftgun.destination_missing");
        if (destinationId.equals(data.selectedDestinationId())) data.selectedDestinationId(null);
        if (destinationId.equals(data.lastViewedDestinationId())) data.lastViewedDestinationId(null);
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
        Destination destination = data.destination(destinationId)
            .orElseThrow(() -> error("message.riftgun.destination_missing"));
        data.selectedDestinationId(destinationId);
        data.lastViewedDestinationId(destinationId);
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

    private static boolean setExpanded(PortalPlayerData data, CompoundTag request) {
        UUID groupId = id(request, "Group");
        if (!groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID) && data.group(groupId).isEmpty()) {
            throw error("message.riftgun.group_missing");
        }
        if (request.getBoolean("Expanded")) data.expandedGroups().add(groupId);
        else data.expandedGroups().remove(groupId);
        return true;
    }

    private static boolean setSettings(PortalPlayerData data, CompoundTag request) {
        DestinationSort sort;
        try {
            sort = DestinationSort.valueOf(request.getString("Sort"));
        } catch (IllegalArgumentException ignored) {
            sort = DestinationSort.RECENT;
        }
        data.settings(new PortalPlayerSettings(
            request.getBoolean("SafetyCheck"),
            request.getBoolean("ConfirmDeletion"),
            request.getBoolean("ConfirmDiscardedChanges"),
            request.getBoolean("Animations"),
            request.getBoolean("Sounds"),
            sort
        ));
        return true;
    }

    private static boolean openDestination(ServerPlayer player, PortalPlayerData data, UUID destinationId,
                                           boolean fromGui, boolean confirmedUnsafe) {
        Destination destination = data.destination(destinationId)
            .orElseThrow(() -> error("message.riftgun.destination_missing"));
        var dimensionResult = PortalServices.DIMENSION_POLICY.validate(player, destination);
        if (!dimensionResult.allowed()) {
            player.displayClientMessage(dimensionResult.message(), true);
            return false;
        }

        SafetyReport report = SafetyReport.SAFE;
        if (data.settings().safetyCheckEnabled()) {
            report = PortalServices.SAFETY_INSPECTOR.inspect((ServerLevel) player.level(), destination);
            if (!report.safe() && fromGui && !confirmedUnsafe) {
                PortalNetworking.sendSafety(player, destination.id(), report.flags(), true);
                return false;
            }
            if (!report.safe()) {
                player.displayClientMessage(Component.translatable("message.riftgun.destination_unsafe"), true);
            }
        }

        Destination resolved = PortalServices.SAFE_DESTINATION_RESOLVER.resolve((ServerLevel) player.level(), destination, report);
        PortalEntity.openPair(player, resolved);
        data.selectedDestinationId(destination.id());
        data.replaceDestination(destination.usedAt(player.level().getGameTime()));
        return true;
    }

    private static void checkSafety(ServerPlayer player, PortalPlayerData data, UUID destinationId, boolean confirmation) {
        Destination destination = data.destination(destinationId)
            .orElseThrow(() -> error("message.riftgun.destination_missing"));
        if (!data.settings().safetyCheckEnabled()) {
            return;
        }
        var dimensionResult = PortalServices.DIMENSION_POLICY.validate(player, destination);
        if (!dimensionResult.allowed()) {
            player.displayClientMessage(dimensionResult.message(), true);
            return;
        }
        SafetyReport report = PortalServices.SAFETY_INSPECTOR.inspect((ServerLevel) player.level(), destination);
        PortalNetworking.sendSafety(player, destinationId, report.flags(), confirmation);
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

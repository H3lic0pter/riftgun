package dev.riftgun.network;

import dev.riftgun.config.ServerConfig;
import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.service.CoordinateParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Mutations for saved destinations and their shared group hierarchy. */
final class PortalDestinationActions {
    static boolean createCurrent(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
        requireDestinationCapacity(data);
        UUID group = validGroup(data, PortalRequestFields.optionalGroupId(request, "Group"));
        String name = destinationName(data, request.getString("Name"), true);
        long time = player.level().getGameTime();
        UUID destinationId = UUID.randomUUID();
        data.destinations().add(new Destination(
            destinationId, name, group, player.level().dimension(),
            player.getX(), player.getY(), player.getZ(), player.getYRot(), time, 0L, false
        ));
        select(data, destinationId);
        return true;
    }

    static boolean createCoordinate(ServerPlayer player, PortalPlayerData data,
                                    CompoundTag request, ItemStack gun) {
        if (!PortalGunCapabilities.resolve(gun, data.settings().smartDistance()).coordinateOverride()) {
            throw PortalRequestFields.error("message.riftgun.coordinate_module_required");
        }
        requireDestinationCapacity(data);
        requireCoordinateLengths(request);
        UUID group = validGroup(data, PortalRequestFields.optionalGroupId(request, "Group"));
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
        select(data, destinationId);
        return true;
    }

    static boolean edit(ServerPlayer player, PortalPlayerData data,
                        CompoundTag request, ItemStack gun) {
        UUID destinationId = PortalRequestFields.id(request, "Destination");
        Destination current = data.destination(destinationId).orElseThrow(
            () -> PortalRequestFields.error("message.riftgun.destination_missing"));
        UUID group = validGroup(data, PortalRequestFields.optionalGroupId(request, "Group"));
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

    static boolean delete(PortalPlayerData data, CompoundTag request) {
        UUID destinationId = PortalRequestFields.id(request, "Destination");
        boolean removed = data.destinations().removeIf(destination -> destination.id().equals(destinationId));
        if (!removed) throw PortalRequestFields.error("message.riftgun.destination_missing");
        if (destinationId.equals(data.selectedDestinationId())) data.selectedDestinationId(null);
        if (destinationId.equals(data.lastViewedDestinationId())) data.lastViewedDestinationId(null);
        data.clearSafetyResult(destinationId);
        return true;
    }

    static boolean togglePin(PortalPlayerData data, CompoundTag request) {
        Destination destination = data.destination(PortalRequestFields.id(request, "Destination"))
            .orElseThrow(() -> PortalRequestFields.error("message.riftgun.destination_missing"));
        data.replaceDestination(destination.withPinned(!destination.pinned()));
        return true;
    }

    static boolean view(PortalPlayerData data, CompoundTag request) {
        UUID destinationId = PortalRequestFields.id(request, "Destination");
        if (data.destination(destinationId).isEmpty()) {
            throw PortalRequestFields.error("message.riftgun.destination_missing");
        }
        data.lastViewedDestinationId(destinationId);
        return true;
    }

    static boolean select(PortalPlayerData data, CompoundTag request) {
        UUID destinationId = PortalRequestFields.id(request, "Destination");
        if (data.destination(destinationId).isEmpty()) {
            throw PortalRequestFields.error("message.riftgun.destination_missing");
        }
        data.selectedPlayerId(null);
        select(data, destinationId);
        return true;
    }

    static boolean createGroup(PortalPlayerData data, CompoundTag request) {
        if (data.groups().size() >= ServerConfig.VALUES.maxGroups.get()) {
            throw PortalRequestFields.error("message.riftgun.group_limit");
        }
        String name = groupName(data, request.getString("Name"), null);
        int order = data.groups().stream().mapToInt(DestinationGroup::order).max().orElse(-1) + 1;
        DestinationGroup group = new DestinationGroup(UUID.randomUUID(), name, order);
        data.groups().add(group);
        data.expandedGroups().add(group.id());
        return true;
    }

    static boolean renameGroup(PortalPlayerData data, CompoundTag request) {
        UUID groupId = PortalRequestFields.id(request, "Group");
        if (groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) {
            throw PortalRequestFields.error("message.riftgun.default_group_immutable");
        }
        DestinationGroup group = data.group(groupId).orElseThrow(
            () -> PortalRequestFields.error("message.riftgun.group_missing"));
        data.replaceGroup(group.withName(groupName(data, request.getString("Name"), groupId)));
        return true;
    }

    static boolean deleteGroup(PortalPlayerData data, CompoundTag request) {
        UUID groupId = PortalRequestFields.id(request, "Group");
        if (groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) {
            throw PortalRequestFields.error("message.riftgun.default_group_immutable");
        }
        if (data.group(groupId).isEmpty()) {
            throw PortalRequestFields.error("message.riftgun.group_missing");
        }
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

    static boolean moveGroup(PortalPlayerData data, CompoundTag request) {
        UUID groupId = PortalRequestFields.id(request, "Group");
        if (groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) return false;
        List<DestinationGroup> ordered = new ArrayList<>(data.groups());
        ordered.sort(Comparator.comparingInt(DestinationGroup::order));
        int from = -1;
        for (int index = 0; index < ordered.size(); index++) {
            if (ordered.get(index).id().equals(groupId)) from = index;
        }
        if (from < 0) throw PortalRequestFields.error("message.riftgun.group_missing");
        int to = request.contains("TargetIndex")
            ? Math.clamp(request.getInt("TargetIndex"), 0, ordered.size() - 1)
            : Math.clamp(from + Math.clamp(request.getInt("Delta"), -1, 1), 0, ordered.size() - 1);
        if (to == from) return false;
        DestinationGroup moved = ordered.remove(from);
        ordered.add(to, moved);
        for (int index = 0; index < ordered.size(); index++) {
            data.replaceGroup(ordered.get(index).withOrder(index));
        }
        return true;
    }

    static boolean moveDestinationGroup(PortalPlayerData data, CompoundTag request) {
        Destination destination = data.destination(PortalRequestFields.id(request, "Destination"))
            .orElseThrow(() -> PortalRequestFields.error("message.riftgun.destination_missing"));
        UUID groupId = validGroup(data, PortalRequestFields.optionalGroupId(request, "Group"));
        if (destination.groupId().equals(groupId)) return false;
        data.replaceDestination(destination.withGroup(groupId));
        select(data, destination.id());
        return true;
    }

    static boolean setExpanded(PortalPlayerData data, CompoundTag request) {
        UUID groupId = PortalRequestFields.id(request, "Group");
        if (!groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID)
            && !groupId.equals(PortalPlayerData.PLAYER_SECTION_ID)
            && data.group(groupId).isEmpty()) {
            throw PortalRequestFields.error("message.riftgun.group_missing");
        }
        if (request.getBoolean("Expanded")) data.expandedGroups().add(groupId);
        else data.expandedGroups().remove(groupId);
        return true;
    }

    private static void select(PortalPlayerData data, UUID destinationId) {
        data.selectedDestinationId(destinationId);
        data.lastViewedDestinationId(destinationId);
    }

    private static void requireDestinationCapacity(PortalPlayerData data) {
        if (data.destinations().size() >= ServerConfig.VALUES.maxDestinations.get()) {
            throw PortalRequestFields.error("message.riftgun.destination_limit");
        }
    }

    private static void requireCoordinateLengths(CompoundTag request) {
        for (String key : List.of("X", "Y", "Z", "Yaw")) {
            if (request.getString(key).length() > 64) {
                throw PortalRequestFields.error("message.riftgun.invalid_coordinate");
            }
        }
    }

    private static String destinationName(PortalPlayerData data, String raw, boolean allowDefault) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty() && allowDefault) name = data.nextLocationName();
        if (name.isEmpty()) throw PortalRequestFields.error("message.riftgun.name_empty");
        if (name.length() > ServerConfig.VALUES.maxDestinationNameLength.get()) {
            throw PortalRequestFields.error("message.riftgun.name_too_long");
        }
        return name;
    }

    private static String groupName(PortalPlayerData data, String raw, UUID ignoredGroup) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty()) throw PortalRequestFields.error("message.riftgun.name_empty");
        if (name.length() > ServerConfig.VALUES.maxGroupNameLength.get()) {
            throw PortalRequestFields.error("message.riftgun.name_too_long");
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.equals("default")) throw PortalRequestFields.error("message.riftgun.group_duplicate");
        boolean duplicate = data.groups().stream()
            .filter(group -> ignoredGroup == null || !group.id().equals(ignoredGroup))
            .anyMatch(group -> group.name().strip().toLowerCase(Locale.ROOT).equals(normalized));
        if (duplicate) throw PortalRequestFields.error("message.riftgun.group_duplicate");
        return name;
    }

    private static UUID validGroup(PortalPlayerData data, UUID groupId) {
        if (groupId == null || groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) {
            return PortalPlayerData.DEFAULT_GROUP_ID;
        }
        return data.group(groupId).isPresent() ? groupId : PortalPlayerData.DEFAULT_GROUP_ID;
    }

    private static void normalizeGroupOrder(PortalPlayerData data) {
        List<DestinationGroup> groups = new ArrayList<>(data.groups());
        groups.sort(Comparator.comparingInt(DestinationGroup::order));
        for (int index = 0; index < groups.size(); index++) {
            data.replaceGroup(groups.get(index).withOrder(index));
        }
    }

    private PortalDestinationActions() {}
}

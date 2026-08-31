package dev.riftgun.network;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.service.CoordinateParser;
import dev.riftgun.navigation.DimensionalTraversalTargets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Mutations for saved destinations and their shared group hierarchy. */
final class PortalDestinationActions {
    static boolean createCurrent(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
        requireDestinationCapacity(data);
        UUID group = validGroup(data, PortalRequestFields.optionalGroupId(request, "Group"));
        String name = destinationName(data, Nbt.getString(request, "Name"), true);
        long time = player.level().getGameTime();
        UUID destinationId = UUID.randomUUID();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        requireInWorldBounds(player, x, y, z);
        data.destinations().add(new Destination(
            destinationId, name, group, player.level().dimension(), x, y, z, player.getYRot(), time, 0L, false
        ));
        select(data, destinationId);
        return true;
    }

    private static void requireInWorldBounds(ServerPlayer player, double x, double y, double z) {
        requireInWorldBounds((ServerLevel) player.level(), x, y, z);
    }

    private static void requireInWorldBounds(ServerLevel level, double x, double y, double z) {
        if (!level.isInWorldBounds(new net.minecraft.core.BlockPos(
            net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(y), net.minecraft.util.Mth.floor(z)))) {
            throw PortalRequestFields.error("message.riftgun.coordinate_out_of_bounds");
        }
    }

    static boolean createCoordinate(ServerPlayer player, PortalPlayerData data,
                                    CompoundTag request, ItemStack gun) {
        if (!PortalGunCapabilities.resolve(gun, data.settings().smartDistance()).coordinateOverride()) {
            throw PortalRequestFields.error("message.riftgun.coordinate_module_required");
        }
        return createCoordinateDestination(player, data, request, (ServerLevel) player.level(),
            player.getX(), player.getY(), player.getZ(), false);
    }

    static boolean createDimensionalCoordinate(ServerPlayer player, PortalPlayerData data,
                                               CompoundTag request, ItemStack gun) {
        if (!RiftConfigs.server().dimensionalTraversal().enabled()) {
            throw PortalRequestFields.error("message.riftgun.dimensional_traversal_disabled");
        }
        if (!PortalGunCapabilities.resolve(gun, data.settings().smartDistance()).dimensionalTraversal()) {
            throw PortalRequestFields.error("message.riftgun.dimensional_traversal_module_required");
        }
        ServerLevel target = DimensionalTraversalTargets.resolve(
                player, Nbt.getString(request, "Dimension"))
            .orElseThrow(() -> PortalRequestFields.error("message.riftgun.dimension_unavailable"));
        double baseX = DimensionalTraversalTargets.mapCoordinate(player.getX(), player.level(), target);
        double baseZ = DimensionalTraversalTargets.mapCoordinate(player.getZ(), player.level(), target);
        return createCoordinateDestination(player, data, request, target,
            baseX, player.getY(), baseZ, true);
    }

    private static boolean createCoordinateDestination(
        ServerPlayer player, PortalPlayerData data, CompoundTag request, ServerLevel target,
        double baseX, double baseY, double baseZ, boolean blankCoordinatesAreRelative
    ) {
        requireDestinationCapacity(data);
        requireCoordinateLengths(request);
        UUID group = validGroup(data, PortalRequestFields.optionalGroupId(request, "Group"));
        String name = destinationName(data, Nbt.getString(request, "Name"), true);
        double x = CoordinateParser.parse(coordinate(request, "X", blankCoordinatesAreRelative), baseX);
        double y = CoordinateParser.parse(coordinate(request, "Y", blankCoordinatesAreRelative), baseY);
        double z = CoordinateParser.parse(coordinate(request, "Z", blankCoordinatesAreRelative), baseZ);
        float yaw = CoordinateParser.parseYaw(Nbt.getString(request, "Yaw"), player.getYRot());
        requireInWorldBounds(target, x, y, z);
        UUID destinationId = UUID.randomUUID();
        data.destinations().add(new Destination(
            destinationId, name, group, target.dimension(), x, y, z, yaw,
            target.getGameTime(), 0L, false));
        select(data, destinationId);
        return true;
    }

    private static String coordinate(CompoundTag request, String key, boolean blankIsRelative) {
        String value = Nbt.getString(request, key);
        return blankIsRelative && value.isBlank() ? "~" : value;
    }

    static boolean edit(ServerPlayer player, PortalPlayerData data,
                        CompoundTag request, ItemStack gun) {
        UUID destinationId = PortalRequestFields.id(request, "Destination");
        Destination current = data.destination(destinationId).orElseThrow(
            () -> PortalRequestFields.error("message.riftgun.destination_missing"));
        UUID requestedGroup = PortalRequestFields.optionalGroupId(request, "Group");
        UUID group = current.groupId().equals(PortalPlayerData.SHARED_SECTION_ID)
            && PortalPlayerData.SHARED_SECTION_ID.equals(requestedGroup)
            ? PortalPlayerData.SHARED_SECTION_ID : validGroup(data, requestedGroup);
        String name = destinationName(data, Nbt.getString(request, "Name"), false);
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun, data.settings().smartDistance());
        boolean coordinateOverride = capabilities.coordinateOverride()
            || capabilities.dimensionalTraversal()
                && RiftConfigs.server().dimensionalTraversal().enabled();
        if (coordinateOverride) requireCoordinateLengths(request);
        double x = coordinateOverride ? CoordinateParser.parse(Nbt.getString(request, "X"), player.getX()) : current.x();
        double y = coordinateOverride ? CoordinateParser.parse(Nbt.getString(request, "Y"), player.getY()) : current.y();
        double z = coordinateOverride ? CoordinateParser.parse(Nbt.getString(request, "Z"), player.getZ()) : current.z();
        float yaw = coordinateOverride
            ? CoordinateParser.parseYaw(Nbt.getString(request, "Yaw"), player.getYRot()) : current.yaw();
        if (coordinateOverride) {
//? if >=1.21.11 {
            /*ServerLevel target = player.level().getServer() == null ? null
                : player.level().getServer().getLevel(current.dimension());
*///?} else {
            ServerLevel target = player.getServer() == null ? null
                : player.getServer().getLevel(current.dimension());
//?}
            if (target == null) throw PortalRequestFields.error("message.riftgun.dimension_unavailable");
            requireInWorldBounds(target, x, y, z);
        }
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
        data.shareProvenance(destinationId, null);
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
        if (data.groups().size() >= RiftConfigs.server().destinations().maximumGroups()) {
            throw PortalRequestFields.error("message.riftgun.group_limit");
        }
        String name = groupName(data, Nbt.getString(request, "Name"), null);
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
        data.replaceGroup(group.withName(groupName(data, Nbt.getString(request, "Name"), groupId)));
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
            ? Math.clamp(Nbt.getInt(request, "TargetIndex"), 0, ordered.size() - 1)
            : Math.clamp(from + Math.clamp(Nbt.getInt(request, "Delta"), -1, 1), 0, ordered.size() - 1);
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
            && !groupId.equals(PortalPlayerData.SHARED_SECTION_ID)
            && !groupId.equals(PortalPlayerData.JOURNEYMAP_SECTION_ID)
            && !groupId.equals(PortalPlayerData.XAERO_MINIMAP_SECTION_ID)
            && data.group(groupId).isEmpty()) {
            throw PortalRequestFields.error("message.riftgun.group_missing");
        }
        if (Nbt.getBoolean(request, "Expanded")) data.expandedGroups().add(groupId);
        else data.expandedGroups().remove(groupId);
        return true;
    }

    private static void select(PortalPlayerData data, UUID destinationId) {
        // Selecting a destination always deselects the player target; without
        // this, creating a destination while a player was selected left the
        // stale player selection in place and every open went to the player.
        data.selectedPlayerId(null);
        data.selectedDestinationId(destinationId);
        data.lastViewedDestinationId(destinationId);
    }

    private static void requireDestinationCapacity(PortalPlayerData data) {
        if (data.destinations().size() >= RiftConfigs.server().destinations().maximumDestinations()) {
            throw PortalRequestFields.error("message.riftgun.destination_limit");
        }
    }

    private static void requireCoordinateLengths(CompoundTag request) {
        for (String key : List.of("X", "Y", "Z", "Yaw")) {
            if (Nbt.getString(request, key).length() > 64) {
                throw PortalRequestFields.error("message.riftgun.invalid_coordinate");
            }
        }
    }

    private static String destinationName(PortalPlayerData data, String raw, boolean allowDefault) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty() && allowDefault) name = data.nextLocationName();
        if (name.isEmpty()) throw PortalRequestFields.error("message.riftgun.name_empty");
        if (name.length() > RiftConfigs.server().destinations().maximumDestinationNameLength()) {
            throw PortalRequestFields.error("message.riftgun.name_too_long");
        }
        return name;
    }

    private static String groupName(PortalPlayerData data, String raw, UUID ignoredGroup) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty()) throw PortalRequestFields.error("message.riftgun.name_empty");
        if (name.length() > RiftConfigs.server().destinations().maximumGroupNameLength()) {
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

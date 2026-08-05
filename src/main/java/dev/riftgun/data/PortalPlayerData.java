package dev.riftgun.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public final class PortalPlayerData {
    public static final int CURRENT_VERSION = 3;
    public static final UUID DEFAULT_GROUP_ID = new UUID(0L, 0L);

    private final List<DestinationGroup> groups = new ArrayList<>();
    private final List<Destination> destinations = new ArrayList<>();
    private final Set<UUID> expandedGroups = new HashSet<>();
    private @Nullable UUID selectedDestinationId;
    private @Nullable UUID lastViewedDestinationId;
    private long nextLocationNumber = 1L;
    private PortalPlayerSettings settings = PortalPlayerSettings.defaults();

    public PortalPlayerData() {
        expandedGroups.add(DEFAULT_GROUP_ID);
    }

    public List<DestinationGroup> groups() {
        return groups;
    }

    public List<Destination> destinations() {
        return destinations;
    }

    public Set<UUID> expandedGroups() {
        return expandedGroups;
    }

    public @Nullable UUID selectedDestinationId() {
        return selectedDestinationId;
    }

    public void selectedDestinationId(@Nullable UUID id) {
        selectedDestinationId = id;
    }

    public @Nullable UUID lastViewedDestinationId() {
        return lastViewedDestinationId;
    }

    public void lastViewedDestinationId(@Nullable UUID id) {
        lastViewedDestinationId = id;
    }

    public PortalPlayerSettings settings() {
        return settings;
    }

    public void settings(PortalPlayerSettings value) {
        settings = value;
    }

    public Optional<Destination> destination(UUID id) {
        return destinations.stream().filter(destination -> destination.id().equals(id)).findFirst();
    }

    public Optional<DestinationGroup> group(UUID id) {
        return groups.stream().filter(group -> group.id().equals(id)).findFirst();
    }

    public String nextLocationName() {
        return "Location" + nextLocationNumber++;
    }

    public void replaceDestination(Destination replacement) {
        for (int index = 0; index < destinations.size(); index++) {
            if (destinations.get(index).id().equals(replacement.id())) {
                destinations.set(index, replacement);
                return;
            }
        }
    }

    public void replaceGroup(DestinationGroup replacement) {
        for (int index = 0; index < groups.size(); index++) {
            if (groups.get(index).id().equals(replacement.id())) {
                groups.set(index, replacement);
                return;
            }
        }
    }

    public CompoundTag save() {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", CURRENT_VERSION);
        root.putLong("NextLocationNumber", nextLocationNumber);
        if (selectedDestinationId != null) root.putUUID("Selected", selectedDestinationId);
        if (lastViewedDestinationId != null) root.putUUID("LastViewed", lastViewedDestinationId);
        root.put("Settings", settings.save());

        ListTag groupTags = new ListTag();
        groups.stream().sorted(Comparator.comparingInt(DestinationGroup::order)).forEach(group -> groupTags.add(group.save()));
        root.put("Groups", groupTags);

        ListTag destinationTags = new ListTag();
        destinations.forEach(destination -> destinationTags.add(destination.save()));
        root.put("Destinations", destinationTags);

        ListTag expandedTags = new ListTag();
        expandedGroups.forEach(id -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            expandedTags.add(tag);
        });
        root.put("ExpandedGroups", expandedTags);
        return root;
    }

    public static PortalPlayerData load(CompoundTag root) {
        PortalPlayerData data = new PortalPlayerData();
        data.nextLocationNumber = Math.max(1L, root.getLong("NextLocationNumber"));
        if (root.hasUUID("Selected")) data.selectedDestinationId = root.getUUID("Selected");
        if (root.hasUUID("LastViewed")) data.lastViewedDestinationId = root.getUUID("LastViewed");
        data.settings = PortalPlayerSettings.load(root.getCompound("Settings"));

        ListTag groups = root.getList("Groups", Tag.TAG_COMPOUND);
        groups.forEach(tag -> data.groups.add(DestinationGroup.load((CompoundTag) tag)));
        ListTag destinations = root.getList("Destinations", Tag.TAG_COMPOUND);
        destinations.forEach(tag -> data.destinations.add(Destination.load((CompoundTag) tag)));
        if (root.contains("ExpandedGroups")) {
            data.expandedGroups.clear();
            ListTag expanded = root.getList("ExpandedGroups", Tag.TAG_COMPOUND);
            expanded.forEach(tag -> {
                CompoundTag compound = (CompoundTag) tag;
                if (compound.hasUUID("Id")) data.expandedGroups.add(compound.getUUID("Id"));
            });
        }

        data.migrate(root.getInt("Version"));
        data.repairReferences();
        return data;
    }

    private void migrate(int storedVersion) {
        // Settings added in v2-v3 are backward-compatible through field defaults.
    }

    private void repairReferences() {
        Set<UUID> groupIds = new HashSet<>();
        groupIds.add(DEFAULT_GROUP_ID);
        groups.removeIf(group -> group.id().equals(DEFAULT_GROUP_ID) || !groupIds.add(group.id()));

        for (int index = 0; index < destinations.size(); index++) {
            Destination destination = destinations.get(index);
            if (!groupIds.contains(destination.groupId())) {
                destinations.set(index, destination.withGroup(DEFAULT_GROUP_ID));
            }
        }
        if (selectedDestinationId != null && destination(selectedDestinationId).isEmpty()) selectedDestinationId = null;
        if (lastViewedDestinationId != null && destination(lastViewedDestinationId).isEmpty()) lastViewedDestinationId = null;
        expandedGroups.retainAll(groupIds);
    }
}

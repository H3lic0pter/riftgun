package dev.riftgun.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public final class PortalPlayerData {
    public static final int CURRENT_VERSION = 6;
    public static final UUID DEFAULT_GROUP_ID = new UUID(0L, 0L);
    /** Sentinel id for the Player section's collapsed/expanded state in {@link #expandedGroups()}. */
    public static final UUID PLAYER_SECTION_ID = new UUID(0L, 0x100L);

    private final List<DestinationGroup> groups = new ArrayList<>();
    private final List<Destination> destinations = new ArrayList<>();
    private final Set<UUID> expandedGroups = new HashSet<>();
    private final Map<UUID, DestinationSafetyResult> safetyResults = new HashMap<>();
    private final Set<UUID> pinnedPlayers = new HashSet<>();
    private final Map<UUID, Long> playerLastUseAt = new HashMap<>();
    private final Map<UUID, PlayerPermissionOverride> privacyOverrides = new HashMap<>();
    private @Nullable UUID selectedDestinationId;
    private @Nullable UUID lastViewedDestinationId;
    private @Nullable UUID selectedPlayerId;
    private long nextLocationNumber = 1L;
    private PortalPlayerSettings settings = PortalPlayerSettings.defaults();
    private TargetPrivacy targetPrivacy = TargetPrivacy.PUBLIC;
    private boolean transitPrivacyEnabled;

    public PortalPlayerData() {
        expandedGroups.add(DEFAULT_GROUP_ID);
        expandedGroups.add(PLAYER_SECTION_ID);
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

    public @Nullable UUID selectedPlayerId() {
        return selectedPlayerId;
    }

    public void selectedPlayerId(@Nullable UUID id) {
        selectedPlayerId = id;
    }

    public PortalPlayerSettings settings() {
        return settings;
    }

    public void settings(PortalPlayerSettings value) {
        settings = value;
    }

    public TargetPrivacy targetPrivacy() {
        return targetPrivacy;
    }

    public void targetPrivacy(TargetPrivacy value) {
        targetPrivacy = value;
    }

    public boolean transitPrivacyEnabled() {
        return transitPrivacyEnabled;
    }

    public void transitPrivacyEnabled(boolean value) {
        transitPrivacyEnabled = value;
    }

    public PlayerPermissionOverride privacyOverride(UUID playerId) {
        return privacyOverrides.getOrDefault(playerId, PlayerPermissionOverride.DEFAULT);
    }

    public void privacyOverride(UUID playerId, PlayerPermissionOverride mode) {
        if (mode == PlayerPermissionOverride.DEFAULT) privacyOverrides.remove(playerId);
        else privacyOverrides.put(playerId, mode);
    }

    public Map<UUID, PlayerPermissionOverride> privacyOverrides() {
        return privacyOverrides;
    }

    public Optional<Destination> destination(UUID id) {
        return destinations.stream().filter(destination -> destination.id().equals(id)).findFirst();
    }

    public Optional<DestinationGroup> group(UUID id) {
        return groups.stream().filter(group -> group.id().equals(id)).findFirst();
    }

    public DestinationSafetyResult safetyResult(UUID destinationId) {
        return safetyResults.getOrDefault(destinationId, DestinationSafetyResult.UNKNOWN);
    }

    public void recordSafetyResult(UUID destinationId, boolean safe) {
        safetyResults.put(destinationId, safe ? DestinationSafetyResult.SAFE : DestinationSafetyResult.UNSAFE);
    }

    public void clearSafetyResult(UUID destinationId) {
        safetyResults.remove(destinationId);
    }

    public Set<UUID> pinnedPlayers() {
        return pinnedPlayers;
    }

    public boolean isPlayerPinned(UUID playerId) {
        return pinnedPlayers.contains(playerId);
    }

    public long playerLastUseAt(UUID playerId) {
        return playerLastUseAt.getOrDefault(playerId, 0L);
    }

    public void recordPlayerUse(UUID playerId, long time) {
        playerLastUseAt.put(playerId, time);
    }

    public String nextLocationName() {
        return "Location" + nextLocationNumber++;
    }

    public void replaceDestination(Destination replacement) {
        for (int index = 0; index < destinations.size(); index++) {
            Destination current = destinations.get(index);
            if (current.id().equals(replacement.id())) {
                if (!samePosition(current, replacement)) safetyResults.remove(replacement.id());
                destinations.set(index, replacement);
                return;
            }
        }
    }

    private static boolean samePosition(Destination first, Destination second) {
        return first.dimension().equals(second.dimension())
            && Double.compare(first.x(), second.x()) == 0
            && Double.compare(first.y(), second.y()) == 0
            && Double.compare(first.z(), second.z()) == 0;
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
        if (selectedPlayerId != null) root.putUUID("SelectedPlayer", selectedPlayerId);
        root.put("Settings", settings.save());
        root.putString("TargetPrivacy", targetPrivacy.name());
        root.putBoolean("TransitPrivacy", transitPrivacyEnabled);

        ListTag overrideTags = new ListTag();
        privacyOverrides.forEach((id, mode) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putString("Mode", mode.name());
            overrideTags.add(tag);
        });
        root.put("PrivacyOverrides", overrideTags);

        ListTag groupTags = new ListTag();
        groups.stream().sorted(Comparator.comparingInt(DestinationGroup::order)).forEach(group -> groupTags.add(group.save()));
        root.put("Groups", groupTags);

        ListTag destinationTags = new ListTag();
        destinations.forEach(destination -> destinationTags.add(destination.save()));
        root.put("Destinations", destinationTags);

        ListTag safetyTags = new ListTag();
        safetyResults.forEach((id, result) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putString("Result", result.name());
            safetyTags.add(tag);
        });
        root.put("SafetyResults", safetyTags);

        ListTag expandedTags = new ListTag();
        expandedGroups.forEach(id -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            expandedTags.add(tag);
        });
        root.put("ExpandedGroups", expandedTags);

        ListTag pinnedTags = new ListTag();
        pinnedPlayers.forEach(id -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            pinnedTags.add(tag);
        });
        root.put("PinnedPlayers", pinnedTags);

        ListTag lastUseTags = new ListTag();
        playerLastUseAt.forEach((id, time) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putLong("Time", time);
            lastUseTags.add(tag);
        });
        root.put("PlayerLastUse", lastUseTags);
        return root;
    }

    public static PortalPlayerData load(CompoundTag root) {
        return load(root, TargetPrivacy.PUBLIC);
    }

    public static PortalPlayerData load(CompoundTag root, TargetPrivacy defaultTargetPrivacy) {
        PortalPlayerData data = new PortalPlayerData();
        data.nextLocationNumber = Math.max(1L, root.getLong("NextLocationNumber"));
        if (root.hasUUID("Selected")) data.selectedDestinationId = root.getUUID("Selected");
        if (root.hasUUID("LastViewed")) data.lastViewedDestinationId = root.getUUID("LastViewed");
        if (root.hasUUID("SelectedPlayer")) data.selectedPlayerId = root.getUUID("SelectedPlayer");
        data.settings = PortalPlayerSettings.load(root.getCompound("Settings"));
        data.targetPrivacy = root.contains("TargetPrivacy", Tag.TAG_STRING)
            ? TargetPrivacy.parse(root.getString("TargetPrivacy"), defaultTargetPrivacy)
            : defaultTargetPrivacy;
        data.transitPrivacyEnabled = root.getBoolean("TransitPrivacy");
        if (root.contains("PrivacyOverrides")) {
            ListTag overrides = root.getList("PrivacyOverrides", Tag.TAG_COMPOUND);
            overrides.forEach(tag -> {
                CompoundTag compound = (CompoundTag) tag;
                if (!compound.hasUUID("Id")) return;
                PlayerPermissionOverride mode = PlayerPermissionOverride.parse(
                    compound.getString("Mode"), PlayerPermissionOverride.DEFAULT);
                if (mode != PlayerPermissionOverride.DEFAULT) {
                    data.privacyOverrides.put(compound.getUUID("Id"), mode);
                }
            });
        }

        ListTag groups = root.getList("Groups", Tag.TAG_COMPOUND);
        groups.forEach(tag -> data.groups.add(DestinationGroup.load((CompoundTag) tag)));
        ListTag destinations = root.getList("Destinations", Tag.TAG_COMPOUND);
        destinations.forEach(tag -> data.destinations.add(Destination.load((CompoundTag) tag)));
        ListTag safetyResults = root.getList("SafetyResults", Tag.TAG_COMPOUND);
        safetyResults.forEach(tag -> {
            CompoundTag compound = (CompoundTag) tag;
            if (!compound.hasUUID("Id")) return;
            DestinationSafetyResult result = DestinationSafetyResult.parse(compound.getString("Result"));
            if (result != DestinationSafetyResult.UNKNOWN) {
                data.safetyResults.put(compound.getUUID("Id"), result);
            }
        });
        if (root.contains("ExpandedGroups")) {
            data.expandedGroups.clear();
            ListTag expanded = root.getList("ExpandedGroups", Tag.TAG_COMPOUND);
            expanded.forEach(tag -> {
                CompoundTag compound = (CompoundTag) tag;
                if (compound.hasUUID("Id")) data.expandedGroups.add(compound.getUUID("Id"));
            });
        }
        if (root.contains("PinnedPlayers")) {
            ListTag pinned = root.getList("PinnedPlayers", Tag.TAG_COMPOUND);
            pinned.forEach(tag -> {
                CompoundTag compound = (CompoundTag) tag;
                if (compound.hasUUID("Id")) data.pinnedPlayers.add(compound.getUUID("Id"));
            });
        }
        if (root.contains("PlayerLastUse")) {
            ListTag lastUse = root.getList("PlayerLastUse", Tag.TAG_COMPOUND);
            lastUse.forEach(tag -> {
                CompoundTag compound = (CompoundTag) tag;
                if (compound.hasUUID("Id")) data.playerLastUseAt.put(compound.getUUID("Id"), compound.getLong("Time"));
            });
        }

        data.migrate(root.getInt("Version"));
        data.repairReferences();
        return data;
    }

    private void migrate(int storedVersion) {
        // Settings and v4 safety history are backward-compatible through missing-field defaults.
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
        safetyResults.keySet().removeIf(id -> destination(id).isEmpty());
        expandedGroups.removeIf(id -> !groupIds.contains(id) && !id.equals(PLAYER_SECTION_ID));
    }

    /** Clears pinned flags, use timestamps, and selection for players that are no longer online. */
    public void prunePlayerTargets(Set<UUID> onlinePlayers) {
        pinnedPlayers.removeIf(id -> !onlinePlayers.contains(id));
        playerLastUseAt.keySet().removeIf(id -> !onlinePlayers.contains(id));
        if (selectedPlayerId != null && !onlinePlayers.contains(selectedPlayerId)) selectedPlayerId = null;
    }
}

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
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class PortalPlayerData {
    public static final int CURRENT_VERSION = 8;
    public static final UUID DEFAULT_GROUP_ID = new UUID(0L, 0L);
    /** Sentinel id for the Player section's collapsed/expanded state in {@link #expandedGroups()}. */
    public static final UUID PLAYER_SECTION_ID = new UUID(0L, 0x100L);

    private final List<DestinationGroup> groups = new ArrayList<>();
    private final List<Destination> destinations = new ArrayList<>();
    private final Set<UUID> expandedGroups = new HashSet<>();
    private final Map<UUID, DestinationSafetyResult> safetyResults = new HashMap<>();
    private final Set<UUID> pinnedPlayers = new HashSet<>();
    private final Map<UUID, Long> playerLastUseAt = new HashMap<>();
    private final Map<ResourceLocation, PortalPermissionPolicy> globalPermissions = new HashMap<>();
    private final Map<UUID, PlayerPermissionProfile> permissionProfiles = new HashMap<>();
    private @Nullable UUID selectedDestinationId;
    private @Nullable UUID lastViewedDestinationId;
    private @Nullable UUID selectedPlayerId;
    private long nextLocationNumber = 1L;
    private PortalPlayerSettings settings = PortalPlayerSettings.defaults();

    public PortalPlayerData() {
        expandedGroups.add(DEFAULT_GROUP_ID);
        expandedGroups.add(PLAYER_SECTION_ID);
        PortalPermissions.definitions().forEach(definition ->
            globalPermissions.put(definition.id(), definition.fallbackGlobalPolicy()));
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
        return switch (globalPermission(PortalPermissions.PLAYER_PORTAL)) {
            case ASK -> TargetPrivacy.REQUEST;
            case DENY -> TargetPrivacy.PRIVATE;
            default -> TargetPrivacy.PUBLIC;
        };
    }

    public void targetPrivacy(TargetPrivacy value) {
        globalPermission(PortalPermissions.PLAYER_PORTAL, switch (value) {
            case PUBLIC -> PortalPermissionPolicy.ALLOW;
            case REQUEST -> PortalPermissionPolicy.ASK;
            case PRIVATE -> PortalPermissionPolicy.DENY;
        });
    }

    public boolean transitPrivacyEnabled() {
        return globalPermission(PortalPermissions.FOREIGN_EXIT_TRANSIT) == PortalPermissionPolicy.DENY;
    }

    public void transitPrivacyEnabled(boolean value) {
        globalPermission(PortalPermissions.FOREIGN_EXIT_TRANSIT,
            value ? PortalPermissionPolicy.DENY : PortalPermissionPolicy.ALLOW);
    }

    public PortalPermissionPolicy globalPermission(ResourceLocation permissionId) {
        PortalPermissionDefinition definition = PortalPermissions.definition(permissionId);
        PortalPermissionPolicy fallback = definition == null
            ? PortalPermissionPolicy.DENY : definition.fallbackGlobalPolicy();
        return globalPermissions.getOrDefault(permissionId, fallback);
    }

    public void globalPermission(ResourceLocation permissionId, PortalPermissionPolicy policy) {
        PortalPermissionDefinition definition = PortalPermissions.definition(permissionId);
        if (policy == null || policy == PortalPermissionPolicy.FOLLOW_GLOBAL
            || definition != null && !definition.supportsAsk() && policy == PortalPermissionPolicy.ASK) return;
        globalPermissions.put(permissionId, policy);
    }

    public Map<ResourceLocation, PortalPermissionPolicy> globalPermissions() {
        return globalPermissions;
    }

    public PlayerPermissionProfile permissionProfile(UUID playerId) {
        return permissionProfiles.computeIfAbsent(playerId, ignored -> new PlayerPermissionProfile());
    }

    public void permissionProfileMode(UUID playerId, PlayerPermissionProfileMode mode) {
        if (mode == PlayerPermissionProfileMode.FOLLOW_GLOBAL) {
            permissionProfiles.remove(playerId);
            return;
        }
        permissionProfile(playerId).mode(mode);
    }

    public Map<UUID, PlayerPermissionProfile> permissionProfiles() {
        return permissionProfiles;
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
        ListTag globalPermissionTags = new ListTag();
        globalPermissions.forEach((id, policy) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", id.toString());
            tag.putString("Policy", policy.name());
            globalPermissionTags.add(tag);
        });
        root.put("GlobalPermissions", globalPermissionTags);

        ListTag profileTags = new ListTag();
        permissionProfiles.forEach((playerId, profile) -> {
            CompoundTag profileTag = new CompoundTag();
            profileTag.putUUID("Player", playerId);
            profileTag.putString("Mode", profile.mode().name());
            ListTag values = new ListTag();
            profile.values().forEach((permissionId, policy) -> {
                CompoundTag value = new CompoundTag();
                value.putString("Id", permissionId.toString());
                value.putString("Policy", policy.name());
                values.add(value);
            });
            profileTag.put("Values", values);
            profileTags.add(profileTag);
        });
        root.put("PermissionProfiles", profileTags);

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
        return load(root, Map.of());
    }

    public static PortalPlayerData load(CompoundTag root, TargetPrivacy defaultTargetPrivacy) {
        return load(root, Map.of(PortalPermissions.PLAYER_PORTAL, switch (defaultTargetPrivacy) {
            case PUBLIC -> PortalPermissionPolicy.ALLOW;
            case REQUEST -> PortalPermissionPolicy.ASK;
            case PRIVATE -> PortalPermissionPolicy.DENY;
        }));
    }

    public static PortalPlayerData load(
            CompoundTag root, Map<ResourceLocation, PortalPermissionPolicy> permissionDefaults) {
        PortalPlayerData data = new PortalPlayerData();
        permissionDefaults.forEach(data::globalPermission);
        data.nextLocationNumber = Math.max(1L, root.getLong("NextLocationNumber"));
        if (root.hasUUID("Selected")) data.selectedDestinationId = root.getUUID("Selected");
        if (root.hasUUID("LastViewed")) data.lastViewedDestinationId = root.getUUID("LastViewed");
        if (root.hasUUID("SelectedPlayer")) data.selectedPlayerId = root.getUUID("SelectedPlayer");
        data.settings = PortalPlayerSettings.load(root.getCompound("Settings"));
        boolean structuredPermissions = root.contains("GlobalPermissions", Tag.TAG_LIST);
        if (structuredPermissions) {
            ListTag globals = root.getList("GlobalPermissions", Tag.TAG_COMPOUND);
            globals.forEach(raw -> {
                CompoundTag tag = (CompoundTag) raw;
                ResourceLocation id = ResourceLocation.tryParse(tag.getString("Id"));
                PortalPermissionPolicy policy = PortalPermissionPolicy.parse(
                    tag.getString("Policy"), PortalPermissionPolicy.FOLLOW_GLOBAL);
                if (id != null && policy != PortalPermissionPolicy.FOLLOW_GLOBAL) {
                    PortalPermissionDefinition definition = PortalPermissions.definition(id);
                    if (definition == null) data.globalPermissions.put(id, policy);
                    else data.globalPermission(id, policy);
                }
            });
        } else {
            if (root.contains("TargetPrivacy", Tag.TAG_STRING)) {
                data.targetPrivacy(TargetPrivacy.parse(root.getString("TargetPrivacy"), data.targetPrivacy()));
            }
            data.transitPrivacyEnabled(root.getBoolean("TransitPrivacy"));
        }
        if (root.contains("PermissionProfiles", Tag.TAG_LIST)) {
            ListTag profiles = root.getList("PermissionProfiles", Tag.TAG_COMPOUND);
            profiles.forEach(raw -> {
                CompoundTag tag = (CompoundTag) raw;
                if (!tag.hasUUID("Player")) return;
                PlayerPermissionProfile profile = new PlayerPermissionProfile();
                PlayerPermissionProfileMode mode = PlayerPermissionProfileMode.parse(
                    tag.getString("Mode"), PlayerPermissionProfileMode.FOLLOW_GLOBAL);
                profile.mode(mode);
                ListTag values = tag.getList("Values", Tag.TAG_COMPOUND);
                values.forEach(valueRaw -> {
                    CompoundTag value = (CompoundTag) valueRaw;
                    ResourceLocation id = ResourceLocation.tryParse(value.getString("Id"));
                    PortalPermissionPolicy policy = PortalPermissionPolicy.parse(
                        value.getString("Policy"), PortalPermissionPolicy.FOLLOW_GLOBAL);
                    if (id != null && policy != PortalPermissionPolicy.FOLLOW_GLOBAL) {
                        profile.values().put(id, policy);
                    }
                });
                if (mode != PlayerPermissionProfileMode.FOLLOW_GLOBAL) {
                    data.permissionProfiles.put(tag.getUUID("Player"), profile);
                }
            });
        } else if (root.contains("PrivacyOverrides")) {
            ListTag overrides = root.getList("PrivacyOverrides", Tag.TAG_COMPOUND);
            overrides.forEach(tag -> {
                CompoundTag compound = (CompoundTag) tag;
                if (!compound.hasUUID("Id")) return;
                PlayerPermissionOverride mode = PlayerPermissionOverride.parse(
                    compound.getString("Mode"), PlayerPermissionOverride.DEFAULT);
                if (mode != PlayerPermissionOverride.DEFAULT) {
                    PlayerPermissionProfile profile = data.permissionProfile(compound.getUUID("Id"));
                    PortalPermissionPolicy policy = mode == PlayerPermissionOverride.ALLOW
                        ? PortalPermissionPolicy.ALLOW : PortalPermissionPolicy.DENY;
                    profile.customize(PortalPermissions.PLAYER_PORTAL, policy);
                    profile.customize(PortalPermissions.ENTITY_RELOCATION_SUBJECT, policy);
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
        // Settings, sound themes, and v4 safety history use missing-field defaults.
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

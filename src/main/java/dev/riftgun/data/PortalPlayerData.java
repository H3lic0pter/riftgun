package dev.riftgun.data;
import dev.riftgun.core.nbt.Nbt;

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
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import org.jetbrains.annotations.Nullable;

public final class PortalPlayerData {
    public static final int CURRENT_VERSION = 10;
    public static final UUID DEFAULT_GROUP_ID = new UUID(0L, 0L);
    /** Sentinel id for the Player section's collapsed/expanded state in {@link #expandedGroups()}. */
    public static final UUID PLAYER_SECTION_ID = new UUID(0L, 0x100L);
    /** Virtual group shown only while imported destinations exist. */
    public static final UUID SHARED_SECTION_ID = new UUID(0L, 0x200L);
    /** Sentinel ids for client-only map sections; their expansion follows Player persistence. */
    public static final UUID JOURNEYMAP_SECTION_ID = new UUID(0L, 0x300L);
    public static final UUID XAERO_MINIMAP_SECTION_ID = new UUID(0L, 0x400L);

    private final List<DestinationGroup> groups = new ArrayList<>();
    private final List<Destination> destinations = new ArrayList<>();
    private final Set<UUID> expandedGroups = new HashSet<>();
    private final Map<UUID, DestinationSafetyResult> safetyResults = new HashMap<>();
    private final Set<UUID> pinnedPlayers = new HashSet<>();
    private final Map<UUID, Long> playerLastUseAt = new HashMap<>();
    private final Map<UUID, ShareProvenance> shareProvenance = new HashMap<>();
//? if >=1.21.11 {
    /*private final Map<Identifier, PortalPermissionPolicy> globalPermissions = new HashMap<>();
*///?} else {
    private final Map<ResourceLocation, PortalPermissionPolicy> globalPermissions = new HashMap<>();
//?}
    private final Map<UUID, PlayerPermissionProfile> permissionProfiles = new HashMap<>();
    private @Nullable UUID selectedDestinationId;
    private @Nullable UUID lastViewedDestinationId;
    private @Nullable UUID selectedPlayerId;
    private long nextLocationNumber = 1L;
    private PortalPlayerSettings settings = PortalPlayerSettings.defaults();

    public PortalPlayerData() {
        expandedGroups.add(DEFAULT_GROUP_ID);
        expandedGroups.add(PLAYER_SECTION_ID);
        expandedGroups.add(SHARED_SECTION_ID);
        expandedGroups.add(JOURNEYMAP_SECTION_ID);
        expandedGroups.add(XAERO_MINIMAP_SECTION_ID);
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

//? if >=1.21.11 {
    /*public PortalPermissionPolicy globalPermission(Identifier permissionId) {
*///?} else {
    public PortalPermissionPolicy globalPermission(ResourceLocation permissionId) {
//?}
        PortalPermissionDefinition definition = PortalPermissions.definition(permissionId);
        PortalPermissionPolicy fallback = definition == null
            ? PortalPermissionPolicy.DENY : definition.fallbackGlobalPolicy();
        return globalPermissions.getOrDefault(permissionId, fallback);
    }

//? if >=1.21.11 {
    /*public void globalPermission(Identifier permissionId, PortalPermissionPolicy policy) {
*///?} else {
    public void globalPermission(ResourceLocation permissionId, PortalPermissionPolicy policy) {
//?}
        PortalPermissionDefinition definition = PortalPermissions.definition(permissionId);
        if (policy == null || policy == PortalPermissionPolicy.FOLLOW_GLOBAL
            || definition != null && !definition.supportsAsk() && policy == PortalPermissionPolicy.ASK) return;
        globalPermissions.put(permissionId, policy);
    }

//? if >=1.21.11 {
    /*public Map<Identifier, PortalPermissionPolicy> globalPermissions() {
*///?} else {
    public Map<ResourceLocation, PortalPermissionPolicy> globalPermissions() {
//?}
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

    public Optional<ShareProvenance> shareProvenance(UUID destinationId) {
        return Optional.ofNullable(shareProvenance.get(destinationId));
    }

    public void shareProvenance(UUID destinationId, ShareProvenance provenance) {
        if (provenance == null) shareProvenance.remove(destinationId);
        else shareProvenance.put(destinationId, provenance);
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
        if (selectedDestinationId != null) Nbt.putUUID(root, "Selected", selectedDestinationId);
        if (lastViewedDestinationId != null) Nbt.putUUID(root, "LastViewed", lastViewedDestinationId);
        if (selectedPlayerId != null) Nbt.putUUID(root, "SelectedPlayer", selectedPlayerId);
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
            Nbt.putUUID(profileTag, "Player", playerId);
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

        ListTag provenanceTags = new ListTag();
        shareProvenance.forEach((id, provenance) -> provenanceTags.add(provenance.save(id)));
        root.put("ShareProvenance", provenanceTags);

        ListTag safetyTags = new ListTag();
        safetyResults.forEach((id, result) -> {
            CompoundTag tag = new CompoundTag();
            Nbt.putUUID(tag, "Id", id);
            tag.putString("Result", result.name());
            safetyTags.add(tag);
        });
        root.put("SafetyResults", safetyTags);

        ListTag expandedTags = new ListTag();
        expandedGroups.forEach(id -> {
            CompoundTag tag = new CompoundTag();
            Nbt.putUUID(tag, "Id", id);
            expandedTags.add(tag);
        });
        root.put("ExpandedGroups", expandedTags);

        ListTag pinnedTags = new ListTag();
        pinnedPlayers.forEach(id -> {
            CompoundTag tag = new CompoundTag();
            Nbt.putUUID(tag, "Id", id);
            pinnedTags.add(tag);
        });
        root.put("PinnedPlayers", pinnedTags);

        ListTag lastUseTags = new ListTag();
        playerLastUseAt.forEach((id, time) -> {
            CompoundTag tag = new CompoundTag();
            Nbt.putUUID(tag, "Id", id);
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
//? if >=1.21.11 {
            /*CompoundTag root, Map<Identifier, PortalPermissionPolicy> permissionDefaults) {
*///?} else {
            CompoundTag root, Map<ResourceLocation, PortalPermissionPolicy> permissionDefaults) {
//?}
        PortalPlayerData data = new PortalPlayerData();
        permissionDefaults.forEach(data::globalPermission);
        data.nextLocationNumber = Math.max(1L, Nbt.getLong(root, "NextLocationNumber"));
        if (Nbt.hasUUID(root, "Selected")) data.selectedDestinationId = Nbt.getUUID(root, "Selected");
        if (Nbt.hasUUID(root, "LastViewed")) data.lastViewedDestinationId = Nbt.getUUID(root, "LastViewed");
        if (Nbt.hasUUID(root, "SelectedPlayer")) data.selectedPlayerId = Nbt.getUUID(root, "SelectedPlayer");
        data.settings = PortalPlayerSettings.load(Nbt.getCompound(root, "Settings"));
        boolean structuredPermissions = Nbt.contains(root, "GlobalPermissions", Tag.TAG_LIST);
        if (structuredPermissions) {
            ListTag globals = Nbt.getList(root, "GlobalPermissions");
            globals.forEach(raw -> {
                CompoundTag tag = (CompoundTag) raw;
//? if >=1.21.11 {
                /*Identifier id = Identifier.tryParse(Nbt.getString(tag, "Id"));
*///?} else {
                ResourceLocation id = ResourceLocation.tryParse(Nbt.getString(tag, "Id"));
//?}
                PortalPermissionPolicy policy = PortalPermissionPolicy.parse(
                    Nbt.getString(tag, "Policy"), PortalPermissionPolicy.FOLLOW_GLOBAL);
                if (id != null && policy != PortalPermissionPolicy.FOLLOW_GLOBAL) {
                    PortalPermissionDefinition definition = PortalPermissions.definition(id);
                    if (definition == null) data.globalPermissions.put(id, policy);
                    else data.globalPermission(id, policy);
                }
            });
        } else {
            if (Nbt.contains(root, "TargetPrivacy", Tag.TAG_STRING)) {
                data.targetPrivacy(TargetPrivacy.parse(Nbt.getString(root, "TargetPrivacy"), data.targetPrivacy()));
            }
            data.transitPrivacyEnabled(Nbt.getBoolean(root, "TransitPrivacy"));
        }
        if (Nbt.contains(root, "PermissionProfiles", Tag.TAG_LIST)) {
            ListTag profiles = Nbt.getList(root, "PermissionProfiles");
            profiles.forEach(raw -> {
                CompoundTag tag = (CompoundTag) raw;
                if (!Nbt.hasUUID(tag, "Player")) return;
                PlayerPermissionProfile profile = new PlayerPermissionProfile();
                PlayerPermissionProfileMode mode = PlayerPermissionProfileMode.parse(
                    Nbt.getString(tag, "Mode"), PlayerPermissionProfileMode.FOLLOW_GLOBAL);
                profile.mode(mode);
                ListTag values = Nbt.getList(tag, "Values");
                values.forEach(valueRaw -> {
                    CompoundTag value = (CompoundTag) valueRaw;
//? if >=1.21.11 {
                    /*Identifier id = Identifier.tryParse(Nbt.getString(value, "Id"));
*///?} else {
                    ResourceLocation id = ResourceLocation.tryParse(Nbt.getString(value, "Id"));
//?}
                    PortalPermissionPolicy policy = PortalPermissionPolicy.parse(
                        Nbt.getString(value, "Policy"), PortalPermissionPolicy.FOLLOW_GLOBAL);
                    if (id != null && policy != PortalPermissionPolicy.FOLLOW_GLOBAL) {
                        profile.values().put(id, policy);
                    }
                });
                if (mode != PlayerPermissionProfileMode.FOLLOW_GLOBAL) {
                    data.permissionProfiles.put(Nbt.getUUID(tag, "Player"), profile);
                }
            });
        } else if (root.contains("PrivacyOverrides")) {
            ListTag overrides = Nbt.getList(root, "PrivacyOverrides");
            overrides.forEach(tag -> {
                CompoundTag compound = (CompoundTag) tag;
                if (!Nbt.hasUUID(compound, "Id")) return;
                PlayerPermissionOverride mode = PlayerPermissionOverride.parse(
                    Nbt.getString(compound, "Mode"), PlayerPermissionOverride.DEFAULT);
                if (mode != PlayerPermissionOverride.DEFAULT) {
                    PlayerPermissionProfile profile = data.permissionProfile(Nbt.getUUID(compound, "Id"));
                    PortalPermissionPolicy policy = mode == PlayerPermissionOverride.ALLOW
                        ? PortalPermissionPolicy.ALLOW : PortalPermissionPolicy.DENY;
                    profile.customize(PortalPermissions.PLAYER_PORTAL, policy);
                    profile.customize(PortalPermissions.ENTITY_RELOCATION_SUBJECT, policy);
                }
            });
        }

        ListTag groups = Nbt.getList(root, "Groups");
        groups.forEach(tag -> data.groups.add(DestinationGroup.load((CompoundTag) tag)));
        ListTag destinations = Nbt.getList(root, "Destinations");
        destinations.forEach(tag -> data.destinations.add(Destination.load((CompoundTag) tag)));
        ListTag provenance = Nbt.getList(root, "ShareProvenance");
        provenance.forEach(raw -> {
            CompoundTag tag = (CompoundTag) raw;
            if (!Nbt.hasUUID(tag, "Destination")) return;
            ShareProvenance value = ShareProvenance.load(tag);
            if (value != null) data.shareProvenance.put(Nbt.getUUID(tag, "Destination"), value);
        });
        ListTag safetyResults = Nbt.getList(root, "SafetyResults");
        safetyResults.forEach(tag -> {
            CompoundTag compound = (CompoundTag) tag;
            if (!Nbt.hasUUID(compound, "Id")) return;
            DestinationSafetyResult result = DestinationSafetyResult.parse(Nbt.getString(compound, "Result"));
            if (result != DestinationSafetyResult.UNKNOWN) {
                data.safetyResults.put(Nbt.getUUID(compound, "Id"), result);
            }
        });
        if (root.contains("ExpandedGroups")) {
            data.expandedGroups.clear();
            ListTag expanded = Nbt.getList(root, "ExpandedGroups");
            expanded.forEach(tag -> {
                CompoundTag compound = (CompoundTag) tag;
                if (Nbt.hasUUID(compound, "Id")) data.expandedGroups.add(Nbt.getUUID(compound, "Id"));
            });
        }
        if (root.contains("PinnedPlayers")) {
            ListTag pinned = Nbt.getList(root, "PinnedPlayers");
            pinned.forEach(tag -> {
                CompoundTag compound = (CompoundTag) tag;
                if (Nbt.hasUUID(compound, "Id")) data.pinnedPlayers.add(Nbt.getUUID(compound, "Id"));
            });
        }
        if (root.contains("PlayerLastUse")) {
            ListTag lastUse = Nbt.getList(root, "PlayerLastUse");
            lastUse.forEach(tag -> {
                CompoundTag compound = (CompoundTag) tag;
                if (Nbt.hasUUID(compound, "Id")) data.playerLastUseAt.put(Nbt.getUUID(compound, "Id"), Nbt.getLong(compound, "Time"));
            });
        }

        data.migrate(Nbt.getInt(root, "Version"));
        data.repairReferences();
        return data;
    }

    private void migrate(int storedVersion) {
        // Settings, sound themes, and v4 safety history use missing-field defaults.
        if (storedVersion < 10) {
            expandedGroups.add(JOURNEYMAP_SECTION_ID);
            expandedGroups.add(XAERO_MINIMAP_SECTION_ID);
        }
    }

    private void repairReferences() {
        Set<UUID> groupIds = new HashSet<>();
        groupIds.add(DEFAULT_GROUP_ID);
        groupIds.add(SHARED_SECTION_ID);
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
        shareProvenance.keySet().removeIf(id -> destination(id).isEmpty());
        expandedGroups.removeIf(id -> !groupIds.contains(id)
            && !id.equals(PLAYER_SECTION_ID)
            && !id.equals(JOURNEYMAP_SECTION_ID)
            && !id.equals(XAERO_MINIMAP_SECTION_ID));
    }

    /** Clears pinned flags, use timestamps, and selection for players that are no longer online. */
    public void prunePlayerTargets(Set<UUID> onlinePlayers) {
        pinnedPlayers.removeIf(id -> !onlinePlayers.contains(id));
        playerLastUseAt.keySet().removeIf(id -> !onlinePlayers.contains(id));
        if (selectedPlayerId != null && !onlinePlayers.contains(selectedPlayerId)) selectedPlayerId = null;
    }
}

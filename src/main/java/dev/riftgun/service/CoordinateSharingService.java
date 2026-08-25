package dev.riftgun.service;

import dev.riftgun.RiftGun;
import dev.riftgun.api.CoordinateNoteRequest;
import dev.riftgun.api.CoordinateNoteResult;
import dev.riftgun.api.CoordinateNoteStatus;
import dev.riftgun.api.RiftGunDimensionLabels;
import dev.riftgun.api.RiftResourceId;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.msg.Msg;
import dev.riftgun.data.CoordinateSnapshot;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.ShareProvenance;
import dev.riftgun.fuel.PortalGunComponents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/** Server authority for chat tokens, coordinate notes, and all import validation. */
public final class CoordinateSharingService {
    private static final Map<UUID, ChatShare> CHAT_SHARES = new HashMap<>();
    private static final Map<UUID, Long> CHAT_COOLDOWNS = new HashMap<>();

    public enum Result {
        SUCCESS("message.riftgun.coordinate_imported"),
        DISABLED("message.riftgun.coordinate_sharing_disabled"),
        MISSING("message.riftgun.destination_missing"),
        DIMENSION_UNAVAILABLE("message.riftgun.dimension_unavailable"),
        EXPIRED("message.riftgun.coordinate_share_expired"),
        ALREADY_IMPORTED("message.riftgun.coordinate_already_imported"),
        LIMIT("message.riftgun.destination_limit"),
        COOLDOWN("message.riftgun.coordinate_share_cooldown"),
        PAPER_REQUIRED("message.riftgun.coordinate_paper_required"),
        INVENTORY_FULL("message.riftgun.inventory_full"),
        INVALID("message.riftgun.invalid_request");

        private final String key;

        Result(String key) { this.key = key; }
        public String key() { return key; }
    }

    public static Result shareToChat(ServerPlayer player, UUID destinationId) {
        if (!RiftConfigs.server().coordinateSharing().enabled()) return tell(player, Result.DISABLED);
        PortalPlayerData data = PortalDataStore.load(player);
        Destination destination = data.destination(destinationId).orElse(null);
        if (destination == null) return tell(player, Result.MISSING);
        MinecraftServer server = server(player);
        if (server == null || server.getLevel(destination.dimension()) == null) {
            return tell(player, Result.DIMENSION_UNAVAILABLE);
        }
        long now = server.getTickCount();
        int cooldownTicks = RiftConfigs.server().coordinateSharing().chatCooldownSeconds() * 20;
        long allowedAt = CHAT_COOLDOWNS.getOrDefault(player.getUUID(), 0L);
        if (now < allowedAt) return tell(player, Result.COOLDOWN);

        CoordinateSnapshot snapshot = snapshot(player, data, destination);
        long expiresAt = now + RiftConfigs.server().coordinateSharing().chatExpirySeconds() * 20L;
        ChatShare share = new ChatShare(snapshot, expiresAt, new HashSet<>());
        share.importedPlayers().add(player.getUUID());
        CHAT_SHARES.put(snapshot.snapshotId(), share);
        CHAT_COOLDOWNS.put(player.getUUID(), now + cooldownTicks);
        prune(now);

        Component message = Component.translatable("chat.riftgun.coordinate_share",
            player.getDisplayName(), dimensionName(player, destination), destination.name())
            .append("  ").append(clickAction(player, snapshot));
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            Msg.displayClientMessage(target, message, false);
        }
        return Result.SUCCESS;
    }

    public static Result createNote(ServerPlayer player, UUID destinationId) {
        if (!RiftConfigs.server().coordinateSharing().enabled()) return tell(player, Result.DISABLED);
        PortalPlayerData data = PortalDataStore.load(player);
        Destination destination = data.destination(destinationId).orElse(null);
        if (destination == null) return tell(player, Result.MISSING);
        MinecraftServer server = server(player);
        if (server == null || server.getLevel(destination.dimension()) == null) {
            return tell(player, Result.DIMENSION_UNAVAILABLE);
        }
        Result result = createNoteItem(player, snapshot(player, data, destination));
        if (result == Result.SUCCESS) {
            Msg.displayClientMessage(player,
                Component.translatable("message.riftgun.coordinate_note_created"), true);
            return result;
        }
        return tell(player, result);
    }

    /** Public-API implementation for addon-owned destinations. */
    public static CoordinateNoteResult createExternalNote(CoordinateNoteRequest request) {
        ServerPlayer player = request.player();
        if (!RiftConfigs.server().coordinateSharing().enabled()) {
            return apiResult(CoordinateNoteStatus.SHARING_DISABLED, Result.DISABLED);
        }
        MinecraftServer server = server(player);
        if (server == null) return apiResult(CoordinateNoteStatus.TARGET_DIMENSION_UNAVAILABLE,
            Result.DIMENSION_UNAVAILABLE);
//? if >=1.21.11 {
        /*var dimensionId = net.minecraft.resources.Identifier.tryParse(
            request.destination().dimensionId().toString());
*///?} else {
        var dimensionId = net.minecraft.resources.ResourceLocation.tryParse(
            request.destination().dimensionId().toString());
//?}
        if (dimensionId == null) return apiResult(CoordinateNoteStatus.INVALID_REQUEST, Result.INVALID);
        var dimension = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, dimensionId);
        if (server.getLevel(dimension) == null) {
            return apiResult(CoordinateNoteStatus.TARGET_DIMENSION_UNAVAILABLE,
                Result.DIMENSION_UNAVAILABLE);
        }
        String name = trim(request.displayName().getString(),
            RiftConfigs.server().destinations().maximumDestinationNameLength());
        if (name.isBlank()) return apiResult(CoordinateNoteStatus.INVALID_REQUEST, Result.INVALID);
        UUID sourceId = UUID.nameUUIDFromBytes(request.sourceId().toString()
            .getBytes(StandardCharsets.UTF_8));
        var target = request.destination();
        CoordinateSnapshot snapshot = new CoordinateSnapshot(
            UUID.randomUUID(), sourceId, name, dimension,
            target.x(), target.y(), target.z(), target.yaw(),
            player.getUUID(), profileName(player), player.getUUID(), profileName(player));
        Result result = createNoteItem(player, snapshot);
        return switch (result) {
            case SUCCESS -> apiResult(CoordinateNoteStatus.CREATED, result);
            case DISABLED -> apiResult(CoordinateNoteStatus.SHARING_DISABLED, result);
            case DIMENSION_UNAVAILABLE -> apiResult(
                CoordinateNoteStatus.TARGET_DIMENSION_UNAVAILABLE, result);
            case PAPER_REQUIRED -> apiResult(CoordinateNoteStatus.PAPER_REQUIRED, result);
            case INVENTORY_FULL -> apiResult(CoordinateNoteStatus.INVENTORY_FULL, result);
            default -> apiResult(CoordinateNoteStatus.INVALID_REQUEST, result);
        };
    }

    private static Result createNoteItem(ServerPlayer player, CoordinateSnapshot snapshot) {
        Inventory inventory = player.getInventory();
        int paperSlot = -1;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(Items.PAPER)) { paperSlot = slot; break; }
        }
        if (paperSlot < 0) return tell(player, Result.PAPER_REQUIRED);
        boolean creative = player.getAbilities().instabuild;
        boolean emptySlot = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).isEmpty()) { emptySlot = true; break; }
        }
        ItemStack paper = inventory.getItem(paperSlot);
        if (!emptySlot && (creative || paper.getCount() > 1)) return tell(player, Result.INVENTORY_FULL);

        ItemStack note = note(player, snapshot);
        if (!creative) paper.shrink(1);
        if (!inventory.add(note)) {
            if (!creative) paper.grow(1);
            return tell(player, Result.INVENTORY_FULL);
        }
        return Result.SUCCESS;
    }

    private static CoordinateNoteResult apiResult(CoordinateNoteStatus status, Result result) {
        String key = status == CoordinateNoteStatus.CREATED
            ? "message.riftgun.coordinate_note_created" : result.key();
        return new CoordinateNoteResult(status, Component.translatable(key));
    }

    public static Result importChat(ServerPlayer player, UUID shareId) {
        if (!RiftConfigs.server().coordinateSharing().enabled()) return tell(player, Result.DISABLED);
        MinecraftServer server = server(player);
        if (server == null) return tell(player, Result.INVALID);
        long now = server.getTickCount();
        ChatShare share = CHAT_SHARES.get(shareId);
        if (share == null || now >= share.expiresAt()) {
            CHAT_SHARES.remove(shareId);
            return tell(player, Result.EXPIRED);
        }
        if (share.importedPlayers().contains(player.getUUID())) return tell(player, Result.ALREADY_IMPORTED);
        Result result = importSnapshot(player, share.snapshot());
        if (result == Result.SUCCESS) share.importedPlayers().add(player.getUUID());
        return tellImport(player, result);
    }

    /** Used by CoordinateNoteItem. Caller consumes the note only on SUCCESS. */
    public static Result importNote(ServerPlayer player, ItemStack stack) {
        CoordinateSnapshot snapshot = CoordinateSnapshot.load(stack.getOrDefault(
            PortalGunComponents.COORDINATE_SNAPSHOT, new net.minecraft.nbt.CompoundTag()));
        Result result = snapshot == null ? Result.INVALID : importSnapshot(player, snapshot);
        return tellImport(player, result);
    }

    private static Result importSnapshot(ServerPlayer player, CoordinateSnapshot snapshot) {
        if (!RiftConfigs.server().coordinateSharing().enabled()) return Result.DISABLED;
        MinecraftServer server = server(player);
        if (!snapshot.valid() || server == null) return Result.INVALID;
        var targetLevel = server.getLevel(snapshot.dimension());
        if (targetLevel == null) return Result.DIMENSION_UNAVAILABLE;
        BlockPos position = new BlockPos((int) Math.floor(snapshot.x()), (int) Math.floor(snapshot.y()),
            (int) Math.floor(snapshot.z()));
        if (!targetLevel.isInWorldBounds(position)) return Result.INVALID;
        PortalPlayerData data = PortalDataStore.load(player);
        if (snapshot.sharedById().equals(player.getUUID())
            && data.destination(snapshot.sourceDestinationId()).isPresent()) return Result.ALREADY_IMPORTED;
        if (data.destinations().size() >= RiftConfigs.server().destinations().maximumDestinations()) return Result.LIMIT;

        UUID id = UUID.randomUUID();
        String name = uniqueName(data, snapshot.name(), snapshot.sharedByName());
        long now = player.level().getGameTime();
        data.destinations().add(new Destination(id, name, PortalPlayerData.SHARED_SECTION_ID,
            snapshot.dimension(), snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), now, 0L, false));
        data.shareProvenance(id, ShareProvenance.from(snapshot));
        data.selectedPlayerId(null);
        data.selectedDestinationId(id);
        data.lastViewedDestinationId(id);
        data.expandedGroups().add(PortalPlayerData.SHARED_SECTION_ID);
        PortalDataStore.save(player, data);
        return Result.SUCCESS;
    }

    private static CoordinateSnapshot snapshot(ServerPlayer player, PortalPlayerData data, Destination destination) {
        ShareProvenance provenance = data.shareProvenance(destination.id()).orElse(null);
        UUID originalId = provenance == null ? player.getUUID() : provenance.originalAuthorId();
        String originalName = provenance == null ? profileName(player) : provenance.originalAuthorName();
        return new CoordinateSnapshot(UUID.randomUUID(), destination.id(), destination.name(), destination.dimension(),
            destination.x(), destination.y(), destination.z(), destination.yaw(), originalId, originalName,
            player.getUUID(), profileName(player));
    }

    private static ItemStack note(ServerPlayer player, CoordinateSnapshot snapshot) {
        ItemStack stack = new ItemStack(RiftGun.coordinateNote());
        stack.set(PortalGunComponents.COORDINATE_SNAPSHOT, snapshot.save());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.riftgun.coordinate_note.named", snapshot.name())
            .withStyle(style -> style.withItalic(false)));
        ArrayList<Component> lore = new ArrayList<>();
        lore.add(Component.translatable("tooltip.riftgun.coordinate_note.shared_by", snapshot.sharedByName())
            .withStyle(ChatFormatting.GRAY));
        lore.add(Component.translatable("tooltip.riftgun.coordinate_note.original_author", snapshot.originalAuthorName())
            .withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.translatable("tooltip.riftgun.coordinate_note.dimension", dimensionName(player, snapshot))
            .withStyle(ChatFormatting.GRAY));
        if (dynamicDimensionName(player, dimensionId(snapshot)).isEmpty()) {
            lore.add(Component.translatable("tooltip.riftgun.coordinate_note.dimension_id", dimensionId(snapshot))
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        lore.add(Component.translatable("tooltip.riftgun.coordinate_note.position",
            format(snapshot.x()), format(snapshot.y()), format(snapshot.z())).withStyle(ChatFormatting.GRAY));
        lore.add(Component.translatable("tooltip.riftgun.coordinate_note.yaw", format(snapshot.yaw()))
            .withStyle(ChatFormatting.GRAY));
        lore.add(Component.translatable("tooltip.riftgun.coordinate_note.use").withStyle(ChatFormatting.AQUA));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private static Component clickAction(ServerPlayer player, CoordinateSnapshot snapshot) {
        String command = "/riftgun share import " + snapshot.snapshotId();
        Component hover = Component.translatable("chat.riftgun.coordinate_hover",
            format(snapshot.x()), format(snapshot.y()), format(snapshot.z()),
            dimensionName(player, snapshot), format(snapshot.yaw()),
            RiftConfigs.server().coordinateSharing().chatExpirySeconds());
        return Component.translatable("chat.riftgun.coordinate_add").withStyle(style -> style
            .withColor(ChatFormatting.AQUA).withUnderlined(true)
//? if >=1.21.11 {
            /*.withClickEvent(new ClickEvent.RunCommand(command))
            .withHoverEvent(new HoverEvent.ShowText(hover)));
*///?} else {
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
//?}
    }

    private static Component dimensionName(ServerPlayer player, Destination destination) {
//? if >=1.21.11 {
        /*String id = destination.dimension().identifier().toString();
*///?} else {
        String id = destination.dimension().location().toString();
//?}
        return friendlyDimension(player, id);
    }

    private static Component dimensionName(ServerPlayer player, CoordinateSnapshot snapshot) {
        return friendlyDimension(player, dimensionId(snapshot));
    }

    private static String dimensionId(CoordinateSnapshot snapshot) {
//? if >=1.21.11 {
        /*return snapshot.dimension().identifier().toString();
*///?} else {
        return snapshot.dimension().location().toString();
//?}
    }

    private static Component friendlyDimension(ServerPlayer player, String id) {
        Optional<Component> dynamic = dynamicDimensionName(player, id);
        if (dynamic.isPresent()) return dynamic.orElseThrow();
        String[] parts = id.split(":", 2);
        return parts.length == 2 ? Component.translatable("dimension." + parts[0] + "." + parts[1])
            : Component.literal(id);
    }

    private static Optional<Component> dynamicDimensionName(ServerPlayer player, String id) {
        try {
            return RiftGunDimensionLabels.label(player, RiftResourceId.parse(id));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static String uniqueName(PortalPlayerData data, String base, String sharer) {
        int maximum = RiftConfigs.server().destinations().maximumDestinationNameLength();
        if (data.destinations().stream().noneMatch(value -> value.name().equalsIgnoreCase(base))) {
            return trim(base, maximum);
        }
        String sharerCandidate = trim(base + " (" + sharer + ")", maximum);
        if (data.destinations().stream().noneMatch(
            value -> value.name().equalsIgnoreCase(sharerCandidate))) return sharerCandidate;
        for (int index = 2; ; index++) {
            String suffix = " (" + index + ")";
            String candidate = trim(base, Math.max(1, maximum - suffix.length())) + suffix;
            String tested = candidate;
            if (data.destinations().stream().noneMatch(value -> value.name().equalsIgnoreCase(tested))) return candidate;
        }
    }

    private static String trim(String value, int maximum) {
        String clean = value.strip();
        return clean.length() <= maximum ? clean : clean.substring(0, maximum);
    }

    private static String format(double value) { return String.format(Locale.ROOT, "%.1f", value); }

    private static MinecraftServer server(ServerPlayer player) {
//? if >=1.21.11 {
        /*return player.level().getServer();
*///?} else {
        return player.getServer();
//?}
    }

    private static String profileName(ServerPlayer player) {
//? if >=1.21.11 {
        /*return player.getGameProfile().name();
*///?} else {
        return player.getGameProfile().getName();
//?}
    }
    private static Result tell(ServerPlayer player, Result result) {
        if (result != Result.SUCCESS) Msg.displayClientMessage(player, Component.translatable(result.key()), true);
        return result;
    }

    private static Result tellImport(ServerPlayer player, Result result) {
        Msg.displayClientMessage(player, Component.translatable(result.key()), true);
        return result;
    }

    private static void prune(long now) {
        CHAT_SHARES.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt());
    }

    private record ChatShare(CoordinateSnapshot snapshot, long expiresAt, Set<UUID> importedPlayers) {}
    private CoordinateSharingService() {}
}

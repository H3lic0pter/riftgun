package dev.riftgun.portal;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.crisis.PortalCrisisConfigurationSnapshot;
import dev.riftgun.crisis.PortalCrisisEvaluationLedger;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Owns crisis configuration, pair-shared evaluation state, exit quota and return linkage. */
final class PortalCrisisSession {
    private PortalCrisisConfigurationSnapshot configuration = PortalCrisisConfigurationSnapshot.stable();
    private final PortalCrisisEvaluationLedger evaluations = new PortalCrisisEvaluationLedger();
    private int exitCount;
    private @Nullable UUID playerId;
    private @Nullable PortalExitTarget returnTarget;
    private @Nullable UUID parentId;
    private @Nullable ResourceKey<Level> parentDimension;

    PortalCrisisConfigurationSnapshot configuration() {
        return configuration;
    }

    void configure(PortalCrisisConfigurationSnapshot configuration) {
        this.configuration = configuration;
    }

    boolean reserve(UUID player, @Nullable PortalCrisisSession linked, int maximumTrackedPlayers) {
        if (!configuration.unstable()) return false;
        return evaluations.reserve(player, linked == null ? null : linked.evaluations, maximumTrackedPlayers);
    }

    boolean canCreateExit(@Nullable PortalCrisisSession linked, int maximumExits) {
        int current = linked == null ? exitCount : Math.max(exitCount, linked.exitCount);
        return current < maximumExits;
    }

    void commitExit(@Nullable PortalCrisisSession linked) {
        int next = (linked == null ? exitCount : Math.max(exitCount, linked.exitCount)) + 1;
        exitCount = next;
        if (linked != null) linked.exitCount = next;
    }

    void copyPairStateTo(PortalCrisisSession target, int maximumTrackedPlayers) {
        target.evaluations.copyFrom(evaluations, maximumTrackedPlayers);
        target.exitCount = exitCount;
    }

    void configureReturn(UUID player, PortalExitTarget target, UUID parent,
                         ResourceKey<Level> parentDimension) {
        configuration = PortalCrisisConfigurationSnapshot.stable();
        playerId = player;
        returnTarget = target;
        parentId = parent;
        this.parentDimension = parentDimension;
    }

    boolean isReturnExit() {
        return returnTarget != null;
    }

    boolean allowsReturn(UUID entityId, boolean hasPassengers) {
        return playerId != null && entityId.equals(playerId) && !hasPassengers;
    }

    @Nullable PortalExitTarget returnTarget() {
        return returnTarget;
    }

    @Nullable ServerLevel returnLevel(MinecraftServer server) {
        return returnTarget == null ? null : server.getLevel(returnTarget.dimension());
    }

    @Nullable PortalEntity findParent(MinecraftServer server) {
        if (parentId == null) return null;
        if (parentDimension != null) {
            ServerLevel level = server.getLevel(parentDimension);
            if (level != null && level.getEntity(parentId) instanceof PortalEntity portal) return portal;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(parentId) instanceof PortalEntity portal) return portal;
        }
        return null;
    }

    void load(CompoundTag tag, int maximumTrackedPlayers) {
        configuration = tag.contains("PortalCrises")
            ? PortalCrisisConfigurationSnapshot.load(Nbt.getCompound(tag, "PortalCrises"))
            : PortalCrisisConfigurationSnapshot.stable();
        if (Nbt.contains(tag, "CrisisEvaluations")) {
            evaluations.load(Nbt.getCompound(tag, "CrisisEvaluations"), maximumTrackedPlayers);
        } else {
            CompoundTag legacy = new CompoundTag();
            legacy.put("Players", Nbt.getList(tag, "CrisisEvaluatedPlayers"));
            evaluations.load(legacy, maximumTrackedPlayers);
        }
        exitCount = Math.max(0, Nbt.getInt(tag, "CrisisExitCount"));
        playerId = Nbt.hasUUID(tag, "CrisisPlayer") ? Nbt.getUUID(tag, "CrisisPlayer") : null;
        returnTarget = tag.contains("CrisisReturnTarget")
            ? PortalExitTarget.load(Nbt.getCompound(tag, "CrisisReturnTarget")) : null;
        parentId = Nbt.hasUUID(tag, "CrisisParent") ? Nbt.getUUID(tag, "CrisisParent") : null;
//? if >=1.21.11 {
        /*Identifier dimensionId = Identifier.tryParse(Nbt.getString(tag, "CrisisParentDimension"));
*///?} else {
        ResourceLocation dimensionId = ResourceLocation.tryParse(Nbt.getString(tag, "CrisisParentDimension"));
//?}
        parentDimension = dimensionId == null ? null
            : ResourceKey.create(Registries.DIMENSION, dimensionId);
    }

    void save(CompoundTag tag) {
        tag.put("PortalCrises", configuration.save());
        tag.put("CrisisEvaluations", evaluations.save());
        tag.putInt("CrisisExitCount", exitCount);
        if (playerId != null) Nbt.putUUID(tag, "CrisisPlayer", playerId);
        if (returnTarget != null) tag.put("CrisisReturnTarget", returnTarget.save());
        if (parentId != null) Nbt.putUUID(tag, "CrisisParent", parentId);
        if (parentDimension != null) {
//? if >=1.21.11 {
            /*tag.putString("CrisisParentDimension", parentDimension.identifier().toString());
*///?} else {
            tag.putString("CrisisParentDimension", parentDimension.location().toString());
//?}
        }
    }
}

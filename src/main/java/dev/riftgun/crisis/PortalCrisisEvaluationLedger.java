package dev.riftgun.crisis;

import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** Bounded access-order ledger that coordinates one crisis roll across a portal pair. */
public final class PortalCrisisEvaluationLedger {
    private final LinkedHashMap<UUID, Boolean> evaluated = new LinkedHashMap<>(16, 0.75F, true);

    public boolean reserve(UUID playerId, @Nullable PortalCrisisEvaluationLedger linked, int capacity) {
        requireCapacity(capacity);
        trimTo(capacity);
        if (linked != null && linked != this) linked.trimTo(capacity);
        boolean knownHere = touch(playerId);
        boolean knownLinked = linked != null && linked != this && linked.touch(playerId);
        if (knownHere || knownLinked) {
            if (!knownHere) record(playerId, capacity);
            if (linked != null && linked != this && !knownLinked) linked.record(playerId, capacity);
            return false;
        }

        record(playerId, capacity);
        if (linked != null && linked != this) linked.record(playerId, capacity);
        return true;
    }

    public void copyFrom(PortalCrisisEvaluationLedger source, int capacity) {
        requireCapacity(capacity);
        evaluated.clear();
        for (UUID playerId : source.evaluated.keySet()) record(playerId, capacity);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag players = new ListTag();
        for (UUID playerId : evaluated.keySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", playerId);
            players.add(entry);
        }
        tag.put("Players", players);
        return tag;
    }

    public void load(CompoundTag tag, int capacity) {
        requireCapacity(capacity);
        evaluated.clear();
        for (Tag raw : tag.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            if (entry.hasUUID("Id")) record(entry.getUUID("Id"), capacity);
        }
    }

    private boolean touch(UUID playerId) {
        return evaluated.get(playerId) != null;
    }

    private void record(UUID playerId, int capacity) {
        evaluated.put(playerId, Boolean.TRUE);
        trimTo(capacity);
    }

    private void trimTo(int capacity) {
        while (evaluated.size() > capacity) {
            var oldest = evaluated.entrySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    private static void requireCapacity(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Crisis evaluation capacity must be positive");
    }
}

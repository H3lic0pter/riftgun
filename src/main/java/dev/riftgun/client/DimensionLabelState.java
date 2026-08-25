package dev.riftgun.client;

import dev.riftgun.core.nbt.Nbt;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;

/** Client cache of server-resolved, addon-owned Dimension display names. */
public final class DimensionLabelState {
    private static volatile Map<String, String> labels = Map.of();

    public static void replace(CompoundTag envelope) {
        labels = decode(envelope);
    }

    public static synchronized void merge(CompoundTag envelope) {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>(labels);
        merged.putAll(decode(envelope));
        labels = Map.copyOf(merged);
    }

    public static Optional<String> label(String dimensionId) {
        return Optional.ofNullable(labels.get(dimensionId));
    }

    private static Map<String, String> decode(CompoundTag envelope) {
        LinkedHashMap<String, String> decoded = new LinkedHashMap<>();
        for (var raw : Nbt.getList(envelope, "DimensionLabels")) {
            CompoundTag entry = (CompoundTag) raw;
            String id = Nbt.getString(entry, "Id");
            String label = Nbt.getString(entry, "Label");
            if (!id.isBlank() && !label.isBlank()) decoded.put(id, label);
        }
        return Map.copyOf(decoded);
    }

    private DimensionLabelState() {}
}

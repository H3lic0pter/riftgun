package dev.riftgun.client;

import dev.riftgun.core.nbt.Nbt;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;

/** Client cache of server-resolved, addon-owned Dimension display names. */
public final class DimensionLabelState {
    private static volatile Map<String, String> labels = Map.of();
    private static volatile Map<String, String> catalogLabels = Map.of();
    private static volatile List<DimensionInfo> dimensions = List.of();

    public static synchronized void replace(CompoundTag envelope) {
        Map<String, String> decoded = decode(envelope);
        // Full mutation snapshots intentionally omit the traversal catalog. Keep the
        // catalog obtained while opening the GUI until a later catalog replaces it.
        if (envelope.contains("Dimensions")) {
            dimensions = decodeDimensions(envelope);
            catalogLabels = decoded;
        }
        LinkedHashMap<String, String> merged = new LinkedHashMap<>(catalogLabels);
        merged.putAll(decoded);
        labels = Map.copyOf(merged);
    }

    public static synchronized void merge(CompoundTag envelope) {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>(labels);
        merged.putAll(decode(envelope));
        labels = Map.copyOf(merged);
    }

    public static Optional<String> label(String dimensionId) {
        return Optional.ofNullable(labels.get(dimensionId));
    }

    public static List<DimensionInfo> dimensions() {
        return dimensions;
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

    private static List<DimensionInfo> decodeDimensions(CompoundTag envelope) {
        ArrayList<DimensionInfo> decoded = new ArrayList<>();
        for (var raw : Nbt.getList(envelope, "Dimensions")) {
            CompoundTag entry = (CompoundTag) raw;
            String id = Nbt.getString(entry, "Id");
            if (!id.isBlank()) decoded.add(new DimensionInfo(id, Nbt.getDouble(entry, "Scale")));
        }
        return List.copyOf(decoded);
    }

    public record DimensionInfo(String id, double coordinateScale) {}

    private DimensionLabelState() {}
}

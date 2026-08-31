package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

final class DimensionLabelStateTest {
    @Test
    void mutationSnapshotPreservesTraversalCatalogAndItsLabels() {
        CompoundTag catalog = new CompoundTag();
        ListTag dimensions = new ListTag();
        CompoundTag dimension = new CompoundTag();
        dimension.putString("Id", "test:catalog_dimension");
        dimension.putDouble("Scale", 8.0);
        dimensions.add(dimension);
        catalog.put("Dimensions", dimensions);
        catalog.put("DimensionLabels", labels("test:catalog_dimension", "Catalog"));
        DimensionLabelState.replace(catalog);

        CompoundTag mutation = new CompoundTag();
        mutation.put("DimensionLabels", labels("test:saved_destination", "Saved"));
        DimensionLabelState.replace(mutation);

        assertEquals("test:catalog_dimension", DimensionLabelState.dimensions().getFirst().id());
        assertEquals("Catalog", DimensionLabelState.label("test:catalog_dimension").orElseThrow());
        assertEquals("Saved", DimensionLabelState.label("test:saved_destination").orElseThrow());
    }

    private static ListTag labels(String id, String label) {
        ListTag labels = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putString("Id", id);
        entry.putString("Label", label);
        labels.add(entry);
        return labels;
    }
}

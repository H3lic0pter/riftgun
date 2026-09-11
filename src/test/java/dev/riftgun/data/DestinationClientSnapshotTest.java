package dev.riftgun.data;

import java.util.UUID;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DestinationClientSnapshotTest {
    private static final net.minecraft.resources.ResourceKey<Level> OVERWORLD =
        net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
//? if >=1.21.11 {
            /*net.minecraft.resources.Identifier.withDefaultNamespace("overworld"));
*///?} else {
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("overworld"));
//?}
    private static Destination destination(String text, boolean automatic) {
        return new Destination(UUID.randomUUID(), "Target", PortalPlayerData.DEFAULT_GROUP_ID,
            OVERWORLD, 1, 64, 2, 90, 1, 0, false, automatic, text);
    }

    @Test
    void fullLegacyLibraryHasSmallSnapshotAndPreservesServerText() throws java.io.IOException {
        var data = new PortalPlayerData();
        for (int i = 0; i < 256; i++) data.destinations().add(destination("a".repeat(16384), i % 2 == 0));
        var snapshot = data.clientSnapshot();
        assertFalse(snapshot.toString().contains("InfinityText"));
        assertTrue(snapshot.toString().length() < 160_000);
        var bytes = new java.io.ByteArrayOutputStream();
        net.minecraft.nbt.NbtIo.write(snapshot, new java.io.DataOutputStream(bytes));
        assertTrue(bytes.size() < 100_000, "Snapshot exceeds 100 KiB: " + bytes.size());
        System.out.println("256 text destinations: " + bytes.size() + " snapshot bytes");
        var client = PortalPlayerData.load(snapshot);
        assertEquals(256, client.destinations().size());
        for (int i = 0; i < 256; i++) {
            assertEquals("", client.destinations().get(i).infinityText());
            assertEquals(data.destinations().get(i).automaticSearch(), client.destinations().get(i).automaticSearch());
        }
        var reloaded = PortalPlayerData.load(data.save());
        assertEquals(data.destinations(), reloaded.destinations());
    }

    @Test
    void absentTextAndEmptyTextRemainDifferentOnClient() {
        assertNull(Destination.load(destination(null, false).clientSnapshot()).infinityText());
        assertEquals("", Destination.load(destination("", false).clientSnapshot()).infinityText());
    }

    @Test
    void budgetCountsUtf8BytesAndAllowsDeletingToRecoverCapacity() {
        var data = new PortalPlayerData();
        for (int i = 0; i < 16; i++) data.destinations().add(destination("a".repeat(16384), false));
        assertTrue(DestinationTextBudget.canAdd(data, ""));
        assertFalse(DestinationTextBudget.canAdd(data, "a"));
        data.destinations().removeFirst();
        assertTrue(DestinationTextBudget.canAdd(data, "a".repeat(16384)));
        assertFalse(DestinationTextBudget.canAdd(data, "中".repeat(6000)));
        assertFalse(DestinationTextBudget.canAdd(new PortalPlayerData(), "a".repeat(262145)));
    }
}

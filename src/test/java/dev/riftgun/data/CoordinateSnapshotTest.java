package dev.riftgun.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

final class CoordinateSnapshotTest {
    @Test
    void portableSnapshotRoundTripsWithoutLosingPrecisionOrProvenance() {
//? if >=1.21.11 {
        /*var id = Identifier.fromNamespaceAndPath("riftgun", "reality/test");
*///?} else {
        var id = ResourceLocation.fromNamespaceAndPath("riftgun", "reality/test");
//?}
        UUID snapshotId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID originalId = UUID.randomUUID();
        UUID sharerId = UUID.randomUUID();
        CoordinateSnapshot original = new CoordinateSnapshot(snapshotId, sourceId, "Village",
            ResourceKey.create(Registries.DIMENSION, id), 12.125, 64.75, -8.5, 91.25F,
            originalId, "Alex", sharerId, "Steve");

        CoordinateSnapshot restored = CoordinateSnapshot.load(original.save());

        assertNotNull(restored);
        assertEquals(original, restored);
    }

    @Test
    void sharedVirtualGroupAndProvenanceSurvivePlayerDataRoundTrip() {
        PortalPlayerData data = new PortalPlayerData();
        UUID destinationId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID originalId = UUID.randomUUID();
        UUID sharerId = UUID.randomUUID();
//? if >=1.21.11 {
        /*var dimensionId = Identifier.fromNamespaceAndPath("minecraft", "overworld");
*///?} else {
        var dimensionId = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
//?}
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        data.destinations().add(new Destination(destinationId, "Village", PortalPlayerData.SHARED_SECTION_ID,
            dimension, 1.0, 2.0, 3.0, 4.0F, 5L, 0L, false));
        data.shareProvenance(destinationId,
            new ShareProvenance(sourceId, originalId, "Alex", sharerId, "Steve"));

        PortalPlayerData restored = PortalPlayerData.load(data.save());

        assertEquals(PortalPlayerData.SHARED_SECTION_ID,
            restored.destination(destinationId).orElseThrow().groupId());
        assertEquals(sourceId, restored.shareProvenance(destinationId).orElseThrow().sourceDestinationId());
    }
}

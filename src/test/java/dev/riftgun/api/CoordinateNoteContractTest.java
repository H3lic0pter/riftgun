package dev.riftgun.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

final class CoordinateNoteContractTest {
    @Test
    void onlyCreatedStatusReportsSuccess() {
        CoordinateNoteResult created = new CoordinateNoteResult(
            CoordinateNoteStatus.CREATED, Component.literal("created"));
        CoordinateNoteResult rejected = new CoordinateNoteResult(
            CoordinateNoteStatus.PAPER_REQUIRED, Component.literal("paper"));

        assertTrue(created.created());
        assertFalse(rejected.created());
        assertEquals(new RiftGunApiVersion(1, 2, 0), RiftGunApi.coordinateNotes().version());
    }
}

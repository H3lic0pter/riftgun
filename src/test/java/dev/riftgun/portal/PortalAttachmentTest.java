package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class PortalAttachmentTest {
    @Test
    void synchronizedValuesRoundTripAnAnchoredPortal() {
        BlockPos anchor = new BlockPos(12, 34, -56);
        PortalAttachment attachment = PortalAttachment.of(anchor, Direction.NORTH);

        PortalAttachment restored = PortalAttachment.fromSynced(
            attachment.syncedAnchor(), attachment.syncedFace());

        assertTrue(restored.anchored());
        assertEquals(anchor, restored.anchor());
        assertEquals(Direction.NORTH, restored.face());
    }

    @Test
    void incompleteOrInvalidSynchronizedValuesBecomeUnanchored() {
        assertFalse(PortalAttachment.fromSynced(Optional.of(BlockPos.ZERO), -1).anchored());
        assertFalse(PortalAttachment.fromSynced(Optional.empty(), Direction.UP.ordinal()).anchored());
        assertFalse(PortalAttachment.fromSynced(Optional.of(BlockPos.ZERO), 99).anchored());
    }

    @Test
    void nbtRoundTripPreservesAttachmentAndRejectsIncompleteData() {
        PortalAttachment original = PortalAttachment.of(new BlockPos(-3, 80, 7), Direction.DOWN);
        assertEquals(original, PortalAttachment.load(original.save()));

        CompoundTag incomplete = new CompoundTag();
        incomplete.putLong("Anchor", BlockPos.ZERO.asLong());
        assertFalse(PortalAttachment.load(incomplete).anchored());
    }
}

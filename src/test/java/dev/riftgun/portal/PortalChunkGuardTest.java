package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class PortalChunkGuardTest {
    @Test
    void portalAndLinkedPortalLookupsAllowVoidPositions() {
        assertTrue(PortalChunkGuard.inWorldBounds(null, new BlockPos(0, -200, 0)));
        assertTrue(PortalChunkGuard.inWorldBounds(null, new BlockPos(0, 400, 0)));
    }

    @Test
    void corruptPositionsStillCannotLoadChunks() {
        assertFalse(PortalChunkGuard.inWorldBounds(null, new BlockPos(30_000_000, -200, 0)));
        assertFalse(PortalChunkGuard.inWorldBounds(null, new BlockPos(0, -20_000_001, 0)));
    }
}

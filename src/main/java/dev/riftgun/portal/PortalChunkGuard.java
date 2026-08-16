package dev.riftgun.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Shared world-bounds guard for portal chunk-ticket acquisition. Both
 * versions crash when asked to create a chunk far outside the world bounds,
 * so every ticket holder checks positions here before touching the chunk
 * system.
 */
public final class PortalChunkGuard {
    public static boolean inWorldBounds(ServerLevel level, BlockPos position) {
        return level.isInWorldBounds(position);
    }

    public static boolean inWorldBounds(ServerLevel level, ChunkPos chunk) {
        return level.isInWorldBounds(new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ()));
    }

    private PortalChunkGuard() {}
}

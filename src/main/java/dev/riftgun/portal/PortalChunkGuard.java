package dev.riftgun.portal;

import dev.riftgun.service.FloatingPortalBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Shared entity-coordinate guard for portal chunk-ticket acquisition. Both
 * versions crash when asked to create a chunk far outside the world bounds,
 * so every ticket holder checks positions here before touching the chunk
 * system. Tickets address X/Z chunks; a portal below block build height is valid.
 */
public final class PortalChunkGuard {
    public static boolean inWorldBounds(ServerLevel level, BlockPos position) {
        return FloatingPortalBounds.allows(position);
    }

    public static boolean inWorldBounds(ServerLevel level, ChunkPos chunk) {
        return FloatingPortalBounds.allows(new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ()));
    }

    private PortalChunkGuard() {}
}

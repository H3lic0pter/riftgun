package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Bounds expensive client prediction work and reuses immutable render geometry between frames. */
public final class PortalPlacementPreviewCache {
    private static final int STATIONARY_REFRESH_TICKS = 10;
    private Input lastInput;
    private long lastRefreshTick = Long.MIN_VALUE;
    private long nextRefreshTick = Long.MIN_VALUE;
    private List<PortalPlacementPreviewGeometry.Segment> segments = List.of();

    public boolean shouldRefresh(long tick, Input input) {
        if (lastInput == null || tick < lastRefreshTick) return true;
        boolean changed = !lastInput.equals(input);
        if (!changed && tick - lastRefreshTick < STATIONARY_REFRESH_TICKS) return false;
        return tick >= nextRefreshTick;
    }

    public void update(long tick, Input input, PortalPlacement placement) {
        lastInput = input;
        lastRefreshTick = tick;
        nextRefreshTick = tick + refreshInterval(input.range());
        segments = placement == null
            ? List.of() : PortalPlacementPreviewGeometry.corners(placement);
    }

    public List<PortalPlacementPreviewGeometry.Segment> segments() {
        return segments;
    }

    public void clear() {
        lastInput = null;
        lastRefreshTick = Long.MIN_VALUE;
        nextRefreshTick = Long.MIN_VALUE;
        segments = List.of();
    }

    static int refreshInterval(int range) {
        if (range >= 256) return 4;
        if (range >= 64) return 2;
        return 1;
    }

    public record Input(Vec3 eye, Vec3 look, int range, PortalAperture aperture,
                        float pitch, float yaw, @Nullable BlockPos anchor,
                        @Nullable Direction face) {
        public Input(Vec3 eye, Vec3 look, int range, PortalAperture aperture,
                     float pitch, float yaw) {
            this(eye, look, range, aperture, pitch, yaw, null, null);
        }
    }
}

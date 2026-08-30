package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Bounds expensive client prediction work and reuses immutable render geometry between frames. */
public final class PortalPlacementPreviewCache {
    private static final int STATIONARY_REFRESH_TICKS = 10;
    private static final long PREVIEW_WORK_BUDGET_NANOS = 2_000_000L;
    private static final int MAXIMUM_ADAPTIVE_REFRESH_TICKS = 20;
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
        updateMeasured(tick, input, placement, 0L);
    }

    public void updateMeasured(long tick, Input input, PortalPlacement placement,
                               long elapsedNanos) {
        lastInput = input;
        lastRefreshTick = tick;
        nextRefreshTick = tick + refreshInterval(input.range(), elapsedNanos);
        segments = placement == null
            ? List.of() : PortalPlacementPreviewGeometry.visibleOutline(placement);
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

    static int refreshInterval(int range, long elapsedNanos) {
        int measuredInterval = elapsedNanos <= 0L ? 1
            : (int) Math.min(MAXIMUM_ADAPTIVE_REFRESH_TICKS,
                Math.ceil((double) elapsedNanos / PREVIEW_WORK_BUDGET_NANOS));
        return Math.max(refreshInterval(range), measuredInterval);
    }

    public record Input(Vec3 eye, Vec3 look, int range, PortalAperture aperture,
                        float pitch, float yaw, @Nullable BlockPos anchor,
                        @Nullable Direction face, @Nullable PortalOrientation orientation) {
        public Input(Vec3 eye, Vec3 look, int range, PortalAperture aperture,
                     float pitch, float yaw) {
            this(eye, look, range, aperture, pitch, yaw, null, null, null);
        }

        public Input(Vec3 eye, Vec3 look, int range, PortalAperture aperture,
                     float pitch, float yaw, PortalOrientation orientation) {
            this(eye, look, range, aperture, pitch, yaw, null, null, orientation);
        }

        public Input(Vec3 eye, Vec3 look, int range, PortalAperture aperture,
                     float pitch, float yaw, BlockPos anchor, Direction face) {
            this(eye, look, range, aperture, pitch, yaw, anchor, face, null);
        }
    }
}

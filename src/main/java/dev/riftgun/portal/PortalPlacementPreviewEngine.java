package dev.riftgun.portal;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import dev.riftgun.pairing.PortalPairingPreviewGeometry;
import dev.riftgun.service.SurfaceFaceSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Version-neutral placement-preview state machine. Callers translate Minecraft client state into
 * one immutable input and provide world queries through {@link Resolver}.
 */
public final class PortalPlacementPreviewEngine {
    private final PortalPlacementPreviewCache cache = new PortalPlacementPreviewCache();
    private PortalPairingPendingEndpoint pendingEndpoint;
    private PortalPairingPendingEndpoint entityTargetEndpoint;
    private List<PortalPairingPreviewGeometry.ColoredSegment> pendingSegments = List.of();
    private List<PortalPairingPreviewGeometry.ColoredSegment> entityTargetSegments = List.of();
    private Object levelIdentity;
    private PreviewContext previewContext = PreviewContext.NONE;

    public void tick(TickInput input, Resolver resolver) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(resolver);
        if (input.levelIdentity() != levelIdentity) {
            levelIdentity = input.levelIdentity();
            cache.clear();
            clearPending();
            clearEntityTarget();
        }
        PreviewContext nextContext = previewContext(input);
        if (nextContext != previewContext) {
            previewContext = nextContext;
            cache.clear();
        }
        tickPending(input, resolver);
        tickEntityTarget(input, resolver);
        if (tickPrecision(input, resolver)) return;
        if (tickShiftRoutedPreview(input, resolver)) return;
        PortalPlacementPreviewCache.Input remoteInput = remoteInput(input);
        if (remoteInput == null) {
            cache.clear();
            return;
        }
        if (!cache.shouldRefresh(input.tick(), remoteInput)) return;
        updateRemotePreview(input.tick(), remoteInput, null, resolver);
    }

    public @Nullable PortalPlacement currentPlacement() {
        return previewContext == PreviewContext.PRECISION ? cache.placement() : null;
    }

    public Frame frame() {
        return new Frame(cache.segments(), pendingSegments, entityTargetSegments);
    }

    private PreviewContext previewContext(TickInput input) {
        if (input.precisionTarget() != null) return PreviewContext.PRECISION;
        if (input.clientIdle() && input.shiftDown() && input.gun() != null
            && usesShiftRoutedPreview(input.gun())) return PreviewContext.SHIFT_ROUTED;
        return remoteInput(input) == null ? PreviewContext.NONE : PreviewContext.REMOTE;
    }

    private void tickPending(TickInput input, Resolver resolver) {
        Gun gun = input.gun();
        if (!input.worldReady() || gun == null
            || gun.placementMode() == PortalPlacementMode.ENTITY_RELOCATION) {
            clearPending();
            return;
        }
        PortalPairingPendingEndpoint next = gun.pending();
        if (next == null || !next.pairEndpoint() || !resolver.markerVisible(next)) {
            clearPending();
            return;
        }
        if (Objects.equals(next, pendingEndpoint)) return;
        pendingEndpoint = next;
        pendingSegments = PortalPairingPreviewGeometry.segments(next.placement(), next.endpoint());
    }

    private void clearPending() {
        pendingEndpoint = null;
        pendingSegments = List.of();
    }

    private void tickEntityTarget(TickInput input, Resolver resolver) {
        Gun gun = input.gun();
        PortalPairingPendingEndpoint next = gun != null
            && gun.functionMode() == PortalFunctionMode.PORTAL_PAIRING
            && gun.placementMode() == PortalPlacementMode.ENTITY_RELOCATION
            ? gun.pending() : null;
        if (next == null || !next.entityTarget() || !input.worldReady()
            || !resolver.markerVisible(next)) {
            clearEntityTarget();
            return;
        }
        if (Objects.equals(next, entityTargetEndpoint)) return;
        entityTargetEndpoint = next;
        entityTargetSegments = PortalPairingPreviewGeometry.entityTargetSegments(next.placement());
    }

    private void clearEntityTarget() {
        entityTargetEndpoint = null;
        entityTargetSegments = List.of();
    }

    private @Nullable PortalPlacementPreviewCache.Input remoteInput(TickInput input) {
        Gun gun = input.gun();
        PlayerView player = input.player();
        if (!input.clientIdle() || player == null || gun == null
            || !gun.remotePlacementPreview() || !gun.remote()
            || gun.placementMode() != PortalPlacementMode.REMOTE) return null;
        return cacheInput(player, gun.remoteDistance(), gun.aperture());
    }

    private boolean tickPrecision(TickInput input, Resolver resolver) {
        PrecisionTarget target = input.precisionTarget();
        if (target == null) return false;
        Gun gun = input.gun();
        PlayerView player = input.player();
        if (gun == null || player == null) {
            cache.clear();
            return true;
        }
        if (target instanceof PrecisionTarget.Floating floating) {
            PortalOrientation orientation = floating.orientation();
            PortalPlacementMode mode = gun.placementMode();
            if (mode == PortalPlacementMode.SMART) {
                mode = gun.smartFallback() == PortalFloatingFallback.REMOTE
                    ? PortalPlacementMode.REMOTE : PortalPlacementMode.FRONT;
            }
            int range = mode == PortalPlacementMode.REMOTE ? gun.remoteDistance() : 2;
            PortalPlacementPreviewCache.Input cacheInput = new PortalPlacementPreviewCache.Input(
                player.eye(), player.look(), range, gun.aperture(), player.pitch(), player.yaw(),
                orientation);
            if (!cache.shouldRefresh(input.tick(), cacheInput)) return true;
            if (mode == PortalPlacementMode.REMOTE) {
                updateRemotePreview(input.tick(), cacheInput, orientation, resolver);
            } else {
                cache.update(input.tick(), cacheInput, resolver.front(gun, orientation));
            }
            return true;
        }
        PrecisionTarget.Surface surface = (PrecisionTarget.Surface) target;
        int range = gun.maximumSurfaceRange();
        PortalPlacementPreviewCache.Input cacheInput = new PortalPlacementPreviewCache.Input(
            player.eye(), player.look(), range, gun.aperture(), player.pitch(), player.yaw(),
            surface.anchor(), surface.face());
        if (!cache.shouldRefresh(input.tick(), cacheInput)) return true;
        Vec3 faceCenter = Vec3.atCenterOf(surface.anchor()).add(new Vec3(
            surface.face().getStepX(), surface.face().getStepY(), surface.face().getStepZ())
            .scale(0.5));
        cache.updateSurface(input.tick(), cacheInput, resolver.surface(gun,
            new SurfaceFaceSelection(surface.anchor(), surface.face()),
            player.eye().distanceTo(faceCenter), range));
        return true;
    }

    private boolean tickShiftRoutedPreview(TickInput input, Resolver resolver) {
        Gun gun = input.gun();
        PlayerView player = input.player();
        if (!input.clientIdle() || !input.shiftDown() || gun == null || player == null
            || !usesShiftRoutedPreview(gun)) return false;
        boolean smartRemote = gun.remote() && gun.placementMode() == PortalPlacementMode.SMART
            && gun.smartFallback() == PortalFloatingFallback.REMOTE;
        int surfaceRange = smartRemote ? gun.smartDistance() : gun.maximumSurfaceRange();
        SurfaceHit hit = resolver.surfaceHit(gun.maximumSurfaceRange());
        if (hit != null && hit.distance() <= surfaceRange) {
            PortalPlacementPreviewCache.Input cacheInput = new PortalPlacementPreviewCache.Input(
                player.eye(), player.look(), surfaceRange, gun.aperture(),
                player.pitch(), player.yaw(), hit.selection().anchor(), hit.selection().face());
            if (!cache.shouldRefresh(input.tick(), cacheInput)) return true;
            PortalPlacement surface = resolver.surface(gun, hit.selection(), hit.distance(),
                gun.maximumSurfaceRange());
            if (surface != null) {
                cache.updateSurface(input.tick(), cacheInput, surface);
                return true;
            }
        }
        int range = gun.remote() ? gun.remoteDistance() : gun.maximumSurfaceRange();
        PortalPlacementPreviewCache.Input cacheInput = cacheInput(player, range, gun.aperture());
        if (cache.shouldRefresh(input.tick(), cacheInput)) {
            updateRemotePreview(input.tick(), cacheInput, null, resolver);
        }
        return true;
    }

    public static boolean usesShiftRoutedPreview(Gun gun) {
        boolean pairingEntity = gun.functionMode() == PortalFunctionMode.PORTAL_PAIRING
            && gun.placementMode() == PortalPlacementMode.ENTITY_RELOCATION;
        boolean smartRemote = gun.remote() && gun.placementMode() == PortalPlacementMode.SMART
            && gun.smartFallback() == PortalFloatingFallback.REMOTE;
        return pairingEntity || smartRemote;
    }

    private PortalPlacementPreviewCache.Input cacheInput(PlayerView player, int range,
                                                         PortalAperture aperture) {
        return new PortalPlacementPreviewCache.Input(player.eye(), player.look(), range, aperture,
            player.pitch(), player.yaw());
    }

    private void updateRemotePreview(long tick, PortalPlacementPreviewCache.Input input,
                                     @Nullable PortalOrientation orientation, Resolver resolver) {
        long started = System.nanoTime();
        PortalPlacement placement = resolver.remote(input.range(), input.aperture(), orientation);
        cache.updateMeasured(tick, input, placement, System.nanoTime() - started);
    }

    public record TickInput(
        @Nullable Object levelIdentity,
        long tick,
        boolean worldReady,
        boolean clientIdle,
        boolean shiftDown,
        @Nullable PlayerView player,
        @Nullable Gun gun,
        @Nullable PrecisionTarget precisionTarget
    ) {}

    public record PlayerView(Vec3 eye, Vec3 look, float pitch, float yaw) {}

    public record Gun(
        PortalFunctionMode functionMode,
        PortalPlacementMode placementMode,
        PortalFloatingFallback smartFallback,
        int maximumSurfaceRange,
        int smartDistance,
        int remoteDistance,
        PortalAperture aperture,
        boolean remote,
        boolean remotePlacementPreview,
        @Nullable PortalPairingPendingEndpoint pending
    ) {}

    public sealed interface PrecisionTarget {
        record Surface(BlockPos anchor, Direction face) implements PrecisionTarget {}
        record Floating(PortalOrientation orientation) implements PrecisionTarget {}
    }

    public record SurfaceHit(SurfaceFaceSelection selection, double distance) {}

    public interface Resolver {
        boolean markerVisible(PortalPairingPendingEndpoint endpoint);
        @Nullable SurfaceHit surfaceHit(int maximumRange);
        @Nullable PortalPlacement surface(Gun gun, SurfaceFaceSelection selection,
                                          double distance, int maximumRange);
        @Nullable PortalPlacement front(Gun gun, PortalOrientation orientation);
        @Nullable PortalPlacement remote(int range, PortalAperture aperture,
                                         @Nullable PortalOrientation orientation);
    }

    public record Frame(
        List<PortalPlacementPreviewGeometry.Segment> segments,
        List<PortalPairingPreviewGeometry.ColoredSegment> pendingSegments,
        List<PortalPairingPreviewGeometry.ColoredSegment> entityTargetSegments
    ) {
        public boolean isEmpty() {
            return segments.isEmpty() && pendingSegments.isEmpty() && entityTargetSegments.isEmpty();
        }
    }

    private enum PreviewContext { NONE, REMOTE, PRECISION, SHIFT_ROUTED }
}

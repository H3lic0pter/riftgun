package dev.riftgun.client;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.input.SurfaceFacePreviewState;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.service.PrecisionPlacementIntent;
import dev.riftgun.service.SurfaceFaceSelection;
import dev.riftgun.state.PortalGunViewState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** Shared, render-agnostic state machine for both mode-radial screen adapters. */
public final class ModeRadialController {
    public enum Page { PLACEMENT, PREDICTION, SURFACE_FACE, FLOATING_ORIENTATION }

    private static final long RANGE_SEND_INTERVAL_NANOS = 100_000_000L;
    private static final List<PortalOrientation> ORIENTATION_OPTIONS =
        List.of(PortalOrientation.values());
    private static final List<PortalPredictionMode> PREDICTION_OPTIONS =
        List.of(PortalPredictionMode.values());
    private static final List<PortalPlacementMode> BASE_PLACEMENT_OPTIONS = List.of(
        PortalPlacementMode.SMART, PortalPlacementMode.FRONT, PortalPlacementMode.SURFACE);
    private static final List<PortalPlacementMode> REMOTE_PLACEMENT_OPTIONS = List.of(
        PortalPlacementMode.SMART, PortalPlacementMode.FRONT, PortalPlacementMode.REMOTE,
        PortalPlacementMode.SURFACE);
    private static final List<PortalPlacementMode> ENTITY_PLACEMENT_OPTIONS = List.of(
        PortalPlacementMode.SMART, PortalPlacementMode.FRONT, PortalPlacementMode.SURFACE,
        PortalPlacementMode.ENTITY_RELOCATION);
    private static final List<PortalPlacementMode> ALL_PLACEMENT_OPTIONS =
        List.of(PortalPlacementMode.values());

    private Page page = Page.PLACEMENT;
    private int selection = -1;
    private int lastAudibleSelection = -1;
    private boolean cancelled;
    private boolean suppressFinalRange;
    private PortalFunctionMode functionMode = PortalFunctionMode.COORDINATE_TRAVEL;
    private int remoteDistance = 1;
    private int maximumSurfaceRange = 1;
    private boolean draggingRange;
    private int lastSentRange = 1;
    private long lastRangeSendNanos;
    private final SurfaceFacePreviewState facePreview;
    private final boolean precisionPreviewOnly;
    private final BlockPos surfaceAnchor;
    private PortalOrientation selectedOrientation;

    public ModeRadialController(@Nullable PrecisionPlacementIntent precisionIntent,
                                Direction playerHeading, List<String> faceOrder) {
        SurfaceFaceSelection surface = precisionIntent != null
            && precisionIntent.kind() == PrecisionPlacementIntent.Kind.SURFACE
            ? precisionIntent.surface() : null;
        precisionPreviewOnly = precisionIntent != null;
        surfaceAnchor = surface == null ? null : surface.anchor();
        selectedOrientation = precisionIntent == null
            ? PortalOrientation.VERTICAL : precisionIntent.orientation();
        Direction referenceFace = surface == null ? Direction.NORTH : surface.face();
        facePreview = new SurfaceFacePreviewState(referenceFace, playerHeading, faceOrder);
        if (precisionIntent != null) {
            page = surface == null ? Page.FLOATING_ORIENTATION : Page.SURFACE_FACE;
        }
    }

    public void refresh(PortalGunViewState gun) {
        functionMode = gun.functionMode();
        maximumSurfaceRange = Math.max(1, gun.maximumSurfaceRange());
        remoteDistance = Math.clamp(gun.remoteDistance(), 1, maximumSurfaceRange);
        lastSentRange = remoteDistance;
    }

    public List<?> options(PortalGunViewState gun) {
        if (page == Page.SURFACE_FACE) return facePreview.choices();
        if (page == Page.FLOATING_ORIENTATION) return ORIENTATION_OPTIONS;
        if (page == Page.PREDICTION) return PREDICTION_OPTIONS;
        boolean entity = gun.entityRelocationEnabled();
        return entity ? gun.remoteInstalled() ? ALL_PLACEMENT_OPTIONS : ENTITY_PLACEMENT_OPTIONS
            : gun.remoteInstalled() ? REMOTE_PLACEMENT_OPTIONS : BASE_PLACEMENT_OPTIONS;
    }

    /** Updates the hovered option and returns whether the adapter should play a tick sound. */
    public boolean select(int index, PortalGunViewState gun) {
        List<?> options = options(gun);
        selection = index >= 0 && index < options.size() ? index : -1;
        if (page == Page.SURFACE_FACE && selection >= 0) {
            facePreview.select((SurfaceFacePreviewState.Choice) options.get(selection));
        } else if (page == Page.FLOATING_ORIENTATION && selection >= 0) {
            selectedOrientation = (PortalOrientation) options.get(selection);
        }
        boolean audible = selection >= 0 && selection != lastAudibleSelection;
        lastAudibleSelection = selection;
        return audible;
    }

    public PortalFunctionMode toggleFunctionMode() {
        functionMode = functionMode.toggle();
        return functionMode;
    }

    public SurfaceFacePreviewState.Frame toggleFaceFrame() {
        facePreview.toggleFrame();
        clearSelection();
        return facePreview.frame();
    }

    public void switchPage() {
        page = page == Page.PLACEMENT ? Page.PREDICTION : Page.PLACEMENT;
        clearSelection();
    }

    public int updateRange(double fraction) {
        double clamped = Math.clamp(fraction, 0.0, 1.0);
        remoteDistance = 1 + (int) Math.round(clamped * (maximumSurfaceRange - 1));
        return remoteDistance;
    }

    public boolean rangeSendDue(boolean force, boolean ready, boolean sliderEnabled, long now) {
        if (!ready || !sliderEnabled || remoteDistance == lastSentRange) return false;
        return force || now - lastRangeSendNanos >= RANGE_SEND_INTERVAL_NANOS;
    }

    public void rangeSent(long now) {
        lastSentRange = remoteDistance;
        lastRangeSendNanos = now;
    }

    public PrecisionPlacementIntent selectedPrecisionIntent(@Nullable PortalPlacement preview,
                                                            boolean includePreview) {
        PrecisionPlacementIntent intent = surfacePreviewOnly()
            ? PrecisionPlacementIntent.surface(
                new SurfaceFaceSelection(surfaceAnchor, facePreview.selectedFace()))
            : PrecisionPlacementIntent.floating(selectedOrientation);
        return includePreview && intent.kind() == PrecisionPlacementIntent.Kind.FLOATING
            ? intent.withPreviewPlacement(preview) : intent;
    }

    public @Nullable RadialSelection selectedRadialMode(PortalGunViewState gun) {
        if (page == Page.SURFACE_FACE) {
            return new RadialSelection(Page.PLACEMENT, PortalPlacementMode.SURFACE, functionMode);
        }
        if (selection < 0) return null;
        return new RadialSelection(page, (Enum<?>) options(gun).get(selection), functionMode);
    }

    public PortalPlacementMode floatingPlacementMode(PortalPlacementMode configured,
                                                       PortalGunViewState gun) {
        if (configured == PortalPlacementMode.REMOTE && !gun.remoteInstalled()) {
            return PortalPlacementMode.FRONT;
        }
        if (configured != PortalPlacementMode.SMART) return configured;
        PortalFloatingFallback fallback = functionMode == PortalFunctionMode.PORTAL_PAIRING
            ? gun.placement().pairingSmartFallback()
            : gun.placement().coordinateSmartFallback();
        return gun.remoteInstalled() && fallback == PortalFloatingFallback.REMOTE
            ? PortalPlacementMode.REMOTE : PortalPlacementMode.FRONT;
    }

    public void cancel(boolean suppressRange) {
        cancelled = true;
        suppressFinalRange |= suppressRange;
    }

    public Page page() { return page; }
    public int selection() { return selection; }
    public boolean cancelled() { return cancelled; }
    public boolean shouldSendFinalRange() { return !suppressFinalRange; }
    public PortalFunctionMode functionMode() { return functionMode; }
    public int remoteDistance() { return remoteDistance; }
    public int maximumSurfaceRange() { return maximumSurfaceRange; }
    public boolean draggingRange() { return draggingRange; }
    public void draggingRange(boolean value) { draggingRange = value; }
    public boolean precisionPreviewOnly() { return precisionPreviewOnly; }
    public boolean surfacePreviewOnly() { return surfaceAnchor != null; }
    public boolean surfaceFacePreviewOpen() { return surfaceAnchor != null; }
    public boolean floatingOrientationPreviewOpen() {
        return precisionPreviewOnly && page == Page.FLOATING_ORIENTATION;
    }
    public BlockPos surfaceAnchor() { return surfaceAnchor; }
    public Direction selectedSurfaceFace() { return facePreview.selectedFace(); }
    public PortalOrientation selectedOrientation() { return selectedOrientation; }
    public SurfaceFacePreviewState facePreview() { return facePreview; }

    private void clearSelection() {
        selection = -1;
        lastAudibleSelection = -1;
    }

    public record RadialSelection(Page page, Enum<?> mode, PortalFunctionMode functionMode) {}
}

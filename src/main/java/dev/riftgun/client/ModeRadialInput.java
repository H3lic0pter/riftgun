package dev.riftgun.client;

import dev.riftgun.input.RadialRequestState;
import dev.riftgun.network.PrecisionPlacementRequest;

/** Owns tap/hold and dedicated preview behavior without coupling the screen to key bindings. */
public final class ModeRadialInput {
    private static final int HOLD_TICKS = 6;
    private static boolean cycleWasDown;
    private static boolean radialWasDown;
    private static boolean surfacePreviewWasDown;
    private static boolean pairingOperationWasDown;
    private static int cycleHeldTicks;
    private static Source pendingSource;
    private static final RadialRequestState REQUEST = new RadialRequestState();
    private static boolean suppressUntilRelease;
    private static boolean cycleShortcutConsumed;
    private static PrecisionPlacementRequest pendingPrecisionRequest;

    public static void tick() {
        ModeRadialClientAccess.Keys keys = ModeRadialClientAccess.keys();
        boolean cycleDown = keys.cycleDown();
        boolean radialDown = keys.radialDown();
        boolean surfacePreviewDown = keys.precisionDown();
        boolean pairingOperationDown = keys.pairingOperationDown();

        if (ModeRadialClientAccess.radialScreenOpen()) {
            if (pairingOperationDown && !pairingOperationWasDown) {
                ModeRadialClientAccess.commitPairingShortcut();
            }
            if (pendingSource != null
                && !sourceDown(pendingSource, cycleDown, radialDown, surfacePreviewDown)) {
                if (REQUEST.release() == RadialRequestState.ReleaseResult.COMMIT) {
                    if (pendingSource == Source.PRECISION_PREVIEW) {
                        closePrecisionFromShortcutRelease();
                    } else {
                        ModeRadialClientAccess.commitAndClose(false);
                        suppressUntilRelease = true;
                    }
                }
            }
            remember(cycleDown, radialDown, surfacePreviewDown, pairingOperationDown);
            return;
        }
        if (suppressUntilRelease) {
            if (!cycleDown && !radialDown && !surfacePreviewDown) {
                suppressUntilRelease = false;
                pendingSource = null;
                pendingPrecisionRequest = null;
            }
            remember(cycleDown, radialDown, surfacePreviewDown, pairingOperationDown);
            return;
        }
        if (ModeRadialClientAccess.blockedOrUnavailable()) {
            reset(cycleDown, radialDown, surfacePreviewDown, pairingOperationDown);
            return;
        }

        if (cycleDown && keys.altDown() && !cycleWasDown) {
            ModeRadialClientAccess.sendToggleFunctionRequest();
            cycleShortcutConsumed = true;
            cycleHeldTicks = 0;
        }
        if (cycleShortcutConsumed) {
            if (!cycleDown) cycleShortcutConsumed = false;
            remember(cycleDown, radialDown, surfacePreviewDown, pairingOperationDown);
            return;
        }

        if (pendingSource != null) {
            if (!sourceDown(pendingSource, cycleDown, radialDown, surfacePreviewDown)) {
                REQUEST.release();
            }
            remember(cycleDown, radialDown, surfacePreviewDown, pairingOperationDown);
            return;
        }
        if (surfacePreviewDown && !surfacePreviewWasDown) {
            pendingPrecisionRequest = ModeRadialClientAccess.capturePrecisionTarget();
            if (pendingPrecisionRequest != null) request(Source.PRECISION_PREVIEW);
        } else if (radialDown && !radialWasDown) {
            request(Source.DEDICATED);
        }
        if (cycleDown) {
            cycleHeldTicks = cycleWasDown ? cycleHeldTicks + 1 : 1;
            if (cycleHeldTicks == HOLD_TICKS) request(Source.CYCLE);
        } else if (cycleWasDown) {
            if (cycleHeldTicks < HOLD_TICKS) {
                ModeRadialClientAccess.sendCycleRequest();
            }
            cycleHeldTicks = 0;
        }
        remember(cycleDown, radialDown, surfacePreviewDown, pairingOperationDown);
    }

    public static void openFromServer(int requestId) {
        if (pendingSource == null) return;
        RadialRequestState.AcknowledgeResult result = REQUEST.acknowledge(requestId,
            sourceDown(pendingSource, ModeRadialClientAccess.keys()));
        if (result == RadialRequestState.AcknowledgeResult.IGNORE) return;
        ModeRadialClientAccess.openOrRefresh(
            pendingSource == Source.PRECISION_PREVIEW ? pendingPrecisionRequest : null);
        if (result == RadialRequestState.AcknowledgeResult.COMMIT
            && ModeRadialClientAccess.radialScreenOpen()) {
            if (pendingSource == Source.PRECISION_PREVIEW) {
                closePrecisionFromShortcutRelease();
            } else {
                ModeRadialClientAccess.commitAndClose(false);
                suppressUntilRelease = true;
            }
        }
    }

    public static void rejectFromServer(int requestId) {
        if (pendingSource == null || !REQUEST.reject(requestId)) return;
        pendingSource = null;
        pendingPrecisionRequest = null;
        suppressUntilRelease = true;
        ModeRadialClientAccess.rejectAndClose();
    }

    public static void cancelFromScreen() {
        pendingSource = null;
        pendingPrecisionRequest = null;
        REQUEST.cancel();
        suppressUntilRelease = true;
    }

    private static void closePrecisionFromShortcutRelease() {
        pendingSource = null;
        pendingPrecisionRequest = null;
        REQUEST.cancel();
        suppressUntilRelease = true;
        ModeRadialClientAccess.commitAndClose(true);
    }

    public static boolean ready() { return pendingSource != null && REQUEST.ready(); }

    /** Reads the configured Sneak binding directly while a Screen owns keyboard input. */
    public static boolean sneakDown() {
        return ModeRadialClientAccess.sneakDown();
    }

    private static void request(Source source) {
        pendingSource = source;
        int requestId = REQUEST.begin();
        ModeRadialClientAccess.sendOpenRequest(
            requestId, source == Source.PRECISION_PREVIEW);
    }

    private static boolean sourceDown(Source source, boolean cycleDown, boolean radialDown,
                                      boolean surfacePreviewDown) {
        return switch (source) {
            case CYCLE -> cycleDown;
            case DEDICATED -> radialDown;
            case PRECISION_PREVIEW -> surfacePreviewDown;
        };
    }

    private static boolean sourceDown(Source source, ModeRadialClientAccess.Keys keys) {
        return sourceDown(source, keys.cycleDown(), keys.radialDown(), keys.precisionDown());
    }

    private static void remember(boolean cycleDown, boolean radialDown, boolean surfacePreviewDown,
                                 boolean pairingOperationDown) {
        cycleWasDown = cycleDown;
        radialWasDown = radialDown;
        surfacePreviewWasDown = surfacePreviewDown;
        pairingOperationWasDown = pairingOperationDown;
    }

    private static void reset(boolean cycleDown, boolean radialDown, boolean surfacePreviewDown,
                              boolean pairingOperationDown) {
        pendingSource = null;
        pendingPrecisionRequest = null;
        REQUEST.cancel();
        cycleHeldTicks = 0;
        cycleShortcutConsumed = false;
        remember(cycleDown, radialDown, surfacePreviewDown, pairingOperationDown);
    }

    private enum Source { CYCLE, DEDICATED, PRECISION_PREVIEW }

    private ModeRadialInput() {}
}

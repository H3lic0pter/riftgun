package dev.riftgun.input;

/** Loader-neutral state machine for a latency-tolerant hold-to-open radial request. */
public final class RadialRequestState {
    private int nextRequestId;
    private int pendingRequestId;
    private boolean pending;
    private boolean acknowledged;
    private boolean releasedBeforeAcknowledgement;

    public int begin() {
        pendingRequestId = ++nextRequestId;
        pending = true;
        acknowledged = false;
        releasedBeforeAcknowledgement = false;
        return pendingRequestId;
    }

    public ReleaseResult release() {
        if (!pending) return ReleaseResult.IGNORE;
        if (acknowledged) return ReleaseResult.COMMIT;
        releasedBeforeAcknowledgement = true;
        return ReleaseResult.WAIT_FOR_ACKNOWLEDGEMENT;
    }

    public AcknowledgeResult acknowledge(int requestId, boolean sourceStillDown) {
        if (!pending || requestId != pendingRequestId) return AcknowledgeResult.IGNORE;
        acknowledged = true;
        return releasedBeforeAcknowledgement || !sourceStillDown
            ? AcknowledgeResult.COMMIT : AcknowledgeResult.READY;
    }

    public boolean reject(int requestId) {
        if (!pending || requestId != pendingRequestId) return false;
        cancel();
        return true;
    }

    public boolean ready() {
        return pending && acknowledged;
    }

    public void cancel() {
        pending = false;
        acknowledged = false;
        releasedBeforeAcknowledgement = false;
    }

    public enum ReleaseResult { IGNORE, WAIT_FOR_ACKNOWLEDGEMENT, COMMIT }
    public enum AcknowledgeResult { IGNORE, READY, COMMIT }
}

package dev.riftgun.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RadialRequestStateTest {
    @Test
    void releaseBeforeAcknowledgementCommitsWhenMatchingResponseArrives() {
        RadialRequestState state = new RadialRequestState();
        int requestId = state.begin();

        assertEquals(RadialRequestState.ReleaseResult.WAIT_FOR_ACKNOWLEDGEMENT,
            state.release());
        assertEquals(RadialRequestState.AcknowledgeResult.COMMIT,
            state.acknowledge(requestId, false));
        assertTrue(state.ready());
    }

    @Test
    void staleResponsesCannotAcknowledgeOrRejectTheCurrentRequest() {
        RadialRequestState state = new RadialRequestState();
        int stale = state.begin();
        int current = state.begin();

        assertEquals(RadialRequestState.AcknowledgeResult.IGNORE,
            state.acknowledge(stale, true));
        assertFalse(state.reject(stale));
        assertEquals(RadialRequestState.AcknowledgeResult.READY,
            state.acknowledge(current, true));
        assertTrue(state.ready());
    }

    @Test
    void matchingRejectionCancelsPendingInteraction() {
        RadialRequestState state = new RadialRequestState();
        int requestId = state.begin();

        assertTrue(state.reject(requestId));
        assertFalse(state.ready());
        assertEquals(RadialRequestState.ReleaseResult.IGNORE, state.release());
    }
}

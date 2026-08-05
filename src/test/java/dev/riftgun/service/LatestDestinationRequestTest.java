package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class LatestDestinationRequestTest {
    @Test
    void duplicateTargetIsIgnoredAndReplacementInvalidatesOldToken() {
        LatestDestinationRequest requests = new LatestDestinationRequest();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        LatestDestinationRequest.Begin initial = requests.begin(first);
        assertEquals(LatestDestinationRequest.Outcome.STARTED, initial.outcome());
        assertEquals(LatestDestinationRequest.Outcome.DUPLICATE, requests.begin(first).outcome());

        LatestDestinationRequest.Begin replacement = requests.begin(second);
        assertEquals(LatestDestinationRequest.Outcome.REPLACED, replacement.outcome());
        assertFalse(requests.isCurrent(initial.token()));
        assertTrue(requests.isCurrent(replacement.token()));
    }
}

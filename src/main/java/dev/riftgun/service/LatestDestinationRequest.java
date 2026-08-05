package dev.riftgun.service;

import java.util.Objects;
import java.util.UUID;

/** Latest-wins identity gate for one player's pending portal request. */
public final class LatestDestinationRequest {
    private UUID destinationId;
    private long token;

    public Begin begin(UUID nextDestinationId) {
        Objects.requireNonNull(nextDestinationId, "nextDestinationId");
        if (nextDestinationId.equals(destinationId)) {
            return new Begin(Outcome.DUPLICATE, token);
        }

        Outcome outcome = destinationId == null ? Outcome.STARTED : Outcome.REPLACED;
        destinationId = nextDestinationId;
        token++;
        return new Begin(outcome, token);
    }

    public boolean isCurrent(long candidateToken) {
        return destinationId != null && token == candidateToken;
    }

    public boolean isCurrent(long candidateToken, UUID candidateDestinationId) {
        return isCurrent(candidateToken) && destinationId.equals(candidateDestinationId);
    }

    public UUID destinationId() {
        return destinationId;
    }

    public void clear() {
        destinationId = null;
        token++;
    }

    public enum Outcome {
        STARTED,
        DUPLICATE,
        REPLACED
    }

    public record Begin(Outcome outcome, long token) {}
}

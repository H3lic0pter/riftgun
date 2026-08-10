package dev.riftgun.crisis;

import java.util.Objects;
import java.util.Optional;

/** Result of preparing an operator-forced crisis, including a player-facing failure category. */
public record ForcedCrisisPreparation(Optional<PortalCrisisPlan> plan, Failure failure) {
    public ForcedCrisisPreparation {
        plan = Objects.requireNonNull(plan, "plan");
        failure = Objects.requireNonNull(failure, "failure");
        if (plan.isPresent() == (failure != Failure.NONE)) {
            throw new IllegalArgumentException("forced crisis result must contain either a plan or a failure");
        }
    }

    public static ForcedCrisisPreparation success(PortalCrisisPlan plan) {
        return new ForcedCrisisPreparation(Optional.of(plan), Failure.NONE);
    }

    public static ForcedCrisisPreparation failed(Failure failure) {
        if (failure == Failure.NONE) throw new IllegalArgumentException("NONE is not a failure");
        return new ForcedCrisisPreparation(Optional.empty(), failure);
    }

    public enum Failure {
        NONE,
        UNKNOWN_CRISIS,
        SPECTATOR,
        MOUNTED_TRANSIT,
        CRISIS_EXIT_LIMIT,
        DESTINATION_UNAVAILABLE,
        INTERNAL_ERROR
    }
}

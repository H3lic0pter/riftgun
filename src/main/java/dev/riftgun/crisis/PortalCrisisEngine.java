package dev.riftgun.crisis;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Fixed-budget roulette that preserves each crisis' absolute configured probability. */
public final class PortalCrisisEngine {
    public static final int TOTAL_WEIGHT = 1000;

    public static <T> Optional<T> select(List<Candidate<T>> candidates, int roll) {
        Objects.requireNonNull(candidates, "candidates");
        if (roll < 0 || roll >= TOTAL_WEIGHT) {
            throw new IllegalArgumentException("roll must be between 0 and 999");
        }
        int total = candidates.stream().mapToInt(Candidate::weight).sum();
        if (total > TOTAL_WEIGHT) {
            throw new IllegalArgumentException("crisis weights exceed " + TOTAL_WEIGHT);
        }

        int cursor = 0;
        for (Candidate<T> candidate : candidates) {
            cursor += candidate.weight();
            if (roll < cursor) {
                return candidate.eligible() ? Optional.of(candidate.value()) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    public record Candidate<T>(T value, int weight, boolean eligible) {
        public Candidate {
            Objects.requireNonNull(value, "value");
            if (weight < 0 || weight > TOTAL_WEIGHT) {
                throw new IllegalArgumentException("weight must be between 0 and 1000");
            }
        }
    }

    private PortalCrisisEngine() {}
}

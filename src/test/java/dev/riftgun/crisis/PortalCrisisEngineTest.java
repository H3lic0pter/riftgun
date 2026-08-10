package dev.riftgun.crisis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PortalCrisisEngineTest {
    @Test
    void ineligibleWeightBecomesNoCrisisInsteadOfBeingRedistributed() {
        List<PortalCrisisEngine.Candidate<String>> candidates = List.of(
            new PortalCrisisEngine.Candidate<>("fall", 80, false),
            new PortalCrisisEngine.Candidate<>("nausea", 55, true)
        );

        assertEquals(Optional.empty(), PortalCrisisEngine.select(candidates, 40));
        assertEquals(Optional.of("nausea"), PortalCrisisEngine.select(candidates, 100));
        assertEquals(Optional.empty(), PortalCrisisEngine.select(candidates, 500));
    }

    @Test
    void rejectsAnInvalidWeightBudget() {
        List<PortalCrisisEngine.Candidate<String>> candidates = List.of(
            new PortalCrisisEngine.Candidate<>("a", 600, true),
            new PortalCrisisEngine.Candidate<>("b", 401, true)
        );

        assertThrows(IllegalArgumentException.class,
            () -> PortalCrisisEngine.select(candidates, 0));
    }
}

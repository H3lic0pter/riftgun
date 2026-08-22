package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RandomRiftSearchPolicyTest {
    @Test
    void rejectsNewSearchAtOrAboveTheConfiguredLimit() {
        assertTrue(RandomRiftSearchPolicy.hasCapacity(7, 8));
        assertFalse(RandomRiftSearchPolicy.hasCapacity(8, 8));
        assertFalse(RandomRiftSearchPolicy.hasCapacity(10, 8));
    }

    @Test
    void probesTheCandidateChunkCenterAboveMinimumBuildHeight() {
        assertEquals(new RandomRiftSearchPolicy.CandidateProbe(-24, -63, 56),
            RandomRiftSearchPolicy.candidateProbe(-2, 3, -64));
    }

    @Test
    void ceilingDimensionsStopAtTheirLogicalCeiling() {
        assertEquals(128, RandomRiftSearchPolicy.searchCeiling(true, 0, 128, 256));
        assertEquals(192, RandomRiftSearchPolicy.searchCeiling(true, -64, 256, 320));
        assertEquals(320, RandomRiftSearchPolicy.searchCeiling(false, -64, 384, 320));
    }
}

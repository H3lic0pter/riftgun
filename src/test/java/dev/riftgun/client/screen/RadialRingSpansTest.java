package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.math.RadialRingSpans;
import org.junit.jupiter.api.Test;

final class RadialRingSpansTest {
    @Test
    void precisionRingPreservesEveryPixelWithFarFewerDrawCalls() {
        assertPrecisionRing(6, 320);
        assertPrecisionRing(3, 270);
    }

    private static void assertPrecisionRing(int optionCount, int maximumSpans) {
        int[] spans = {0};
        int[] coveredPixels = {0};

        RadialRingSpans.forEach(35, 66, 1, true, optionCount,
            (xFrom, y, xTo, height, optionIndex) -> {
                spans[0]++;
                coveredPixels[0] += (xTo - xFrom) * height;
                assertTrue(optionIndex >= 0 && optionIndex < optionCount);
            });

        assertEquals(9_840, coveredPixels[0]);
        assertTrue(spans[0] <= maximumSpans,
            "expected at most " + maximumSpans + " fills, got " + spans[0]);
    }
}

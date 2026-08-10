package dev.riftgun.recipe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FluidTransmutationMatcherTest {
    @Test
    void combinesCountsAcrossMultipleItemEntities() {
        int[] plan = FluidTransmutationMatcher.plan(new boolean[][] {
            {true, true}, {true, true}, {true, true}
        }, new int[] {1, 2}).orElseThrow();

        assertArrayEquals(new int[] {1, 2}, plan);
    }

    @Test
    void resolvesOverlappingIngredientsWithoutGreedyFailure() {
        int[] plan = FluidTransmutationMatcher.plan(new boolean[][] {
            {true, true},
            {true, false}
        }, new int[] {1, 1}).orElseThrow();

        assertArrayEquals(new int[] {1, 1}, plan);
    }

    @Test
    void rejectsInsufficientMaterial() {
        assertTrue(FluidTransmutationMatcher.plan(new boolean[][] {
            {true}, {true}
        }, new int[] {1}).isEmpty());
    }
}

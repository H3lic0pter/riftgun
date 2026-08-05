package dev.riftgun.fuel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class WorldFluidOverflowPolicyTest {
    @Test
    void wholeSourcePolicyAllowsOneOverflowingScoop() {
        WorldFluidOverflowPolicy policy = new WholeSourceOverflowPolicy();

        assertEquals(1000, policy.acceptedAmount(7500, 8000, 1000));
        assertEquals(1000, policy.acceptedAmount(7999, 8000, 1000));
        assertEquals(0, policy.acceptedAmount(8000, 8000, 1000));
        assertEquals(0, policy.acceptedAmount(8500, 8000, 1000));
    }

    @Test
    void strictAdapterCanReplaceOverflowWithoutChangingCallers() {
        WorldFluidOverflowPolicy policy = new StrictWorldFluidPolicy();

        assertEquals(1000, policy.acceptedAmount(7000, 8000, 1000));
        assertEquals(0, policy.acceptedAmount(7500, 8000, 1000));
    }
}
